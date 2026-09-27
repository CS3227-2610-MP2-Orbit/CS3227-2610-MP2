package seedu.eventmanager.storage;

import java.sql.SQLException;
import java.util.Objects;
import org.flywaydb.core.Flyway;

/** Explicit bootstrap for registration consumers, including canonical prerequisites. */
public final class RegistrationDatabaseMigration {
    private RegistrationDatabaseMigration() { }

    public static void migrate(DatabaseConfiguration configuration) {
        Objects.requireNonNull(configuration);
        DatabaseBootstrap.migrate(configuration);
        try {
            new DatabaseMigration(new DriverManagerDataSource(new DatabaseConfig(
                    configuration.url(), configuration.username(), configuration.password()))).migrate();
        } catch (SQLException failure) {
            throw new IllegalStateException("Could not initialize registration prerequisites.", failure);
        }
        // Organizer has its own migration stream. Do not collide with Venue V1/V5,
        // or require organizer_event before the shared login bootstrap can run.
        Flyway.configure().dataSource(configuration.url(), configuration.username(), configuration.password())
                .locations("classpath:db/registration").table("registration_schema_history")
                .baselineOnMigrate(true).baselineVersion("0").load().migrate();
    }
}
