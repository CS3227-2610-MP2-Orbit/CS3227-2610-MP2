package seedu.eventmanager.attendee;

import java.util.Optional;
import seedu.eventmanager.registration.CheckInPolicy;
import seedu.eventmanager.registration.Registration;
import seedu.eventmanager.registration.RegistrationEligibilityPolicy;

/** Public event information plus only the authenticated attendee's own status. */
public record AttendeeEventDetails(CatalogueEvent event, Optional<Venue> venue, int occupiedSeats,
        int remainingSeats, Optional<Registration.Status> ownStatus, long ownRegistrationVersion,
        RegistrationEligibilityPolicy.Result eligibility, boolean canCheckIn,
        CheckInPolicy.Result checkInAvailability) {
    public AttendeeEventDetails {
        if (checkInAvailability != null) canCheckIn = checkInAvailability == CheckInPolicy.Result.AVAILABLE;
    }

    public AttendeeEventDetails(CatalogueEvent event, Optional<Venue> venue, int occupiedSeats,
            int remainingSeats, Optional<Registration.Status> ownStatus, long ownRegistrationVersion,
            RegistrationEligibilityPolicy.Result eligibility, CheckInPolicy.Result checkInAvailability) {
        this(event, venue, occupiedSeats, remainingSeats, ownStatus, ownRegistrationVersion,
                eligibility, checkInAvailability == CheckInPolicy.Result.AVAILABLE,
                java.util.Objects.requireNonNull(checkInAvailability));
    }

    /** Compatibility for callers that have only a boolean preview, not an explanatory result. */
    public AttendeeEventDetails(CatalogueEvent event, Optional<Venue> venue, int occupiedSeats,
            int remainingSeats, Optional<Registration.Status> ownStatus, long ownRegistrationVersion,
            RegistrationEligibilityPolicy.Result eligibility, boolean canCheckIn) {
        this(event, venue, occupiedSeats, remainingSeats, ownStatus, ownRegistrationVersion,
                eligibility, canCheckIn, null);
    }
    public AttendeeEventDetails(CatalogueEvent event, Optional<Venue> venue, int occupiedSeats,
            int remainingSeats, Optional<Registration.Status> ownStatus, long ownRegistrationVersion,
            RegistrationEligibilityPolicy.Result eligibility) {
        this(event, venue, occupiedSeats, remainingSeats, ownStatus, ownRegistrationVersion, eligibility, false);
    }
    public record Venue(String name, String location, String bookingStatus, String venueStatus) { }
}
