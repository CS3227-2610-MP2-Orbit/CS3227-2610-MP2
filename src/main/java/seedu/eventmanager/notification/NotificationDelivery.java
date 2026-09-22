package seedu.eventmanager.notification;

public interface NotificationDelivery {
    void deliver(NotificationOutboxRepository.OutboxEvent event);
}
