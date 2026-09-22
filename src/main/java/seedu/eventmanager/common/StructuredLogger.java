package seedu.eventmanager.common;

import java.util.Map;

/** Structured logging boundary shared by application services. */
public interface StructuredLogger {
    void info(String event, Map<String, ?> fields);
    void warn(String event, Map<String, ?> fields);
    void error(String event, Map<String, ?> fields, Throwable error);
}
