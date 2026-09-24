package seedu.eventmanager.service;

import seedu.eventmanager.venue.VenueRequest;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface VenueRequestRepository {
    VenueRequest get(UUID requestId);
    void save(VenueRequest request);

    default List<VenueRequest> findSubmitted() {
        return List.of();
    }

    /** Open request for an event (`DRAFT` or `SUBMITTED`), if any. */
    default Optional<VenueRequest> findOpenByEventId(UUID eventId) {
        return Optional.empty();
    }

    /** Latest request for an event in any status (most recently updated/created). */
    default Optional<VenueRequest> findLatestByEventId(UUID eventId) {
        return Optional.empty();
    }

    /** Persists a decision while retaining the audit fields required by storage. */
    default void save(VenueRequest request, UUID decidedBy, String decisionReason) {
        save(request);
    }
}
