package seedu.eventmanager.notification;

import java.util.Objects;
import seedu.eventmanager.common.StructuredLogger;

/** Local delivery adapter used until email or in-app delivery is configured. */
public final class LoggingNotificationDelivery implements NotificationDelivery {
    private final StructuredLogger logger;

    public LoggingNotificationDelivery(StructuredLogger logger) {
        this.logger = Objects.requireNonNull(logger);
    }

    @Override
    public void deliver(NotificationOutboxRepository.OutboxEvent event) {
        logger.info("notification_delivered_locally", java.util.Map.of(
                "notificationId", event.notificationId().toString(),
                "recipientId", event.recipientId().toString(),
                "event", event.event()));
    }
}
