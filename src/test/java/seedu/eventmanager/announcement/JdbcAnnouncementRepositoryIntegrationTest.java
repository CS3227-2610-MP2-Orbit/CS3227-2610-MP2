package seedu.eventmanager.announcement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.event.EventDetails;
import seedu.eventmanager.event.EventService;
import seedu.eventmanager.event.JdbcEventRepository;
import seedu.eventmanager.event.OrganizerIdentity;
import seedu.eventmanager.storage.DatabaseConfig;
import seedu.eventmanager.storage.DatabaseMigration;
import seedu.eventmanager.storage.DriverManagerDataSource;

/**
 * PostgreSQL integration for announcements. Registrations and the notification outbox are faked;
 * this verifies announcement persistence and its audit transaction only. Enabled by
 * {@code EVENT_MANAGER_TEST_DB_URL}; the target database is truncated, so use a test database.
 */
class JdbcAnnouncementRepositoryIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-09-25T04:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final OrganizerIdentity ORGANIZER =
            new OrganizerIdentity("organizer-1", Set.of("club-1"));

    private DataSource dataSource;
    private JdbcAnnouncementRepository repository;
    private AnnouncementService service;

    @BeforeEach
    void setUp() throws Exception {
        String url = System.getenv("EVENT_MANAGER_TEST_DB_URL");
        assumeTrue(url != null && !url.isBlank(), "EVENT_MANAGER_TEST_DB_URL is not configured");
        dataSource = new DriverManagerDataSource(new DatabaseConfig(
                url,
                System.getenv().getOrDefault("EVENT_MANAGER_TEST_DB_USER", "event_manager"),
                System.getenv("EVENT_MANAGER_TEST_DB_PASSWORD")));
        new DatabaseMigration(dataSource).migrate();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE event_announcement, event_announcement_audit_record, "
                    + "event_volunteer, event_volunteer_audit_record, "
                    + "organizer_event, organizer_event_audit_record RESTART IDENTITY");
        }

        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        EventService eventService = new EventService(new JdbcEventRepository(dataSource), () -> EVENT_ID, clock);
        eventService.createEvent(ORGANIZER, "club-1", new EventDetails(
                "Campus Night",
                "Demo",
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T12:00:00Z"),
                80));
        repository = new JdbcAnnouncementRepository(dataSource);
        service = new AnnouncementService(
                eventService,
                eventId -> List.of(),
                repository,
                (recipientId, event, data) -> { },
                UUID::randomUUID,
                clock);
    }

    @Test
    void postAndList_roundTripThroughPostgresWithAudit() throws Exception {
        AnnouncementResult result = service.post(ORGANIZER, EVENT_ID, "Doors open at 6pm");

        assertEquals(List.of(result.announcement()), service.list(ORGANIZER, EVENT_ID));
        assertEquals(List.of("POST_ANNOUNCEMENT"), auditActions());
    }

    @Test
    void findByEventId_newestFirst() {
        repository.add(announcement("Older", NOW), audit(NOW));
        repository.add(announcement("Newer", NOW.plusSeconds(60)), audit(NOW.plusSeconds(60)));

        assertEquals(
                List.of("Newer", "Older"),
                repository.findByEventId(EVENT_ID).stream().map(Announcement::message).toList());
    }

    @Test
    void add_auditFailure_rollsBackAnnouncement() throws Exception {
        AnnouncementAuditRecord invalidAudit = new AnnouncementAuditRecord(
                NOW, null, AnnouncementAuditRecord.Action.POST_ANNOUNCEMENT, EVENT_ID, UUID.randomUUID());

        assertThrows(RuntimeException.class, () -> repository.add(announcement("Hello", NOW), invalidAudit));

        assertTrue(repository.findByEventId(EVENT_ID).isEmpty());
        assertTrue(auditActions().isEmpty());
    }

    @Test
    void add_unknownEvent_rejectedByForeignKeyWithoutAudit() throws Exception {
        UUID unknownEvent = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> repository.add(
                new Announcement(UUID.randomUUID(), unknownEvent, "organizer-1", "Hello", NOW),
                new AnnouncementAuditRecord(NOW, "organizer-1",
                        AnnouncementAuditRecord.Action.POST_ANNOUNCEMENT, unknownEvent, UUID.randomUUID())));

        assertTrue(auditActions().isEmpty());
    }

    private static Announcement announcement(String message, Instant createdAt) {
        return new Announcement(UUID.randomUUID(), EVENT_ID, "organizer-1", message, createdAt);
    }

    private static AnnouncementAuditRecord audit(Instant occurredAt) {
        return new AnnouncementAuditRecord(occurredAt, "organizer-1",
                AnnouncementAuditRecord.Action.POST_ANNOUNCEMENT, EVENT_ID, UUID.randomUUID());
    }

    private List<String> auditActions() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet results = statement.executeQuery(
                        "SELECT action FROM event_announcement_audit_record ORDER BY id")) {
            List<String> actions = new ArrayList<>();
            while (results.next()) {
                actions.add(results.getString(1));
            }
            return actions;
        }
    }
}
