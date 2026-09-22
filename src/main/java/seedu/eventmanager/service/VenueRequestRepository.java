package seedu.eventmanager.service;

import seedu.eventmanager.venue.VenueRequest;
import java.util.UUID;
import java.util.List;

public interface VenueRequestRepository {
    VenueRequest get(UUID requestId);
    void save(VenueRequest request);

    default List<VenueRequest> findSubmitted() {
        return List.of();
    }

    /** Persists a decision while retaining the audit fields required by storage. */
    default void save(VenueRequest request, UUID decidedBy, String decisionReason) {
        save(request);
    }
}
