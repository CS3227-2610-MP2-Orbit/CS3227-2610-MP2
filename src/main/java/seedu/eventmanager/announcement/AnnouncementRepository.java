package seedu.eventmanager.announcement;

import java.util.List;
import java.util.UUID;

/** Persistence for announcements. {@code add} must store the announcement and its audit record atomically. */
public interface AnnouncementRepository {
    /** Announcements for the event, newest first. */
    List<Announcement> findByEventId(UUID eventId);

    void add(Announcement announcement, AnnouncementAuditRecord auditRecord);
}
