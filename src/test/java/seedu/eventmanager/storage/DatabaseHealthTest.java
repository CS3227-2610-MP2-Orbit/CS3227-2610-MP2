package seedu.eventmanager.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

class DatabaseHealthTest {
    @Test
    void check_reportsAReachableDatabaseAsUp() {
        String url = System.getenv("EVENT_MANAGER_TEST_DB_URL");
        assumeTrue(url != null && !url.isBlank(), "Disposable test database not configured");
        DatabaseHealth.Status status = DatabaseHealth.check(new DatabaseConfiguration(url,
                System.getenv().getOrDefault("EVENT_MANAGER_TEST_DB_USER", "event_manager"),
                System.getenv().getOrDefault("EVENT_MANAGER_TEST_DB_PASSWORD", "")));

        assertTrue(status.up(), status.problem());
        assertTrue(status.latencyMillis() >= 0);
        assertTrue(status.serverVersion().startsWith("PostgreSQL"), status.serverVersion());
        assertTrue(status.summary().startsWith("Database connected"), status.summary());
    }

    @Test
    void check_reportsAnUnreachableDatabaseWithoutLeakingCredentials() {
        DatabaseHealth.Status status = DatabaseHealth.check(new DatabaseConfiguration(
                "jdbc:postgresql://127.0.0.1:1/secret_db_name", "secret_user", "secret_password"));

        assertFalse(status.up());
        assertEquals("", status.serverVersion());
        String shown = status.summary() + " " + status.problem();
        assertTrue(shown.startsWith("Database unavailable"), shown);
        assertFalse(shown.contains("secret"), shown);
    }
}
