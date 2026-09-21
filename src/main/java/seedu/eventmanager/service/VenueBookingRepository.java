package seedu.eventmanager.service;

import seedu.eventmanager.venue.VenueRequest;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface VenueBookingRepository {
    boolean hasConflict(UUID venueId, OffsetDateTime startsAt, OffsetDateTime endsAt);
    void createFromApprovedRequest(VenueRequest request, UUID approverId);
}
