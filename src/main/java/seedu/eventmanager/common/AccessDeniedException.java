package seedu.eventmanager.common;

/** Raised when an authenticated user is not allowed to perform an operation. */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
