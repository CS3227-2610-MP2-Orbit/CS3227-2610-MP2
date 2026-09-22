package seedu.eventmanager.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void rejectsMissingDatabaseSettings() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> DatabaseConfiguration.from(Map.of("DATABASE_URL", "jdbc:postgresql://localhost/CS3227",
                        "DATABASE_USER", "admin")));

        assertEquals("DATABASE_PASSWORD must be configured.", exception.getMessage());
    }
}
