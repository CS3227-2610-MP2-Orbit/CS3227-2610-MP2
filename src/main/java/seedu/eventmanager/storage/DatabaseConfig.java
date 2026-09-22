package seedu.eventmanager.storage;

import java.util.Map;
import java.util.Objects;

/** PostgreSQL connection settings loaded from the application environment. */
public final class DatabaseConfig {
    private final String url;
    private final String username;
    private final String password;

    public DatabaseConfig(String url, String username, String password) {
        this.url = requireText(url, "Database URL");
        this.username = requireText(username, "Database username");
        this.password = password;
    }

    public static DatabaseConfig fromEnvironment(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment");
        return new DatabaseConfig(
                environment.getOrDefault(
                        "EVENT_MANAGER_DB_URL", "jdbc:postgresql://localhost:5432/event_manager"),
                environment.getOrDefault("EVENT_MANAGER_DB_USER", "event_manager"),
                environment.get("EVENT_MANAGER_DB_PASSWORD"));
    }

    public String url() {
        return url;
    }

    public String username() {
        return username;
    }

    String password() {
        return password;
    }

    @Override
    public String toString() {
        return "DatabaseConfig[url=<redacted>, username=" + username + ", password=<redacted>]";
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }
}
