package seedu.eventmanager.common;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** JDK-backed structured logger; production adapters can replace this implementation. */
public final class JavaUtilStructuredLogger implements StructuredLogger {
    private final Logger logger;

    public JavaUtilStructuredLogger(Class<?> source) {
        logger = Logger.getLogger(source.getName());
    }

    @Override public void info(String event, Map<String, ?> fields) {
        logger.log(Level.INFO, format(event, fields));
    }

    @Override public void warn(String event, Map<String, ?> fields) {
        logger.log(Level.WARNING, format(event, fields));
    }

    @Override public void error(String event, Map<String, ?> fields, Throwable error) {
        logger.log(Level.SEVERE, format(event, fields), error);
    }

    private static String format(String event, Map<String, ?> fields) {
        StringBuilder message = new StringBuilder("event=").append(event);
        fields.forEach((key, value) -> message.append(' ').append(key).append('=').append(value));
        return message.toString();
    }
}
