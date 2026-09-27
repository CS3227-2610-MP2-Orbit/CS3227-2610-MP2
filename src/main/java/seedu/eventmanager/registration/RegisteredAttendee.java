package seedu.eventmanager.registration;

import java.util.Objects;
import java.util.UUID;

/** An attendee account currently registered for an event, as exposed to other roles. */
public record RegisteredAttendee(UUID attendeeId, String displayName) {
    public RegisteredAttendee {
        Objects.requireNonNull(attendeeId, "attendeeId");
        Objects.requireNonNull(displayName, "displayName");
    }
}
