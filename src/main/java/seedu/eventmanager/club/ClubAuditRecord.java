package seedu.eventmanager.club;

import java.time.Instant;
import java.util.UUID;

/** Sanitized business audit data for a club change. */
public record ClubAuditRecord(Instant occurredAt, UUID actorId, Action action, UUID clubId) {
    public enum Action {
        CREATE_CLUB
    }
}
