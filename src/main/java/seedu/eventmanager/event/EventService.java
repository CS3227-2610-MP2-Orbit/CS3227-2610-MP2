package seedu.eventmanager.event;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import seedu.eventmanager.common.AccessDeniedException;
import seedu.eventmanager.common.EntityNotFoundException;
import seedu.eventmanager.common.ValidationException;

/** Organizer application workflow for creating, viewing, and editing events. */
public final class EventService {
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 5_000;

    private final EventRepository repository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    /** Supplies deterministic event identifiers at the application boundary. */
    @FunctionalInterface
    public interface IdGenerator {
        UUID nextId();
    }

    public EventService(EventRepository repository, IdGenerator idGenerator, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Event createEvent(OrganizerIdentity actor, String clubId, EventDetails details) {
        requireActor(actor);
        String normalizedClubId = requireClubId(clubId);
        requireOwnership(actor, normalizedClubId);
        EventDetails validDetails = validate(details);
        UUID eventId = Objects.requireNonNull(idGenerator.nextId(), "generated event ID");

        Event event = new Event(
                eventId,
                normalizedClubId,
                actor.userId(),
                validDetails.title(),
                validDetails.description(),
                validDetails.startsAt(),
                validDetails.endsAt(),
                validDetails.capacity(),
                EventStatus.DRAFT,
                0);
        EventAuditRecord auditRecord = new EventAuditRecord(
                clock.instant(),
                actor.userId(),
                EventAuditRecord.Action.CREATE_EVENT,
                eventId,
                event.version());
        repository.create(event, auditRecord);
        return event;
    }

    public Event editEvent(
            OrganizerIdentity actor, UUID eventId, long expectedVersion, EventDetails details) {
        requireActor(actor);
        Objects.requireNonNull(eventId, "eventId");
        if (expectedVersion < 0) {
            throw new ValidationException("Expected version must not be negative");
        }

        Event current = findExisting(eventId);
        requireOwnership(actor, current.clubId());
        if (current.version() != expectedVersion) {
            throw new EventVersionConflictException("Event was modified by another operation");
        }
        if (current.status() != EventStatus.DRAFT) {
            throw new ValidationException("Only draft events can be edited in this workflow");
        }
        EventDetails validDetails = validate(details);

        Event edited = new Event(
                current.id(),
                current.clubId(),
                current.organizerId(),
                validDetails.title(),
                validDetails.description(),
                validDetails.startsAt(),
                validDetails.endsAt(),
                validDetails.capacity(),
                current.status(),
                current.version() + 1);
        EventAuditRecord auditRecord = new EventAuditRecord(
                clock.instant(),
                actor.userId(),
                EventAuditRecord.Action.EDIT_EVENT,
                eventId,
                edited.version());
        repository.update(edited, expectedVersion, auditRecord);
        return edited;
    }

    public Event getEvent(OrganizerIdentity actor, UUID eventId) {
        requireActor(actor);
        Objects.requireNonNull(eventId, "eventId");
        Event event = findExisting(eventId);
        requireOwnership(actor, event.clubId());
        return event;
    }

    public List<Event> listEvents(OrganizerIdentity actor) {
        requireActor(actor);
        return List.copyOf(repository.findByClubIds(actor.ownedClubIds()));
    }

    private Event findExisting(UUID eventId) {
        return repository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
    }

    private static void requireActor(OrganizerIdentity actor) {
        Objects.requireNonNull(actor, "actor");
    }

    private static String requireClubId(String clubId) {
        if (clubId == null || clubId.isBlank()) {
            throw new ValidationException("Club ID must not be blank");
        }
        return clubId.strip();
    }

    private static void requireOwnership(OrganizerIdentity actor, String clubId) {
        if (!actor.ownedClubIds().contains(clubId)) {
            throw new AccessDeniedException("Organizer does not own this club's events");
        }
    }

    private static EventDetails validate(EventDetails details) {
        if (details == null) {
            throw new ValidationException("Event details are required");
        }
        String title = details.title();
        if (title == null || title.isBlank()) {
            throw new ValidationException("Event title must not be blank");
        }
        title = title.strip();
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new ValidationException("Event title must not exceed " + MAX_TITLE_LENGTH + " characters");
        }

        String description = details.description() == null ? "" : details.description().strip();
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException(
                    "Event description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }

        Instant startsAt = details.startsAt();
        Instant endsAt = details.endsAt();
        if (startsAt == null || endsAt == null) {
            throw new ValidationException("Event start and end times are required");
        }
        if (!startsAt.isBefore(endsAt)) {
            throw new ValidationException("Event start time must be before its end time");
        }
        if (details.capacity() <= 0) {
            throw new ValidationException("Event capacity must be positive");
        }

        return new EventDetails(title, description, startsAt, endsAt, details.capacity());
    }
}
