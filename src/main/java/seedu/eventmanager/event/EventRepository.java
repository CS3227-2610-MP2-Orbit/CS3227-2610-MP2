package seedu.eventmanager.event;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence boundary for events and their atomic business audit records. */
public interface EventRepository {
    Optional<Event> findById(UUID eventId);

    List<Event> findByClubIds(java.util.Set<String> clubIds);

    void create(Event event, EventAuditRecord auditRecord);

    void update(Event event, long expectedVersion, EventAuditRecord auditRecord);
}
