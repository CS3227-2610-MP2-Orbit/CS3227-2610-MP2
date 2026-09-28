package seedu.eventmanager.event;

/** Lifecycle states currently supported by the organizer event workflow. */
public enum EventStatus {
    DRAFT,
    PUBLISHED,
    COMPLETED,
    /** Soft-deleted draft: kept for the audit trail, hidden from every workflow. */
    DELETED
}
