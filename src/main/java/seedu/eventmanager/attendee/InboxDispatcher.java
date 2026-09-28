package seedu.eventmanager.attendee;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import seedu.eventmanager.common.JavaUtilStructuredLogger;
import seedu.eventmanager.notification.NotificationOutboxWorker;

/** App-lifetime background dispatcher; started after inbox bootstrap, stopped on app shutdown. */
public final class InboxDispatcher implements AutoCloseable {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("attendee-inbox-delivery").factory());
    private boolean started;
    private boolean closed;

    public synchronized void start(NotificationOutboxWorker worker) {
        if (closed || started) return;
        started = true;
        executor.scheduleWithFixedDelay(() -> {
            try {
                // Bound each batch so shutdown and other consumers can make progress.
                for (int i = 0; i < 25 && !Thread.currentThread().isInterrupted() && worker.processOnce(); i++) {
                    // processOnce() does the work; the loop only bounds the batch.
                }
            } catch (RuntimeException failure) {
                new JavaUtilStructuredLogger(InboxDispatcher.class).warn("attendee_inbox_dispatch_failed",
                        Map.of("failureType", failure.getClass().getSimpleName()));
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    @Override public synchronized void close() {
        closed = true;
        executor.shutdownNow();
    }
}
