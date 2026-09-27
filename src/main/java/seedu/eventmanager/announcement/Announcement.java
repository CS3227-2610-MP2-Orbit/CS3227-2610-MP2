package seedu.eventmanager.announcement;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A message an organizer posted for one of their events. Announcements are immutable once posted. */
public record Announcement(UUID id, UUID eventId, String authorId, String message, Instant createdAt) {
    public Announcement {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(authorId, "authorId");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
