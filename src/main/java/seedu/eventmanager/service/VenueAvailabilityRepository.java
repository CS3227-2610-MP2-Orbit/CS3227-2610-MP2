package seedu.eventmanager.service;

import java.util.List;
import seedu.eventmanager.venue.VenueAvailability;

public interface VenueAvailabilityRepository {
    List<VenueAvailability> findAll();
    void save(VenueAvailability availability);
}
