package seedu.eventmanager.registration;

import java.util.List;
import java.util.UUID;

/** Placeholder until attendee registration is implemented: no event has registrants. */
public final class NoEventRegistrations implements EventRegistrations {
    @Override
    public List<RegisteredAttendee> registeredAttendees(UUID eventId) {
        return List.of();
    }
}
