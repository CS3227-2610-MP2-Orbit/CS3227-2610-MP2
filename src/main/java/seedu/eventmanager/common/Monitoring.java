package seedu.eventmanager.common;

/** Process-wide metrics for the desktop app; the shell logs a summary on exit. */
public final class Monitoring {
    private static final InMemoryMetrics METRICS = new InMemoryMetrics();

    private Monitoring() { }

    public static InMemoryMetrics metrics() {
        return METRICS;
    }
}
