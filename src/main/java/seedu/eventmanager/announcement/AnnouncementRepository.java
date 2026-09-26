package seedu.eventmanager.announcement;

import java.util.List;
import java.util.UUID;

/** Persistence for announcements. Each write must store its audit record atomically with the change. */
public interface AnnouncementRepository {
    /** Announcements for the event, newest first. */
    List<Announcement> findByEventId(UUID eventId);

    void add(Announcement announcement, AnnouncementAuditRecord auditRecord);

    /**
     * Permanently removes the announcement if it belongs to {@code eventId}, writing the audit record in the
     * same transaction. Returns {@code false}, with nothing written, when no such announcement exists.
     */
    boolean delete(UUID eventId, UUID announcementId, AnnouncementAuditRecord auditRecord);
}
