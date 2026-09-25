package seedu.eventmanager.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.storage.DatabaseBootstrap;
import seedu.eventmanager.storage.DatabaseConfig;
import seedu.eventmanager.storage.DatabaseConfiguration;
import seedu.eventmanager.storage.DatabaseMigration;
import seedu.eventmanager.storage.DriverManagerDataSource;
import seedu.eventmanager.storage.JdbcDatabase;
import seedu.eventmanager.storage.JdbcVenueRequestRepository;
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;

/**
 * PostgreSQL integration for draft capacity edits syncing open venue-request attendance.
 * Enabled when {@code EVENT_MANAGER_TEST_DB_URL} is set, or when
 * {@code DATABASE_INTEGRATION_TESTS=true} (uses app DatabaseBootstrap / .env).
 */
class EventCapacitySyncIntegrationTest {
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID REQUEST_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID VENUE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final Instant NOW = Instant.parse("2026-09-24T06:00:00Z");

    private DataSource dataSource;
    private JdbcDatabase jdbcDatabase;
    private JdbcEventRepository events;
    private JdbcVenueRequestRepository requests;
    private EventService service;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConfiguration configuration = resolveConfiguration();
        assumeTrue(configuration != null, "No test database configured");

        DatabaseBootstrap.migrate(configuration);
        dataSource = new DriverManagerDataSource(new DatabaseConfig(
                configuration.url(), configuration.username(), configuration.password()));
        new DatabaseMigration(dataSource).migrate();
        jdbcDatabase = new JdbcDatabase(configuration);

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE venue_bookings, venue_requests, venue_availability, venues "
                    + "RESTART IDENTITY CASCADE");
            statement.execute("TRUNCATE event_volunteer, event_volunteer_audit_record, "
                    + "organizer_event_audit_record, organizer_event RESTART IDENTITY");
        }

        events = new JdbcEventRepository(dataSource);
        requests = new JdbcVenueRequestRepository(jdbcDatabase);
        service = new EventService(
                events, () -> EVENT_ID, Clock.fixed(NOW, ZoneOffset.UTC), requests);
    }

    @Test
    void editEvent_capacityChange_updatesOpenSubmittedAttendanceInPostgres() {
        OrganizerIdentity actor = new OrganizerIdentity("demo-organizer", Set.of("club-1"));
        EventDetails details = new EventDetails(
                "Campus Night",
                "Demo",
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T12:00:00Z"),
                80);
        service.createEvent(actor, "club-1", details);
        insertVenue(VENUE_ID);
        OffsetDateTime start = OffsetDateTime.parse("2026-10-01T10:00:00Z");
        requests.save(new VenueRequest(
                REQUEST_ID,
                EVENT_ID,
                VENUE_ID,
                OrganizerIds.toUuid("demo-organizer"),
                start,
                start.plusHours(2),
                80,
                VenueRequestStatus.SUBMITTED));

        CapacityUpdateResult result = service.editEvent(
                actor,
                EVENT_ID,
                0,
                new EventDetails(
                        details.title(),
                        details.description(),
                        details.startsAt(),
                        details.endsAt(),
                        95));

        assertEquals(CapacityUpdateResult.SyncStatus.PENDING_REQUEST_SYNCED, result.syncStatus());
        assertEquals(95, result.event().capacity());
        assertEquals(95, requests.get(REQUEST_ID).expectedAttendance());
        assertEquals(VenueRequestStatus.SUBMITTED, requests.get(REQUEST_ID).status());
        assertEquals(95, events.findById(EVENT_ID).orElseThrow().capacity());
    }

    @Test
    void updateExpectedAttendance_ignoresApprovedRows() {
        insertVenue(VENUE_ID);
        OffsetDateTime start = OffsetDateTime.parse("2026-10-01T10:00:00Z");
        jdbcDatabase.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO venue_requests (
                        request_id, event_id, venue_id, organizer_id,
                        requested_starts_at, requested_ends_at, expected_attendance, status,
                        submitted_at, decided_at, decided_by, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, 80, 'APPROVED', ?, ?, ?, ?, ?)""")) {
                OffsetDateTime now = OffsetDateTime.now();
                statement.setObject(1, REQUEST_ID);
                statement.setObject(2, EVENT_ID);
                statement.setObject(3, VENUE_ID);
                statement.setObject(4, OrganizerIds.toUuid("demo-organizer"));
                statement.setObject(5, start);
                statement.setObject(6, start.plusHours(2));
                statement.setObject(7, now);
                statement.setObject(8, now);
                statement.setObject(9, UUID.randomUUID());
                statement.setObject(10, now);
                statement.setObject(11, now);
                statement.executeUpdate();
                return null;
            } catch (java.sql.SQLException exception) {
                throw new IllegalStateException(exception);
            }
        });

        assertFalse(requests.updateExpectedAttendance(REQUEST_ID, 50));
        assertEquals(80, requests.get(REQUEST_ID).expectedAttendance());
    }

    @Test
    void updateExpectedAttendance_updatesDraftAndSubmittedOnly() {
        insertVenue(VENUE_ID);
        OffsetDateTime start = OffsetDateTime.parse("2026-10-01T10:00:00Z");
        requests.save(new VenueRequest(
                REQUEST_ID,
                EVENT_ID,
                VENUE_ID,
                OrganizerIds.toUuid("demo-organizer"),
                start,
                start.plusHours(2),
                80,
                VenueRequestStatus.DRAFT));

        assertTrue(requests.updateExpectedAttendance(REQUEST_ID, 70));
        assertEquals(70, requests.get(REQUEST_ID).expectedAttendance());
        assertEquals(VenueRequestStatus.DRAFT, requests.get(REQUEST_ID).status());
    }

    private static DatabaseConfiguration resolveConfiguration() {
        String url = System.getenv("EVENT_MANAGER_TEST_DB_URL");
        if (url != null && !url.isBlank()) {
            return new DatabaseConfiguration(
                    url,
                    System.getenv().getOrDefault("EVENT_MANAGER_TEST_DB_USER", "event_manager"),
                    System.getenv("EVENT_MANAGER_TEST_DB_PASSWORD"));
        }
        if ("true".equalsIgnoreCase(System.getenv("DATABASE_INTEGRATION_TESTS"))) {
            return DatabaseBootstrap.configuration();
        }
        return null;
    }

    private void insertVenue(UUID venueId) {
        jdbcDatabase.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO venues (venue_id, name, location, capacity, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?)""")) {
                OffsetDateTime now = OffsetDateTime.now();
                statement.setObject(1, venueId);
                statement.setString(2, "Cap Sync Venue " + venueId);
                statement.setString(3, "Test Location");
                statement.setInt(4, 200);
                statement.setObject(5, now);
                statement.setObject(6, now);
                statement.executeUpdate();
                return null;
            } catch (java.sql.SQLException exception) {
                throw new IllegalStateException(exception);
            }
        });
    }
}
