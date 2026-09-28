package seedu.eventmanager.attendee;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.notification.*;

class InboxDispatcherTest {
    @Test void startsOnceOffCallerThreadAndStopsOnClose() throws Exception {
        var delivered = new CountDownLatch(1);
        var claims = new AtomicInteger();
        Thread caller = Thread.currentThread();
        var event = new NotificationOutboxRepository.OutboxEvent(UUID.randomUUID(), UUID.randomUUID(),
                "EVENT_ANNOUNCEMENT", "{}", 1);
        var repository = new NotificationOutboxRepository() {
            public OutboxEvent claimNext() { return claims.getAndIncrement() == 0 ? event : null; }
            public void markSent(UUID id) { delivered.countDown(); }
            public void markFailed(UUID id, int attempts, String error) { fail(error); }
        };
        var worker = new NotificationOutboxWorker(repository, ignored -> assertNotEquals(caller, Thread.currentThread()));
        var dispatcher = new InboxDispatcher();
        try {
            dispatcher.start(worker); dispatcher.start(worker);
            assertTrue(delivered.await(5, TimeUnit.SECONDS));
        } finally { dispatcher.close(); }
        dispatcher.start(worker); // A late initializer must not restart a closed app dispatcher.
    }
}
