package seedu.eventmanager.club;

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
import seedu.eventmanager.common.AccessDeniedException;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.event.EventDetails;
import seedu.eventmanager.event.EventService;
import seedu.eventmanager.event.JdbcEventRepository;
import seedu.eventmanager.event.OrganizerIdentity;
import seedu.eventmanager.storage.DatabaseConfig;
import seedu.eventmanager.storage.DatabaseMigration;
import seedu.eventmanager.storage.DriverManagerDataSource;

/**
 * PostgreSQL integration for clubs and per-account data isolation. Enabled by
 * {@code EVENT_MANAGER_TEST_DB_URL}; the target database is truncated, so use a test database.
 */
class JdbcClubRepositoryIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-09-26T05:00:00Z");
    private static final Actor ALICE = new Actor(UUID.randomUUID(), Role.CLUB_ORGANIZER);
    private static final Actor BOB = new Actor(UUID.randomUUID(), Role.CLUB_ORGANIZER);

    private DataSource dataSource;
    private JdbcClubRepository repository;
    private ClubService clubs;
    private EventService events;

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
            statement.execute("TRUNCATE organizer_club, organizer_club_audit_record, "
                    + "event_volunteer, event_volunteer_audit_record, "
                    + "organizer_event, organizer_event_audit_record RESTART IDENTITY");
        }
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        repository = new JdbcClubRepository(dataSource);
        clubs = new ClubService(repository, UUID::randomUUID, clock);
        events = new EventService(new JdbcEventRepository(dataSource), UUID::randomUUID, clock);
    }

    @Test
    void createAndList_roundTripThroughPostgresWithAudit() throws Exception {
        Club created = clubs.createClub(ALICE, "Chess Club");

        assertEquals(List.of(created), clubs.myClubs(ALICE));
        assertEquals(List.of("CREATE_CLUB"), auditActions());
    }

    @Test
    void create_duplicateNameIgnoringCaseAtDatabaseLevel_rejectedAndRolledBack() throws Exception {
        clubs.createClub(ALICE, "Chess Club");

        assertThrows(ValidationException.class, () -> repository.create(
                new Club(UUID.randomUUID(), "CHESS club", BOB.userId(), NOW),
                new ClubAuditRecord(NOW, BOB.userId(), ClubAuditRecord.Action.CREATE_CLUB, UUID.randomUUID())));

        assertTrue(clubs.myClubs(BOB).isEmpty());
        assertEquals(List.of("CREATE_CLUB"), auditActions());
    }

    @Test
    void identities_isolateEachOrganizersEvents() {
        Club aliceClub = clubs.createClub(ALICE, "Chess Club");
        clubs.createClub(BOB, "Drama Society");
        OrganizerIdentity alice = clubs.identityFor(ALICE);
        OrganizerIdentity bob = clubs.identityFor(BOB);
        UUID aliceEvent = events.createEvent(alice, aliceClub.id().toString(), new EventDetails(
                "Chess Night",
                "",
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T12:00:00Z"),
                30)).id();

        assertEquals(1, events.listEvents(alice).size());
        assertTrue(events.listEvents(bob).isEmpty());
        assertThrows(AccessDeniedException.class, () -> events.getEvent(bob, aliceEvent));
        assertThrows(AccessDeniedException.class, () -> events.createEvent(
                bob, aliceClub.id().toString(), new EventDetails(
                        "Hijack", "",
                        Instant.parse("2026-10-02T10:00:00Z"),
                        Instant.parse("2026-10-02T12:00:00Z"),
                        10)));
        assertEquals(Set.of(aliceClub.id().toString()), alice.ownedClubIds());
    }

    private List<String> auditActions() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet results = statement.executeQuery(
                        "SELECT action FROM organizer_club_audit_record ORDER BY id")) {
            List<String> actions = new ArrayList<>();
            while (results.next()) {
                actions.add(results.getString(1));
            }
            return actions;
        }
    }
}
