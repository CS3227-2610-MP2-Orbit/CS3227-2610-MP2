package seedu.eventmanager.storage;

import java.util.Map;
import java.util.Objects;

/** Database settings loaded from the process environment. */
public record DatabaseConfiguration(String url, String username, String password) {
    public DatabaseConfiguration {
        requireNonBlank("DATABASE_URL", url);
        requireNonBlank("DATABASE_USER", username);
    }

    public static DatabaseConfiguration fromEnvironment() {
        return from(environment());
    }

    static DatabaseConfiguration from(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment");
        return new DatabaseConfiguration(environment.get("DATABASE_URL"),
                environment.get("DATABASE_USER"), environment.get("DATABASE_PASSWORD"));
    }

    private static void requireNonBlank(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured.");
        }
    }

    private static Map<String, String> environment() {
        return System.getenv();
    }
}
