package seedu.eventmanager.storage;

import org.flywaydb.core.Flyway;

/** Separate Attendee-owned stream; never edits or renumbers shared/applied migrations. */
public final class InboxDatabaseMigration {
    private InboxDatabaseMigration() { }
    public static void migrate(DatabaseConfiguration configuration) {
        RegistrationDatabaseMigration.migrate(configuration);
        Flyway.configure().dataSource(configuration.url(), configuration.username(), configuration.password())
                .locations("classpath:db/attendee_inbox").table("attendee_inbox_schema_history")
                .baselineOnMigrate(true).baselineVersion("0").load().migrate();
    }
}
