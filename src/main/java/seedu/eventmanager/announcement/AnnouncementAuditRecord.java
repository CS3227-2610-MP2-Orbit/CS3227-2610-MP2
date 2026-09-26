package seedu.eventmanager.announcement;

import java.time.Instant;
import java.util.UUID;

/** Sanitized business audit data for a posted or deleted announcement; the message text is not copied here. */
public record AnnouncementAuditRecord(
        Instant occurredAt,
        String actorId,
        Action action,
        UUID eventId,
        UUID announcementId) {

    public enum Action {
        POST_ANNOUNCEMENT,
        DELETE_ANNOUNCEMENT
    }
}
