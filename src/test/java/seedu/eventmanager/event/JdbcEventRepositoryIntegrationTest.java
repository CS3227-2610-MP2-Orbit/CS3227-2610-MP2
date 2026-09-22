package seedu.eventmanager.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.storage.DatabaseConfig;
import seedu.eventmanager.storage.DatabaseMigration;
import seedu.eventmanager.storage.DriverManagerDataSource;

class JdbcEventRepositoryIntegrationTest {
    private static final UUID EVENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final Instant NOW = Instant.parse("2026-09-22T10:00:00Z");

    private DataSource dataSource;
    private JdbcEventRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        String url = System.getenv("EVENT_MANAGER_TEST_DB_URL");
        assumeTrue(url != null && !url.isBlank(), "EVENT_MANAGER_TEST_DB_URL is not configured");
        DatabaseConfig config = new DatabaseConfig(
                url,
                System.getenv().getOrDefault("EVENT_MANAGER_TEST_DB_USER", "event_manager"),
                System.getenv("EVENT_MANAGER_TEST_DB_PASSWORD"));
        dataSource = new DriverManagerDataSource(config);
        new DatabaseMigration(dataSource).migrate();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE organizer_event, organizer_event_audit_record RESTART IDENTITY");
        }
        repository = new JdbcEventRepository(dataSource);
    }

    @Test
    void createAndEdit_roundTripsThroughPostgresWithAuditAndVersionCheck() throws Exception {
        EventService service = new EventService(
                repository,
                () -> EVENT_ID,
                Clock.fixed(NOW, ZoneOffset.UTC));
        OrganizerIdentity actor = new OrganizerIdentity("organizer-1", Set.of("club-1"));
        EventDetails original = new EventDetails(
                "Campus Night",
                "Original",
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T12:00:00Z"),
                80);

        Event created = service.createEvent(actor, "club-1", original);
        Event edited = service.editEvent(
                actor,
                created.id(),
                created.version(),
                new EventDetails(
                        "Campus Night Updated",
                        "Edited",
                        original.startsAt(),
                        original.endsAt(),
                        100));

        assertEquals(edited, repository.findById(EVENT_ID).orElseThrow());
        assertEquals(1, edited.version());
        assertEquals(2, auditRecordCount());

        assertThrows(
                EventVersionConflictException.class,
                () -> repository.update(created, 0, new EventAuditRecord(
                        NOW,
                        actor.userId(),
                        EventAuditRecord.Action.EDIT_EVENT,
                        EVENT_ID,
                        1)));
        assertEquals(edited, repository.findById(EVENT_ID).orElseThrow());
        assertEquals(2, auditRecordCount());
    }

    private int auditRecordCount() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT COUNT(*) FROM organizer_event_audit_record")) {
            result.next();
            return result.getInt(1);
        }
    }
}
