package seedu.eventmanager.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.storage.DatabaseBootstrap;
import seedu.eventmanager.storage.DatabaseConfiguration;
import seedu.eventmanager.storage.JdbcDatabase;
import seedu.eventmanager.storage.JdbcVenueBookingRepository;
import seedu.eventmanager.storage.JdbcVenueRequestRepository;
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;

/** PostgreSQL-backed checks enabled explicitly by DATABASE_INTEGRATION_TESTS. */
class PostgreSqlVenueAdministratorIntegrationTest {
    private static JdbcDatabase database;

    @BeforeAll
    static void setUpDatabase() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("DATABASE_INTEGRATION_TESTS")));
        DatabaseConfiguration configuration = DatabaseBootstrap.configuration();
        DatabaseBootstrap.migrate(configuration);
        database = new JdbcDatabase(configuration);
    }

    @Test
    void persistsRequestAndDetectsBookingConflict() {
        UUID venueId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.now().plusDays(2).withNano(0);
        VenueRequest request = new VenueRequest(requestId, eventId, venueId, organizerId,
                start, start.plusHours(1), 25, VenueRequestStatus.SUBMITTED);

        insertVenue(venueId);
        new JdbcVenueRequestRepository(database).save(request);
        assertEquals(request, new JdbcVenueRequestRepository(database).get(requestId));

        JdbcVenueBookingRepository bookings = new JdbcVenueBookingRepository(database);
        assertFalse(bookings.hasConflict(venueId, start, start.plusHours(1)));
        bookings.createFromApprovedRequest(request, UUID.randomUUID());
        assertTrue(bookings.hasConflict(venueId, start, start.plusHours(1)));
    }

    private void insertVenue(UUID venueId) {
        database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO venues (venue_id, name, location, capacity, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?)""")) {
                OffsetDateTime now = OffsetDateTime.now();
                statement.setObject(1, venueId);
                statement.setString(2, "CI Venue " + venueId);
                statement.setString(3, "CI Test Location");
                statement.setInt(4, 100);
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
