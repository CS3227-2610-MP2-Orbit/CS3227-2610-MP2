package seedu.eventmanager.volunteer;

import java.time.Instant;
import java.util.UUID;

/** Sanitized business audit data for a volunteer assignment change. */
public record VolunteerAuditRecord(
        Instant occurredAt,
        String actorId,
        Action action,
        UUID eventId,
        UUID attendeeId) {

    public enum Action {
        ASSIGN_VOLUNTEER,
        REMOVE_VOLUNTEER
    }
}
