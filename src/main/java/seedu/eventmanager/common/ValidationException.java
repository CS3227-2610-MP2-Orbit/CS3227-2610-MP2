package seedu.eventmanager.common;

/** Raised when a command contains invalid business data. */
public class ValidationException extends IllegalArgumentException {
    public ValidationException(String message) {
        super(message);
    }
}
