package seedu.eventmanager.volunteer;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import seedu.eventmanager.common.EntityNotFoundException;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.event.EventService;
import seedu.eventmanager.event.OrganizerIdentity;
import seedu.eventmanager.registration.EventRegistrations;
import seedu.eventmanager.registration.RegisteredAttendee;

/** Organizer workflow: assign registered attendees as volunteers for an owned event. */
public final class VolunteerService {
    static final int MAX_ROLE_LENGTH = 60;

    private final EventService eventService;
    private final EventRegistrations registrations;
    private final VolunteerRepository volunteers;
    private final Clock clock;

    public VolunteerService(
            EventService eventService,
            EventRegistrations registrations,
            VolunteerRepository volunteers,
            Clock clock) {
        this.eventService = Objects.requireNonNull(eventService, "eventService");
        this.registrations = Objects.requireNonNull(registrations, "registrations");
        this.volunteers = Objects.requireNonNull(volunteers, "volunteers");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public AssignedVolunteer assign(OrganizerIdentity actor, UUID eventId, UUID attendeeId, String role) {
        requireOwnedEvent(actor, eventId);
        Objects.requireNonNull(attendeeId, "attendeeId");
        String normalizedRole = normalizeRole(role);

        RegisteredAttendee attendee = registrations.findRegisteredAttendee(eventId, attendeeId)
                .orElseThrow(() -> new ValidationException(
                        "Only attendees registered for this event can be assigned as volunteers"));
        if (volunteers.exists(eventId, attendeeId)) {
            throw new ValidationException("This attendee is already a volunteer for this event");
        }

        Instant now = clock.instant();
        volunteers.add(
                new VolunteerAssignment(eventId, attendeeId, normalizedRole, actor.userId(), now),
                new VolunteerAuditRecord(
                        now, actor.userId(), VolunteerAuditRecord.Action.ASSIGN_VOLUNTEER, eventId, attendeeId));
        return new AssignedVolunteer(attendeeId, Optional.of(attendee.displayName()), normalizedRole, now);
    }

    public void remove(OrganizerIdentity actor, UUID eventId, UUID attendeeId) {
        requireOwnedEvent(actor, eventId);
        Objects.requireNonNull(attendeeId, "attendeeId");

        VolunteerAuditRecord audit = new VolunteerAuditRecord(
                clock.instant(), actor.userId(), VolunteerAuditRecord.Action.REMOVE_VOLUNTEER, eventId, attendeeId);
        if (!volunteers.remove(eventId, attendeeId, audit)) {
            throw new EntityNotFoundException("This attendee is not a volunteer for this event");
        }
    }

    public List<AssignedVolunteer> listVolunteers(OrganizerIdentity actor, UUID eventId) {
        requireOwnedEvent(actor, eventId);
        Map<UUID, RegisteredAttendee> registered = registrations.registeredAttendees(eventId).stream()
                .collect(Collectors.toMap(RegisteredAttendee::attendeeId, Function.identity(), (a, b) -> a));
        return volunteers.findByEventId(eventId).stream()
                .map(assignment -> new AssignedVolunteer(
                        assignment.attendeeId(),
                        Optional.ofNullable(registered.get(assignment.attendeeId()))
                                .map(RegisteredAttendee::displayName),
                        assignment.role(),
                        assignment.assignedAt()))
                .toList();
    }

    /** Registered attendees for the event who are not yet volunteers. */
    public List<RegisteredAttendee> availableAttendees(OrganizerIdentity actor, UUID eventId) {
        requireOwnedEvent(actor, eventId);
        Set<UUID> assigned = volunteers.findByEventId(eventId).stream()
                .map(VolunteerAssignment::attendeeId)
                .collect(Collectors.toSet());
        return registrations.registeredAttendees(eventId).stream()
                .filter(attendee -> !assigned.contains(attendee.attendeeId()))
                .toList();
    }

    private void requireOwnedEvent(OrganizerIdentity actor, UUID eventId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(eventId, "eventId");
        eventService.getEvent(actor, eventId);
    }

    private static String normalizeRole(String role) {
        String normalized = role == null ? "" : role.strip();
        if (normalized.length() > MAX_ROLE_LENGTH) {
            throw new ValidationException("Volunteer role must not exceed " + MAX_ROLE_LENGTH + " characters");
        }
        return normalized;
    }
}
