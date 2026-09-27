package seedu.eventmanager.event;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import seedu.eventmanager.registration.RegisteredAttendee;

/** Read-only snapshot of who is registered for an event, for the owning organizer. */
public record RegistrationOverview(UUID eventId, int capacity, List<RegisteredAttendee> attendees) {
    public RegistrationOverview {
        Objects.requireNonNull(eventId, "eventId");
        attendees = List.copyOf(attendees);
    }

    public int registeredCount() {
        return attendees.size();
    }
}
