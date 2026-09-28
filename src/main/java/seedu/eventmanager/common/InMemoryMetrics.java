package seedu.eventmanager.common;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Thread-safe in-process counters and gauges, summarised into the diagnostic log. */
public final class InMemoryMetrics implements Metrics {
    private final Map<String, AtomicLong> values = new ConcurrentHashMap<>();

    @Override public void increment(String name) {
        values.computeIfAbsent(requireName(name), ignored -> new AtomicLong()).incrementAndGet();
    }

    @Override public void setGauge(String name, long value) {
        values.computeIfAbsent(requireName(name), ignored -> new AtomicLong()).set(value);
    }

    /** Current values, sorted by name. */
    public Map<String, Long> snapshot() {
        Map<String, Long> snapshot = new TreeMap<>();
        values.forEach((name, value) -> snapshot.put(name, value.get()));
        return snapshot;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Metric name must not be blank.");
        }
        return name;
    }
}
