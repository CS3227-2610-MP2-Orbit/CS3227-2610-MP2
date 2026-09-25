package seedu.eventmanager.volunteer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.event.EventDetails;
import seedu.eventmanager.event.EventService;
import seedu.eventmanager.event.JdbcEventRepository;
import seedu.eventmanager.event.OrganizerIdentity;
import seedu.eventmanager.registration.RegisteredAttendee;
import seedu.eventmanager.storage.DatabaseConfig;
import seedu.eventmanager.storage.DatabaseMigration;
import seedu.eventmanager.storage.DriverManagerDataSource;

/**
 * PostgreSQL integration for volunteer assignment. Registrations are faked because the
 * Attendee registration feature is not in this checkout. Enabled by
 * {@code EVENT_MANAGER_TEST_DB_URL}; the target database is truncated, so use a test database.
 */
class JdbcVolunteerRepositoryIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-09-25T04:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000a11c");
    private static final OrganizerIdentity ORGANIZER =
            new OrganizerIdentity("organizer-1", Set.of("club-1"));

    private DataSource dataSource;
    private JdbcVolunteerRepository repository;
    private VolunteerService service;

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
            statement.execute("TRUNCATE event_volunteer, event_volunteer_audit_record, "
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
        repository = new JdbcVolunteerRepository(dataSource);
        service = new VolunteerService(
                eventService,
                eventId -> List.of(new RegisteredAttendee(ALICE, "Alice")),
                repository,
                clock);
    }

    @Test
    void assignListAndRemove_roundTripThroughPostgresWithAudit() throws Exception {
        service.assign(ORGANIZER, EVENT_ID, ALICE, "Usher");

        assertEquals(
                List.of(new VolunteerAssignment(EVENT_ID, ALICE, "Usher", "organizer-1", NOW)),
                repository.findByEventId(EVENT_ID));
        List<AssignedVolunteer> listed = service.listVolunteers(ORGANIZER, EVENT_ID);
        assertEquals(1, listed.size());
        assertEquals(Optional.of("Alice"), listed.getFirst().displayName());
        assertEquals(List.of("ASSIGN_VOLUNTEER"), auditActions());

        service.remove(ORGANIZER, EVENT_ID, ALICE);

        assertTrue(repository.findByEventId(EVENT_ID).isEmpty());
        assertEquals(List.of("ASSIGN_VOLUNTEER", "REMOVE_VOLUNTEER"), auditActions());
    }

    @Test
    void add_duplicateAtDatabaseLevel_rejectedAndRolledBack() throws Exception {
        VolunteerAssignment assignment = new VolunteerAssignment(EVENT_ID, ALICE, "Usher", "organizer-1", NOW);
        VolunteerAuditRecord audit = new VolunteerAuditRecord(
                NOW, "organizer-1", VolunteerAuditRecord.Action.ASSIGN_VOLUNTEER, EVENT_ID, ALICE);
        repository.add(assignment, audit);

        assertThrows(ValidationException.class, () -> repository.add(assignment, audit));

        assertEquals(1, repository.findByEventId(EVENT_ID).size());
        assertEquals(List.of("ASSIGN_VOLUNTEER"), auditActions());
    }

    @Test
    void remove_missingAssignment_returnsFalseWithoutAudit() throws Exception {
        assertFalse(repository.remove(EVENT_ID, ALICE, new VolunteerAuditRecord(
                NOW, "organizer-1", VolunteerAuditRecord.Action.REMOVE_VOLUNTEER, EVENT_ID, ALICE)));
        assertTrue(auditActions().isEmpty());
    }

    @Test
    void add_unknownEvent_rejectedByForeignKeyWithoutAudit() throws Exception {
        UUID unknownEvent = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> repository.add(
                new VolunteerAssignment(unknownEvent, ALICE, "", "organizer-1", NOW),
                new VolunteerAuditRecord(
                        NOW, "organizer-1", VolunteerAuditRecord.Action.ASSIGN_VOLUNTEER, unknownEvent, ALICE)));
        assertTrue(auditActions().isEmpty());
    }

    private List<String> auditActions() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet results = statement.executeQuery(
                        "SELECT action FROM event_volunteer_audit_record ORDER BY id")) {
            List<String> actions = new java.util.ArrayList<>();
            while (results.next()) {
                actions.add(results.getString(1));
            }
            return actions;
        }
    }
}
