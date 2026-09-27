package seedu.eventmanager.attendee;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.function.Function;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.registration.AttendeeSessionGuard;
import seedu.eventmanager.registration.Registration;
import seedu.eventmanager.registration.RegistrationEvent;
import seedu.eventmanager.registration.CheckInPolicy;

/** Enriches the existing owner-only registration read, including cancelled and past records. */
public final class MyRegistrationsService {
    private final Function<String, List<Registration>> registrations;
    private final RegistrationEventInfoRepository events;
    private final AttendeeSessionGuard sessions;
    private final Clock clock;

    public MyRegistrationsService(Function<String, List<Registration>> registrations,
            RegistrationEventInfoRepository events, Function<String, Actor> sessions, Clock clock) {
        this.registrations = Objects.requireNonNull(registrations);
        this.events = Objects.requireNonNull(events);
        this.sessions = new AttendeeSessionGuard(sessions);
        this.clock = Objects.requireNonNull(clock);
    }

    public List<MyRegistration> list(String sessionToken) {
        Actor actor = sessions.require(sessionToken);
        var records = registrations.apply(sessionToken);
        // Fail closed even if a future collaborator violates the owner-only contract.
        if (records.stream().anyMatch(row -> !row.attendeeId().equals(actor.userId()))) {
            throw new ApplicationException("FORBIDDEN", "Only your own registrations are available.");
        }
        var metadata = records.isEmpty() ? Map.<UUID, RegistrationEventInfoRepository.EventInfo>of()
                : events.findAll(records.stream().map(Registration::eventId).collect(Collectors.toSet()));
        if (!actor.equals(sessions.require(sessionToken))) {
            throw new ApplicationException("UNAUTHENTICATED", "Please log in again.");
        }
        var now = clock.instant();
        return records.stream().map(row -> {
            var event = Objects.requireNonNull(metadata.get(row.eventId()), "Registered event metadata unavailable");
            return new MyRegistration(row.eventId(), event.title(), event.startsAt(), event.venue(),
                    row.status(), row.version(), row.status() == Registration.Status.CONFIRMED && now.isBefore(event.startsAt()),
                    event.endsAt(), event.clubId(), event.description(), event.eventStatus(),
                    CheckInPolicy.evaluate(new RegistrationEvent(row.eventId(), event.eventStatus(), 0,
                            event.startsAt(), event.endsAt()), row.status(), event.confirmedActiveBooking(), now)
                            == CheckInPolicy.Result.AVAILABLE);
        }).toList();
    }
}
