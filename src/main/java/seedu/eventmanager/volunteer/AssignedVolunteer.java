package seedu.eventmanager.volunteer;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Organizer-facing view of a volunteer. {@code displayName} is empty when the attendee
 * is no longer registered for the event.
 */
public record AssignedVolunteer(
        UUID attendeeId,
        Optional<String> displayName,
        String role,
        Instant assignedAt) {
    public AssignedVolunteer {
        Objects.requireNonNull(attendeeId, "attendeeId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(assignedAt, "assignedAt");
    }

    public boolean currentlyRegistered() {
        return displayName.isPresent();
    }
}
