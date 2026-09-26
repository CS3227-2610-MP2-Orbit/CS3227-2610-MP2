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

    default List<VenueRequestDisplay> findSubmittedDisplay() {
        return findSubmitted().stream()
                .map(request -> new VenueRequestDisplay(request, null, null, null, null))
                .toList();
    }

    /** Open request for an event (`DRAFT` or `SUBMITTED`), if any. */
    default Optional<VenueRequest> findOpenByEventId(UUID eventId) {
        return Optional.empty();
    }

    /** Latest request for an event in any status (most recently updated/created). */
    default Optional<VenueRequest> findLatestByEventId(UUID eventId) {
        return Optional.empty();
    }

    /**
     * Updates expected attendance for an open request only (`DRAFT` / `SUBMITTED`).
     * @return true if a row was updated
     */
    default boolean updateExpectedAttendance(UUID requestId, int expectedAttendance) {
        return false;
    }

    /** Persists a decision while retaining the audit fields required by storage. */
    default void save(VenueRequest request, UUID decidedBy, String decisionReason) {
        save(request);
    }
}
