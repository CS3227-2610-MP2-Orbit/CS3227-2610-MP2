package seedu.eventmanager.registration;

import java.time.Clock;
import seedu.eventmanager.storage.DatabaseConfiguration;
import seedu.eventmanager.storage.JdbcAuditLogService;
import seedu.eventmanager.storage.JdbcDatabase;
import seedu.eventmanager.storage.JdbcLocalSessionService;
import seedu.eventmanager.storage.JdbcNotificationService;
import seedu.eventmanager.storage.JdbcRegistrationStore;
import seedu.eventmanager.storage.JdbcTransactionManager;
import seedu.eventmanager.storage.PasswordHasher;

/** One database instance ensures state, audit and outbox share one transaction. */
public final class RegistrationServiceFactory {
    private RegistrationServiceFactory() { }

    /** Migrate explicitly at application startup with RegistrationDatabaseMigration first. */
    public static RegistrationService create(DatabaseConfiguration configuration, Clock clock) {
        JdbcDatabase database = new JdbcDatabase(configuration);
        JdbcLocalSessionService sessions = new JdbcLocalSessionService(database, new PasswordHasher());
        return new RegistrationService(new JdbcRegistrationStore(database), sessions::resolve,
                new JdbcTransactionManager(database), new JdbcAuditLogService(database),
                new JdbcNotificationService(database), clock);
    }
}
