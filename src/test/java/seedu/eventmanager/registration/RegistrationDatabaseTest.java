package seedu.eventmanager.registration;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import seedu.eventmanager.storage.DatabaseConfiguration;
import seedu.eventmanager.storage.JdbcDatabase;
import seedu.eventmanager.storage.JdbcLocalSessionService;
import seedu.eventmanager.storage.PasswordHasher;
import seedu.eventmanager.storage.RegistrationDatabaseMigration;
import seedu.eventmanager.common.Role;

/** Each test owns one isolated schema in an explicitly configured disposable database. */
abstract class RegistrationDatabaseTest {
    static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");
    DatabaseConfiguration configuration;
    JdbcDatabase database;
    JdbcLocalSessionService sessions;
    String schema;
    String baseUrl;
    String user;
    String password;

    @BeforeEach
    void initialize() throws Exception {
        baseUrl = System.getenv("EVENT_MANAGER_TEST_DB_URL");
        assumeTrue(baseUrl != null && !baseUrl.isBlank(), "Disposable test database not configured");
        user = System.getenv().getOrDefault("EVENT_MANAGER_TEST_DB_USER", "event_manager");
        password = System.getenv().getOrDefault("EVENT_MANAGER_TEST_DB_PASSWORD", "");
        schema = "registration_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var c = DriverManager.getConnection(baseUrl, user, password); var s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        configuration = new DatabaseConfiguration(baseUrl + (baseUrl.contains("?") ? "&" : "?")
                + "currentSchema=" + schema, user, password);
        RegistrationDatabaseMigration.migrate(configuration);
        database = new JdbcDatabase(configuration);
        sessions = new JdbcLocalSessionService(database, new PasswordHasher());
    }

    @AfterEach
    void cleanup() throws Exception {
        if (schema != null) {
            try (var c = DriverManager.getConnection(baseUrl, user, password); var s = c.createStatement()) {
                s.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    Connection connection() throws SQLException {
        return DriverManager.getConnection(configuration.url(), user, password);
    }

    UUID account(String name, Role role) {
        return UUID.fromString(sessions.createUser(name, "synthetic-test-account", role));
    }

    String login(String name) {
        return sessions.login(name, "synthetic-test-account").token();
    }

    UUID event(String status, int capacity, Instant start) throws Exception {
        UUID id = UUID.randomUUID();
        try (var c = connection(); var p = c.prepareStatement("""
                INSERT INTO organizer_event
                (id,club_id,organizer_id,title,description,starts_at,ends_at,capacity,status,version)
                VALUES (?, 'test-club', 'test-organizer', 'Synthetic event', '', ?, ?, ?, ?, 0)
                """)) {
            p.setObject(1, id);
            p.setObject(2, start.atOffset(java.time.ZoneOffset.UTC));
            p.setObject(3, start.plusSeconds(7200).atOffset(java.time.ZoneOffset.UTC));
            p.setInt(4, capacity); p.setString(5, status); p.executeUpdate();
        }
        return id;
    }

    void registration(UUID event, UUID attendee, String status) throws Exception {
        try (var c = connection(); var p = c.prepareStatement("""
                INSERT INTO event_registrations
                (registration_id,event_id,attendee_id,status,registered_at,cancelled_at,checked_in_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP,
                    CASE WHEN ? = 'CANCELLED' THEN CURRENT_TIMESTAMP ELSE NULL END,
                    CASE WHEN ? = 'CHECKED_IN' THEN CURRENT_TIMESTAMP ELSE NULL END)
                """)) {
            p.setObject(1, UUID.randomUUID()); p.setObject(2, event); p.setObject(3, attendee);
            p.setString(4, status); p.setString(5, status); p.setString(6, status); p.executeUpdate();
        }
    }

    void sql(String sql) throws Exception {
        try (var c = connection(); var s = c.createStatement()) { s.execute(sql); }
    }
}
