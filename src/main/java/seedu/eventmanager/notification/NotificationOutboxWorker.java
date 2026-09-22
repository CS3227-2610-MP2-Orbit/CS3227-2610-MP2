package seedu.eventmanager.notification;

import java.util.Objects;

/** Processes one pending notification at a time with bounded retries. */
public final class NotificationOutboxWorker {
    private static final int MAX_ATTEMPTS = 5;
    private final NotificationOutboxRepository repository;
    private final NotificationDelivery delivery;

    public NotificationOutboxWorker(NotificationOutboxRepository repository,
            NotificationDelivery delivery) {
        this.repository = Objects.requireNonNull(repository);
        this.delivery = Objects.requireNonNull(delivery);
    }

    public boolean processOnce() {
        NotificationOutboxRepository.OutboxEvent event = repository.claimNext();
        if (event == null) {
            return false;
        }
        try {
            delivery.deliver(event);
            repository.markSent(event.notificationId());
        } catch (RuntimeException exception) {
            repository.markFailed(event.notificationId(), event.attempts(), safeMessage(exception));
        }
        return true;
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
