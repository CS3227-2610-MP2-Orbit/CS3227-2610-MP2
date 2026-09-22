package seedu.eventmanager.storage;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import seedu.eventmanager.service.NotificationService;

/** Stores notification events in a transactional PostgreSQL outbox. */
public final class JdbcNotificationService implements NotificationService {
    private final JdbcDatabase database;

    public JdbcNotificationService(JdbcDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public void notify(UUID recipientId, String event, Map<String, String> data) {
        Objects.requireNonNull(recipientId);
        Objects.requireNonNull(event);
        Objects.requireNonNull(data);

        String idempotencyKey = event + ":" + recipientId + ":" + canonicalData(data);
        String payload = toJson(data);
        database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO notification_outbox
                        (notification_id, idempotency_key, recipient_id, event, payload,
                         status, attempts, next_attempt_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, CAST(? AS jsonb), 'PENDING', 0, ?, ?, ?)
                    ON CONFLICT (idempotency_key) DO NOTHING""")) {
                OffsetDateTime now = OffsetDateTime.now();
                statement.setObject(1, UUID.randomUUID());
                statement.setString(2, idempotencyKey);
                statement.setObject(3, recipientId);
                statement.setString(4, event);
                statement.setString(5, payload);
                statement.setObject(6, now);
                statement.setObject(7, now);
                statement.setObject(8, now);
                statement.executeUpdate();
                return null;
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not enqueue notification.", exception);
            }
        });
    }

    private static String canonicalData(Map<String, String> data) {
        return new TreeMap<>(data).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private static String toJson(Map<String, String> data) {
        return new TreeMap<>(data).entrySet().stream()
                .map(entry -> "\"" + escape(entry.getKey()) + "\":\""
                        + escape(entry.getValue()) + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
