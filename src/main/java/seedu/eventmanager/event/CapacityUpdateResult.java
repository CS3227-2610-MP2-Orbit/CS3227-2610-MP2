package seedu.eventmanager.event;

import java.util.Objects;
import java.util.Optional;
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;

/** Outcome of an Organizer draft edit when capacity may sync a pending venue request. */
public record CapacityUpdateResult(
        Event event,
        SyncStatus syncStatus,
        Optional<VenueRequest> openRequestAfterUpdate) {

    public enum SyncStatus {
        /** Open DRAFT/SUBMITTED request attendance was updated. */
        PENDING_REQUEST_SYNCED,
        /** No open venue request for this event. */
        NO_OPEN_REQUEST,
        /** Latest request is already decided; attendance left unchanged. */
        DECIDED_REQUEST_UNCHANGED
    }

    public CapacityUpdateResult {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(syncStatus, "syncStatus");
        openRequestAfterUpdate = openRequestAfterUpdate == null ? Optional.empty() : openRequestAfterUpdate;
    }

    public static CapacityUpdateResult synced(Event event, VenueRequest request) {
        return new CapacityUpdateResult(event, SyncStatus.PENDING_REQUEST_SYNCED, Optional.of(request));
    }

    public static CapacityUpdateResult noOpenRequest(Event event) {
        return new CapacityUpdateResult(event, SyncStatus.NO_OPEN_REQUEST, Optional.empty());
    }

    public static CapacityUpdateResult decidedUnchanged(Event event) {
        return new CapacityUpdateResult(event, SyncStatus.DECIDED_REQUEST_UNCHANGED, Optional.empty());
    }

    static SyncStatus classifyWithoutOpen(Optional<VenueRequest> latest) {
        if (latest.isEmpty()) {
            return SyncStatus.NO_OPEN_REQUEST;
        }
        VenueRequestStatus status = latest.get().status();
        if (status == VenueRequestStatus.APPROVED
                || status == VenueRequestStatus.REJECTED
                || status == VenueRequestStatus.CANCELLED
                || status == VenueRequestStatus.WITHDRAWN) {
            return SyncStatus.DECIDED_REQUEST_UNCHANGED;
        }
        return SyncStatus.NO_OPEN_REQUEST;
    }
}
