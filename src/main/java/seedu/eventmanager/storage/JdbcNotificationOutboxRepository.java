package seedu.eventmanager.storage;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import seedu.eventmanager.notification.NotificationOutboxRepository;

/** PostgreSQL claim-and-update operations for notification delivery. */
public final class JdbcNotificationOutboxRepository implements NotificationOutboxRepository {
    private final JdbcDatabase database;

    public JdbcNotificationOutboxRepository(JdbcDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public OutboxEvent claimNext() {
        return database.inTransaction(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT notification_id, recipient_id, event, payload::text, attempts
                    FROM notification_outbox
                    WHERE (status = 'PENDING' OR
                           (status = 'PROCESSING' AND next_attempt_at <= CURRENT_TIMESTAMP))
                      AND next_attempt_at <= CURRENT_TIMESTAMP
                    ORDER BY created_at
                    FOR UPDATE SKIP LOCKED
                    LIMIT 1""")) {
                try (var result = statement.executeQuery()) {
                    if (!result.next()) {
                        return null;
                    }
                    UUID id = result.getObject("notification_id", UUID.class);
                    int attempts = result.getInt("attempts") + 1;
                    try (var update = connection.prepareStatement("""
                            UPDATE notification_outbox
                            SET status = 'PROCESSING', attempts = ?,
                                next_attempt_at = CURRENT_TIMESTAMP + INTERVAL '5 minutes',
                                updated_at = CURRENT_TIMESTAMP
                            WHERE notification_id = ?""")) {
                        update.setInt(1, attempts);
                        update.setObject(2, id);
                        update.executeUpdate();
                    }
                    return new OutboxEvent(id, result.getObject("recipient_id", UUID.class),
                            result.getString("event"), result.getString("payload"), attempts);
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not claim notification outbox event.", exception);
            }
        });
    }

    @Override
    public void markSent(UUID notificationId) {
        database.withConnection(connection -> updateStatus(connection, notificationId,
                "SENT", null, null));
    }

    @Override
    public void markFailed(UUID notificationId, int attempts, String error) {
        database.withConnection(connection -> {
            String status = attempts >= 5 ? "FAILED" : "PENDING";
            updateStatus(connection, notificationId, status, error, attempts);
            return null;
        });
    }

    private static void updateStatus(java.sql.Connection connection, UUID id, String status,
            String error, Integer attempts) {
        try (var statement = connection.prepareStatement("""
                UPDATE notification_outbox
                SET status = ?, last_error = ?, attempts = COALESCE(?, attempts),
                    sent_at = CASE WHEN ? = 'SENT' THEN CURRENT_TIMESTAMP ELSE sent_at END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE notification_id = ?""")) {
            statement.setString(1, status);
            statement.setString(2, error);
            if (attempts == null) statement.setObject(3, null);
            else statement.setInt(3, attempts);
            statement.setString(4, status);
            statement.setObject(5, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update notification outbox event.", exception);
        }
    }
}
