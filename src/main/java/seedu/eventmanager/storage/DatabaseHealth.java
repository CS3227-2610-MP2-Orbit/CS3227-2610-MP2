package seedu.eventmanager.storage;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Startup health probe for the configured PostgreSQL database. */
public final class DatabaseHealth {
    private static final String CONNECT_TIMEOUT_SECONDS = "5";

    private DatabaseHealth() { }

    /** Outcome shown in the app and written to the diagnostic log; never contains the URL or password. */
    public record Status(boolean up, long latencyMillis, String serverVersion, String problem) {
        public String summary() {
            return up
                    ? "Database connected (" + serverVersion + ", " + latencyMillis + " ms)"
                    : "Database unavailable: " + problem;
        }
    }

    public static Status check(DatabaseConfiguration configuration) {
        Properties properties = new Properties();
        properties.setProperty("user", configuration.username());
        properties.setProperty("password", configuration.password() == null ? "" : configuration.password());
        properties.setProperty("connectTimeout", CONNECT_TIMEOUT_SECONDS);
        long started = System.nanoTime();
        try (var connection = DriverManager.getConnection(configuration.url(), properties);
                var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT 1")) {
            result.next();
            long latency = (System.nanoTime() - started) / 1_000_000;
            var metadata = connection.getMetaData();
            return new Status(true, latency,
                    metadata.getDatabaseProductName() + " " + metadata.getDatabaseProductVersion(), "");
        } catch (SQLException exception) {
            return new Status(false, (System.nanoTime() - started) / 1_000_000, "", describe(exception));
        }
    }

    /** Maps the SQL state to safe text; driver messages can include host or database names. */
    private static String describe(SQLException exception) {
        String state = exception.getSQLState() == null ? "" : exception.getSQLState();
        if (state.startsWith("08")) {
            return "cannot reach the PostgreSQL server (is it running?)";
        }
        if (state.startsWith("28")) {
            return "PostgreSQL rejected the username or password";
        }
        if ("3D000".equals(state)) {
            return "the configured database does not exist";
        }
        return exception.getClass().getSimpleName() + (state.isEmpty() ? "" : " (SQL state " + state + ")");
    }
}
