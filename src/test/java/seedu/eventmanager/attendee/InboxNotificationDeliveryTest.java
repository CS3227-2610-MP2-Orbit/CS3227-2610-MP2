package seedu.eventmanager.attendee;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.notification.NotificationOutboxRepository.OutboxEvent;

class InboxNotificationDeliveryTest {
    @Test void rejectsUnknownTypeBeforeAnyPersistence() {
        var writes = new AtomicInteger();
        var delivery = new InboxNotificationDelivery(event -> writes.incrementAndGet());
        var failure = assertThrows(ApplicationException.class, () -> delivery.deliver(
                new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), "UNKNOWN", "{}", 1)));
        assertEquals("UNSUPPORTED_NOTIFICATION", failure.code());
        assertEquals(0, writes.get());
    }

    @Test void routesAllThreeSupportedTypesToStorage() {
        var writes = new AtomicInteger();
        var delivery = new InboxNotificationDelivery(event -> writes.incrementAndGet());
        for (String type : InboxNotificationDelivery.EVENT_TYPES) {
            delivery.deliver(new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), type, "{}", 1));
        }
        assertEquals(3, writes.get());
    }
}
