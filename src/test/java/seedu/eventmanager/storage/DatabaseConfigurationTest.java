package seedu.eventmanager.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DatabaseConfigurationTest {
    @Test
    void loadsDatabaseSettingsFromEnvironmentValues() {
        DatabaseConfiguration configuration = DatabaseConfiguration.from(Map.of(
                "DATABASE_URL", "jdbc:postgresql://localhost:5432/CS3227",
                "DATABASE_USER", "admin",
                "DATABASE_PASSWORD", "secret"));

        assertEquals("jdbc:postgresql://localhost:5432/CS3227", configuration.url());
        assertEquals("admin", configuration.username());
        assertEquals("secret", configuration.password());
    }

    @Test
    void acceptsAConfiguredConnectionWithoutPassword() {
        DatabaseConfiguration configuration = DatabaseConfiguration.from(Map.of(
                "DATABASE_URL", "jdbc:postgresql://localhost/CS3227",
                "DATABASE_USER", "admin"));

        assertEquals(null, configuration.password());
    }

    @Test
    void fallsBackToOrganizerDatabaseSettings() {
        DatabaseConfiguration configuration = DatabaseBootstrap.configuration(Map.of(
                "EVENT_MANAGER_DB_URL", "jdbc:postgresql://localhost:5432/event_manager",
                "EVENT_MANAGER_DB_USER", "joseph"), Map.of());

        assertEquals("jdbc:postgresql://localhost:5432/event_manager", configuration.url());
        assertEquals("joseph", configuration.username());
        assertEquals(null, configuration.password());
    }
}
