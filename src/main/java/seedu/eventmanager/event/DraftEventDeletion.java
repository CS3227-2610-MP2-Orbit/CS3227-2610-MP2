package seedu.eventmanager.event;

import java.util.UUID;

/** Persists a draft soft-delete together with its venue clean-up and audit record. */
@FunctionalInterface
public interface DraftEventDeletion {
    /**
     * In one transaction: stores {@code deleted} if the event is still a draft at {@code expectedVersion},
     * withdraws a submitted venue request, releases an approved booking, and records {@code audit}.
     *
     * @throws EventVersionConflictException if the event changed or is no longer a draft
     */
    void deleteDraft(Event deleted, long expectedVersion, EventAuditRecord audit, UUID organizerId);
}
