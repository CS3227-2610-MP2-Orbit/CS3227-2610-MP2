package seedu.eventmanager.event;

/** Raised when an edit is based on an outdated event version. */
public class EventVersionConflictException extends RuntimeException {
    public EventVersionConflictException(String message) {
        super(message);
    }
}
