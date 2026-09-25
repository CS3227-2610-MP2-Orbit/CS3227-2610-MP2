package seedu.eventmanager.notification;

import java.util.UUID;

public interface NotificationOutboxRepository {
    OutboxEvent claimNext();
    void markSent(UUID notificationId);
    void markFailed(UUID notificationId, int attempts, String error);

    record OutboxEvent(UUID notificationId, UUID recipientId, String event,
            String payload, int attempts) { }
}
