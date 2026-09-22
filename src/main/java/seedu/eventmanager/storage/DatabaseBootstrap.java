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
        Map<String, String> values = new HashMap<>();
        values.put("DATABASE_URL", value("DATABASE_URL", dotenv));
        values.put("DATABASE_USER", value("DATABASE_USER", dotenv));
        values.put("DATABASE_PASSWORD", value("DATABASE_PASSWORD", dotenv));
        return DatabaseConfiguration.from(values);
    }

    /** Applies all pending classpath migrations and returns the migration result. */
    public static int migrate() {
        DatabaseConfiguration configuration = configuration();
        return migrate(configuration);
    }

    public static int migrate(DatabaseConfiguration configuration) {
        return Flyway.configure()
                .dataSource(configuration.url(), configuration.username(), configuration.password())
                .locations("classpath:db/migration")
                .load()
                .migrate()
                .migrationsExecuted;
    }

    private static String value(String name, Dotenv dotenv) {
        String processValue = System.getenv(name);
        return processValue == null ? dotenv.get(name) : processValue;
    }
}
