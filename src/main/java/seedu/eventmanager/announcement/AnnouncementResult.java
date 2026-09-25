package seedu.eventmanager.announcement;

import java.util.Objects;

/**
 * Outcome of posting an announcement. {@code notificationsQueued} counts outbox entries created,
 * not deliveries to attendees.
 */
public record AnnouncementResult(Announcement announcement, int notificationsQueued, int notificationFailures) {
    public AnnouncementResult {
        Objects.requireNonNull(announcement, "announcement");
    }
}
