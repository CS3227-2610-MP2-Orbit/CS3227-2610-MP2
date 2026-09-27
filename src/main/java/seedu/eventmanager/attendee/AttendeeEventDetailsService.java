package seedu.eventmanager.attendee;

import java.time.Clock;
import java.util.UUID;
import java.util.Objects;
import java.util.function.Function;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.common.EntityNotFoundException;
import seedu.eventmanager.registration.AttendeeSessionGuard;
import seedu.eventmanager.registration.RegistrationEvent;
import seedu.eventmanager.registration.RegistrationEligibilityPolicy;

/** Authenticated read-only details. Availability is a snapshot, never a seat reservation. */
public final class AttendeeEventDetailsService {
    private final AttendeeEventDetailsRepository repository;
    private final AttendeeSessionGuard sessions;
    private final Clock clock;

    public AttendeeEventDetailsService(AttendeeEventDetailsRepository repository,
            Function<String, Actor> sessions, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.sessions = new AttendeeSessionGuard(sessions);
        this.clock = Objects.requireNonNull(clock);
    }

    public AttendeeEventDetails getEvent(String sessionToken, UUID eventId) {
        Actor actor = sessions.require(sessionToken);
        if (eventId == null) throw unavailable();
        var result = repository.find(eventId, actor.userId());
        // Do not show an in-flight personalized response after revocation or account switching.
        if (!actor.equals(sessions.require(sessionToken))) {
            throw new ApplicationException("UNAUTHENTICATED", "Please log in again.");
        }
        var snapshot = result.orElseThrow(AttendeeEventDetailsService::unavailable);
        var event = snapshot.event();
        var registrationEvent = new RegistrationEvent(event.id(), snapshot.eventStatus(), event.capacity(),
                event.startsAt(), event.endsAt());
        var now = clock.instant();
        if (!event.id().equals(eventId) || !RegistrationEligibilityPolicy.eventOpen(registrationEvent, now)) {
            throw unavailable();
        }
        return new AttendeeEventDetails(event, snapshot.venue(), snapshot.occupiedSeats(),
                Math.max(0, event.capacity() - snapshot.occupiedSeats()), snapshot.ownStatus(), snapshot.ownRegistrationVersion(),
                RegistrationEligibilityPolicy.evaluate(registrationEvent, now, snapshot.booking(), snapshot.occupiedSeats()));
    }

    private static EntityNotFoundException unavailable() {
        return new EntityNotFoundException("Event is not available in the public catalogue.");
    }
}
