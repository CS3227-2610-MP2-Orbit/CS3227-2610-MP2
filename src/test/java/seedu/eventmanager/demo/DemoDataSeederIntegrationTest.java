package seedu.eventmanager.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.DriverManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.storage.DatabaseConfiguration;

/** Seeds an isolated PostgreSQL schema (dropped afterwards) and checks the demo scenario each role sees. */
class DemoDataSeederIntegrationTest {
    private static final Instant NOW = Instant.parse("2030-03-04T05:17:00Z");

    private String baseUrl;
    private String user;
    private String password;
    private String schema;
    private DatabaseConfiguration configuration;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = System.getenv("EVENT_MANAGER_TEST_DB_URL");
        assumeTrue(baseUrl != null && !baseUrl.isBlank(), "Disposable test database not configured");
        user = System.getenv().getOrDefault("EVENT_MANAGER_TEST_DB_USER", "event_manager");
        password = System.getenv().getOrDefault("EVENT_MANAGER_TEST_DB_PASSWORD", "");
        String candidate = "demo_seed_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(baseUrl, user, password);
                var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + candidate);
        }
        schema = candidate;
        configuration = new DatabaseConfiguration(
                baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema, user, password);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (schema != null) {
            try (var connection = DriverManager.getConnection(baseUrl, user, password);
                    var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    @Test
    void seed_givesEveryRoleSomethingToWorkWith() throws Exception {
        assertTrue(seeder().seed());

        assertEquals(DemoDataSeeder.USERNAMES.stream().sorted().toList(),
                sorted("SELECT username FROM users WHERE username LIKE 'demo\\_%'"));
        assertEquals(List.of("Dance Club", "NUS Hackers", "Photography Society"),
                sorted("SELECT name FROM organizer_club"));
        assertEquals(List.of("DRAFT:Robotics Demo Day", "DRAFT:Welcome Tea", "PUBLISHED:Hack Night",
                        "PUBLISHED:Intro to Git Workshop", "PUBLISHED:Photo Walk: Kent Ridge",
                        "PUBLISHED:Photography Basics", "PUBLISHED:Street Dance Showcase"),
                sorted("SELECT status || ':' || title FROM organizer_event"));
        // Venue Administrator: one pending request to review.
        assertEquals(List.of("Robotics Demo Day"), strings("SELECT e.title FROM venue_requests r "
                + "JOIN organizer_event e ON e.id = r.event_id WHERE r.status = 'SUBMITTED'"));
        // Attendee: an ongoing event to check in to, a past check-in for history, and a cancellation.
        assertEquals(List.of("Street Dance Showcase"), strings("SELECT title FROM organizer_event "
                + "WHERE starts_at <= TIMESTAMPTZ '2030-03-04 05:17:00+00' "
                + "AND ends_at > TIMESTAMPTZ '2030-03-04 05:17:00+00'"));
        assertEquals(List.of("CANCELLED:demo_attendee3", "CHECKED_IN:demo_attendee",
                        "CONFIRMED:demo_attendee", "CONFIRMED:demo_attendee", "CONFIRMED:demo_attendee2",
                        "CONFIRMED:demo_attendee2"),
                sorted("SELECT r.status || ':' || u.username FROM event_registrations r "
                        + "JOIN users u ON u.user_id = r.attendee_id"));
        assertEquals(List.of("1"), strings("SELECT count(*) FROM event_announcement"));
        assertEquals(List.of("Usher"), strings("SELECT role FROM event_volunteer"));
    }

    @Test
    void seed_isSkippedWhenDemoDataAlreadyExists() throws Exception {
        assertTrue(seeder().seed());
        List<String> before = strings("SELECT count(*) FROM organizer_event");

        assertFalse(seeder().seed());

        assertEquals(before, strings("SELECT count(*) FROM organizer_event"));
    }

    private DemoDataSeeder seeder() {
        return new DemoDataSeeder(configuration, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /** Sorted in Java, so the expected order does not depend on the database's collation. */
    private List<String> sorted(String query) throws Exception {
        return strings(query).stream().sorted().toList();
    }

    private List<String> strings(String query) throws Exception {
        try (var connection = DriverManager.getConnection(configuration.url(), user, password);
                var statement = connection.createStatement();
                var rows = statement.executeQuery(query)) {
            List<String> values = new ArrayList<>();
            while (rows.next()) {
                values.add(rows.getString(1));
            }
            return values;
        }
    }
}
