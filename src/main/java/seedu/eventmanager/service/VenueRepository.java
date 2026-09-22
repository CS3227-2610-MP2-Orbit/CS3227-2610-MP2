package seedu.eventmanager.service;

import java.util.List;
import java.util.UUID;
import seedu.eventmanager.venue.Venue;

public interface VenueRepository {
    List<Venue> findAll();
    Venue findById(UUID venueId);
    void save(Venue venue);
}
