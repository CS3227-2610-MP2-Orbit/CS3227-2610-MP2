package seedu.eventmanager.volunteer;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A persisted volunteer assignment: one attendee helping at one event. */
public record VolunteerAssignment(
        UUID eventId,
        UUID attendeeId,
        String role,
        String assignedBy,
        Instant assignedAt) {
    public VolunteerAssignment {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(attendeeId, "attendeeId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(assignedBy, "assignedBy");
        Objects.requireNonNull(assignedAt, "assignedAt");
    }
}
