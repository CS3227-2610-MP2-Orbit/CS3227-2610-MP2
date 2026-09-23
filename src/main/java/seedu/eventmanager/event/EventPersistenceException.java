package seedu.eventmanager.event;

/** Raised when event persistence fails without exposing connection details or credentials. */
public class EventPersistenceException extends RuntimeException {
    public EventPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
