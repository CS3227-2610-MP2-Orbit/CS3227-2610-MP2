package seedu.eventmanager.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DatabaseBootstrapTest {
    @Test
    void explicitConfigurationIsAcceptedByBootstrapBoundary() {
        DatabaseConfiguration configuration = new DatabaseConfiguration(
                "jdbc:postgresql://localhost:5432/CS3227", "admin", "secret");

        assertEquals("admin", configuration.username());
        assertEquals("secret", configuration.password());
    }
}
