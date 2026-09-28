package seedu.eventmanager.attendee;

import java.util.Optional;
import java.util.UUID;
import seedu.eventmanager.registration.Registration;
import seedu.eventmanager.registration.RegistrationEligibilityPolicy;

/** Internal read boundary; the service supplies the attendee ID from a live session. */
public interface AttendeeEventDetailsRepository {
    Optional<Snapshot> find(UUID eventId, UUID attendeeId);

    record Snapshot(CatalogueEvent event, String eventStatus, Optional<AttendeeEventDetails.Venue> venue,
            RegistrationEligibilityPolicy.Booking booking, int occupiedSeats,
            Optional<Registration.Status> ownStatus) { }
}
