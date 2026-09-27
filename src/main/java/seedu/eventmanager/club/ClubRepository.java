package seedu.eventmanager.club;

import java.util.List;
import java.util.UUID;

/**
 * Persistence for clubs. {@code create} must store the club and its audit record atomically and
 * reject a name that already exists, ignoring case, with a {@code ValidationException}.
 */
public interface ClubRepository {
    List<Club> findByOwner(UUID ownerId);

    boolean existsByNameIgnoreCase(String name);

    void create(Club club, ClubAuditRecord auditRecord);
}
