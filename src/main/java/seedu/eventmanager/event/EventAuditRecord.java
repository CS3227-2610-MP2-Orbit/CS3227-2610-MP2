package seedu.eventmanager.event;

import java.time.Instant;
import java.util.UUID;

/** Sanitized business audit data for an event mutation. */
public record EventAuditRecord(
        Instant occurredAt,
        String actorId,
        Action action,
        UUID eventId,
        long resultingVersion) {

    public enum Action {
        CREATE_EVENT,
        EDIT_EVENT
    }
}
