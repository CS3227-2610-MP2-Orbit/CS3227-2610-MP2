package seedu.eventmanager.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import seedu.eventmanager.storage.DatabaseConfiguration;

class VenueAdministratorServiceFactoryTest {
    @Test
    void createsPostgresBackedWorkflowWithInjectedExternalServices() {
        VenueAdministratorService service = VenueAdministratorServiceFactory.create(
                new DatabaseConfiguration("jdbc:postgresql://localhost:5432/CS3227", "admin", "secret"),
                (actor, request) -> { },
                (recipientId, event, data) -> { },
                (actor, action, entityType, entityId, previousState, newState, reason) -> { });

        assertNotNull(service);
    }

    @Test
    void createsWorkflowWithDatabaseBackedAuditService() {
        VenueAdministratorService service = VenueAdministratorServiceFactory.create(
                new DatabaseConfiguration("jdbc:postgresql://localhost:5432/CS3227", "admin", "secret"),
                (actor, request) -> { },
                (recipientId, event, data) -> { });

        assertNotNull(service);
    }

    @Test
    void createsWorkflowWithDatabaseBackedAuditAndNotificationServices() {
        VenueAdministratorService service = VenueAdministratorServiceFactory.create(
                new DatabaseConfiguration("jdbc:postgresql://localhost:5432/CS3227", "admin", "secret"),
                (actor, request) -> { });

        assertNotNull(service);
    }
}
