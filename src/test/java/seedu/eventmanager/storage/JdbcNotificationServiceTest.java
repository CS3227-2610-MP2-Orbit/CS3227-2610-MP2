package seedu.eventmanager.storage;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class JdbcNotificationServiceTest {
    @Test
    void createsTransactionalNotificationOutboxAdapter() {
        JdbcDatabase database = new JdbcDatabase(new DatabaseConfiguration(
                "jdbc:postgresql://localhost:5432/CS3227", "admin", "secret"));

        assertNotNull(new JdbcNotificationService(database));
    }
}
