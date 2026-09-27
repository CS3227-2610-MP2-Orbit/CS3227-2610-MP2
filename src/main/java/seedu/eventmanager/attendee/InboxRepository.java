package seedu.eventmanager.attendee;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Internal owner-scoped persistence. Caller identity is resolved by InboxService, not controls. */
public interface InboxRepository {
    List<Entry> list(UUID owner);
    boolean markRead(UUID owner, UUID notificationId, Instant now);
    void markAllRead(UUID owner, Instant now);

    record Entry(UUID id, Instant createdAt, Instant readAt, String type,
            String eventTitle, Instant eventStart, String announcementText) { }
}
