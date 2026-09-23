package seedu.eventmanager.storage;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.HashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;

/** Opens the configured PostgreSQL database and applies pending migrations. */
public final class DatabaseBootstrap {
    private DatabaseBootstrap() { }

    /** Loads local .env values, overridden by process environment variables. */
    public static DatabaseConfiguration configuration() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();
        return configuration(System.getenv(), dotenv.entries().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> entry.getKey(), entry -> entry.getValue(), (first, ignored) -> first)));
    }

    static DatabaseConfiguration configuration(Map<String, String> environment,
            Map<String, String> dotenvValues) {
        Map<String, String> values = new HashMap<>();
        values.put("DATABASE_URL", value("DATABASE_URL", "EVENT_MANAGER_DB_URL",
                environment, dotenvValues));
        values.put("DATABASE_USER", value("DATABASE_USER", "EVENT_MANAGER_DB_USER",
                environment, dotenvValues));
        values.put("DATABASE_PASSWORD", value("DATABASE_PASSWORD", "EVENT_MANAGER_DB_PASSWORD",
                environment, dotenvValues));
        return DatabaseConfiguration.from(values);
    }

    /** Applies all pending classpath migrations and returns the migration result. */
    public static int migrate() {
        DatabaseConfiguration configuration = configuration();
        return migrate(configuration);
    }

    public static int migrate(DatabaseConfiguration configuration) {
        // Organizer tables may already exist via the non-Flyway organizer migration.
        // Baseline at 0 so all Venue Flyway scripts (V1+) still apply on first run.
        return Flyway.configure()
                .dataSource(configuration.url(), configuration.username(), configuration.password())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
                .migrate()
                .migrationsExecuted;
    }

    private static String value(String name, String organizerName, Map<String, String> environment,
            Map<String, String> dotenvValues) {
        String processValue = environment.get(name);
        if (processValue != null) {
            return processValue;
        }
        String organizerProcessValue = environment.get(organizerName);
        if (organizerProcessValue != null) {
            return organizerProcessValue;
        }
        String dotenvValue = dotenvValues.get(name);
        return dotenvValue == null ? dotenvValues.get(organizerName) : dotenvValue;
    }
}
