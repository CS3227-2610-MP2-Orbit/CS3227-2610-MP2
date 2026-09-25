package seedu.eventmanager.registration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only view of who is registered for an event. Implemented by the Attendee
 * registration feature; consumed by Organizer features such as volunteer assignment.
 */
public interface EventRegistrations {
    List<RegisteredAttendee> registeredAttendees(UUID eventId);

    default Optional<RegisteredAttendee> findRegisteredAttendee(UUID eventId, UUID attendeeId) {
        return registeredAttendees(eventId).stream()
                .filter(attendee -> attendee.attendeeId().equals(attendeeId))
                .findFirst();
    }
}
