package seedu.eventmanager.service;

import seedu.eventmanager.venue.VenueRequest;
import java.util.UUID;

public interface VenueRequestRepository {
    VenueRequest get(UUID requestId);
    void save(VenueRequest request);
}
