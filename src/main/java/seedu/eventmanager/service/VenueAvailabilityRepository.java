package seedu.eventmanager.service;

import java.util.List;
import java.util.UUID;
import seedu.eventmanager.venue.VenueAvailability;

public interface VenueAvailabilityRepository {
    List<VenueAvailability> findAll();
    void save(VenueAvailability availability);
    void delete(UUID availabilityId);
}
