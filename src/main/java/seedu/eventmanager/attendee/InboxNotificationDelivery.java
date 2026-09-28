package seedu.eventmanager.attendee;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.notification.NotificationDelivery;
import seedu.eventmanager.notification.NotificationOutboxRepository.OutboxEvent;

/** Routes only supported attendee events to durable inbox storage. */
public final class InboxNotificationDelivery implements NotificationDelivery {
    public static final Set<String> EVENT_TYPES = Set.of(
            "REGISTRATION_CONFIRMED", "REGISTRATION_CANCELLED", "EVENT_ANNOUNCEMENT");
    private final Consumer<OutboxEvent> persist;

    public InboxNotificationDelivery(Consumer<OutboxEvent> persist) {
        this.persist = Objects.requireNonNull(persist);
    }

    @Override public void deliver(OutboxEvent event) {
        Objects.requireNonNull(event);
        if (event.event() == null || !EVENT_TYPES.contains(event.event())) {
            throw new ApplicationException("UNSUPPORTED_NOTIFICATION", "Unsupported attendee notification type.");
        }
        persist.accept(event);
    }
}
