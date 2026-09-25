package seedu.eventmanager.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DatabaseConfigTest {
    @Test
    void fromEnvironment_usesConfiguredConnectionWithoutExposingPassword() {
        DatabaseConfig config = DatabaseConfig.fromEnvironment(Map.of(
                "EVENT_MANAGER_DB_URL", "jdbc:postgresql://db.example/events",
                "EVENT_MANAGER_DB_USER", "organizer_app",
                "EVENT_MANAGER_DB_PASSWORD", "sensitive-value"));

        assertEquals("jdbc:postgresql://db.example/events", config.url());
        assertEquals("organizer_app", config.username());
        assertFalse(config.toString().contains("db.example"));
        assertFalse(config.toString().contains("sensitive-value"));
    }
}
