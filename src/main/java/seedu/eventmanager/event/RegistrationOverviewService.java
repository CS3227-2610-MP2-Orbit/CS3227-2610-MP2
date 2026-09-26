package seedu.eventmanager.event;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import seedu.eventmanager.registration.EventRegistrations;
import seedu.eventmanager.registration.RegisteredAttendee;

/** Organizer workflow: view who is registered for an owned event. */
public final class RegistrationOverviewService {
    private final EventService eventService;
    private final EventRegistrations registrations;

    public RegistrationOverviewService(EventService eventService, EventRegistrations registrations) {
        this.eventService = Objects.requireNonNull(eventService, "eventService");
        this.registrations = Objects.requireNonNull(registrations, "registrations");
    }

    public RegistrationOverview overview(OrganizerIdentity actor, UUID eventId) {
        Event event = eventService.getEvent(actor, eventId);
        List<RegisteredAttendee> attendees = registrations.registeredAttendees(event.id()).stream()
                .sorted(Comparator.comparing(RegisteredAttendee::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new RegistrationOverview(event.id(), event.capacity(), attendees);
    }
}
