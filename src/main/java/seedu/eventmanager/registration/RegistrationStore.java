package seedu.eventmanager.registration;

import java.util.List;
import java.util.UUID;

/** Persistence boundary; lock methods require the shared transaction manager. */
public interface RegistrationStore {
    RegistrationEvent lockEvent(UUID eventId);
    boolean lockActiveAttendee(UUID attendeeId);
    boolean lockConfirmedActiveBooking(RegistrationEvent event);
    Registration find(UUID eventId, UUID attendeeId);
    int occupiedPlaces(UUID eventId);
    void save(Registration registration);
    List<Registration> findByAttendee(UUID attendeeId);
}
