package seedu.eventmanager.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationOutboxWorkerTest {
    @Test
    void successfulDeliveryIsMarkedSent() {
        FakeRepository repository = new FakeRepository();
        NotificationOutboxWorker worker = new NotificationOutboxWorker(repository, event -> { });

        assertTrue(worker.processOnce());
        assertEquals("SENT", repository.status);
    }

    @Test
    void failedDeliveryIsReturnedForRetry() {
        FakeRepository repository = new FakeRepository();
        NotificationOutboxWorker worker = new NotificationOutboxWorker(repository,
                event -> { throw new IllegalStateException("delivery unavailable"); });

        assertTrue(worker.processOnce());
        assertEquals("FAILED", repository.status);
        assertEquals("delivery unavailable", repository.error);
    }

    private static final class FakeRepository implements NotificationOutboxRepository {
        private final OutboxEvent event = new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(),
                "VENUE_REQUEST_APPROVED", "{}", 5);
        private String status;
        private String error;
        @Override public OutboxEvent claimNext() { return status == null ? event : null; }
        @Override public void markSent(UUID id) { status = "SENT"; }
        @Override public void markFailed(UUID id, int attempts, String message) {
            status = "FAILED";
            error = message;
        }
    }
}
