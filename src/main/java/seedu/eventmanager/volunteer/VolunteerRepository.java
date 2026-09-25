package seedu.eventmanager.volunteer;

import java.util.List;
import java.util.UUID;

/** Persistence boundary for volunteer assignments and their atomic audit records. */
public interface VolunteerRepository {
    List<VolunteerAssignment> findByEventId(UUID eventId);

    boolean exists(UUID eventId, UUID attendeeId);

    /** Stores the assignment and its audit record together; rejects duplicates. */
    void add(VolunteerAssignment assignment, VolunteerAuditRecord auditRecord);

    /**
     * Deletes the assignment and records the audit entry together.
     * @return false (and no audit) if no such assignment exists
     */
    boolean remove(UUID eventId, UUID attendeeId, VolunteerAuditRecord auditRecord);
}
