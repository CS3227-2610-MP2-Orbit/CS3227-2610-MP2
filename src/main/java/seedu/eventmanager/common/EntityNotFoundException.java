package seedu.eventmanager.common;

/** Raised when a requested domain entity does not exist. */
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
