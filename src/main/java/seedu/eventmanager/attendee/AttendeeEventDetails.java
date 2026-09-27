package seedu.eventmanager.attendee;

import java.util.Optional;
import seedu.eventmanager.registration.Registration;
import seedu.eventmanager.registration.RegistrationEligibilityPolicy;

/** Public event information plus only the authenticated attendee's own status. */
public record AttendeeEventDetails(CatalogueEvent event, Optional<Venue> venue, int occupiedSeats,
        int remainingSeats, Optional<Registration.Status> ownStatus, long ownRegistrationVersion,
        RegistrationEligibilityPolicy.Result eligibility) {
    public record Venue(String name, String location, String bookingStatus, String venueStatus) { }
}
