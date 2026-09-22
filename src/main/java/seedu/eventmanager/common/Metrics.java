package seedu.eventmanager.common;

/** Metrics boundary. Implementations may publish counters and gauges to the deployment platform. */
public interface Metrics {
    void increment(String name);
    void setGauge(String name, long value);
}
