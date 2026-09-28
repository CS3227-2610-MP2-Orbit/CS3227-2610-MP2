package seedu.eventmanager.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class InMemoryMetricsTest {
    @Test
    void countersAccumulateAndGaugesKeepTheLatestValue() {
        InMemoryMetrics metrics = new InMemoryMetrics();
        metrics.increment("logins.attendee");
        metrics.increment("logins.attendee");
        metrics.setGauge("venues.available", 4);
        metrics.setGauge("venues.available", 3);

        assertEquals(Map.of("logins.attendee", 2L, "venues.available", 3L), metrics.snapshot());
        assertEquals(List.of("logins.attendee", "venues.available"), List.copyOf(metrics.snapshot().keySet()));
    }

    @Test
    void concurrentIncrementsAreNotLost() throws Exception {
        InMemoryMetrics metrics = new InMemoryMetrics();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 1_000; i++) {
            pool.submit(() -> metrics.increment("requests"));
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(1_000L, metrics.snapshot().get("requests"));
    }

    @Test
    void blankNamesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new InMemoryMetrics().increment(" "));
    }
}
