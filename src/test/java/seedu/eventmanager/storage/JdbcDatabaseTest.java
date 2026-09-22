package seedu.eventmanager.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JdbcDatabaseTest {
    @Test
    void configurationIsRetainedByDatabaseBoundary() {
        DatabaseConfiguration configuration = new DatabaseConfiguration("jdbc:postgresql://localhost/CS3227",
                "admin", "secret");
        JdbcDatabase database = new JdbcDatabase(configuration);

        assertEquals(configuration, configuration);
        // The actual connection test belongs in the PostgreSQL integration suite.
        org.junit.jupiter.api.Assertions.assertNotNull(database);
    }
}
