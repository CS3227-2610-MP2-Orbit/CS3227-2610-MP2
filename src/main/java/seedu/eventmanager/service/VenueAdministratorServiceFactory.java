package seedu.eventmanager.service;

import java.util.Objects;
import seedu.eventmanager.common.StructuredLogger;
import seedu.eventmanager.common.Metrics;
import seedu.eventmanager.storage.DatabaseConfiguration;
import seedu.eventmanager.storage.JdbcDatabase;
import seedu.eventmanager.storage.JdbcAuditLogService;
import seedu.eventmanager.storage.JdbcTransactionManager;
import seedu.eventmanager.storage.JdbcVenueBookingRepository;
import seedu.eventmanager.storage.JdbcVenueRequestRepository;

/** Composition root for the PostgreSQL-backed Venue Administrator workflow. */
public final class VenueAdministratorServiceFactory {
    private VenueAdministratorServiceFactory() { }

    public static VenueAdministratorService create(DatabaseConfiguration configuration,
            AuthorizationService authorization, NotificationService notifications) {
        Objects.requireNonNull(configuration);
        JdbcDatabase database = new JdbcDatabase(configuration);
        return new VenueAdministratorService(
                new JdbcVenueRequestRepository(database),
                new JdbcVenueBookingRepository(database),
                authorization,
                notifications,
                new JdbcAuditLogService(database),
                new JdbcTransactionManager(database));
    }

    public static VenueAdministratorService create(DatabaseConfiguration configuration,
            AuthorizationService authorization, NotificationService notifications,
            AuditLogService audit) {
        return create(configuration, authorization, notifications, audit, null, null);
    }

    public static VenueAdministratorService create(DatabaseConfiguration configuration,
            AuthorizationService authorization, NotificationService notifications,
            AuditLogService audit, StructuredLogger logger, Metrics metrics) {
        Objects.requireNonNull(configuration);
        Objects.requireNonNull(authorization);
        Objects.requireNonNull(notifications);
        Objects.requireNonNull(audit);

        JdbcDatabase database = new JdbcDatabase(configuration);
        return new VenueAdministratorService(
                new JdbcVenueRequestRepository(database),
                new JdbcVenueBookingRepository(database),
                authorization,
                notifications,
                audit,
                new JdbcTransactionManager(database),
                logger == null ? new seedu.eventmanager.common.JavaUtilStructuredLogger(
                        VenueAdministratorService.class) : logger,
                metrics == null ? new seedu.eventmanager.common.NoopMetrics() : metrics);
    }
}
