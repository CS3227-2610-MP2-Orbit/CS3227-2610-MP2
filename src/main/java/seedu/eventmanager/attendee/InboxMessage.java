package seedu.eventmanager.attendee;

import java.time.Instant;
import java.util.UUID;

/** Display text resolved for an authenticated owner at read time, never persisted as a copy. */
public record InboxMessage(UUID id, Instant createdAt, Instant readAt, String title, String body) {
    public boolean unread() { return readAt == null; }
}
