package seedu.eventmanager.common;

/** Safe default until the deployment supplies a metrics backend. */
public final class NoopMetrics implements Metrics {
    @Override public void increment(String name) { }
    @Override public void setGauge(String name, long value) { }
}
