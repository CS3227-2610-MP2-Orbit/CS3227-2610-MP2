package seedu.eventmanager.event;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import seedu.eventmanager.common.AccessDeniedException;
import seedu.eventmanager.common.EntityNotFoundException;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.service.VenueRequestRepository;
import seedu.eventmanager.venue.VenueRequest;

/** Organizer application workflow for creating, viewing, and editing events. */
public final class EventService {
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 5_000;
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter
            .ofPattern("d MMM uuuu, h:mm a 'SGT'", Locale.ENGLISH)
            .withZone(ZoneId.of("Asia/Singapore"));

    private final EventRepository repository;
    private final VenueRequestRepository venueRequests;
    private final EventBookingCheck bookingCheck;
    private final DraftEventDeletion deletion;
    private final IdGenerator idGenerator;
    private final Clock clock;

    /** Supplies deterministic event identifiers at the application boundary. */
    @FunctionalInterface
    public interface IdGenerator {
        UUID nextId();
    }

    public EventService(EventRepository repository, IdGenerator idGenerator, Clock clock) {
        this(repository, idGenerator, clock, null);
    }

    public EventService(
            EventRepository repository,
            IdGenerator idGenerator,
            Clock clock,
            VenueRequestRepository venueRequests) {
        this(repository, idGenerator, clock, venueRequests, null);
    }

    /** Without a {@code bookingCheck}, publishing fails closed instead of skipping the booking rule. */
    public EventService(
            EventRepository repository,
            IdGenerator idGenerator,
            Clock clock,
            VenueRequestRepository venueRequests,
            EventBookingCheck bookingCheck) {
        this(repository, idGenerator, clock, venueRequests, bookingCheck, null);
    }

    /** Without a {@code deletion} port, deleting fails closed. */
    public EventService(
            EventRepository repository,
            IdGenerator idGenerator,
            Clock clock,
            VenueRequestRepository venueRequests,
            EventBookingCheck bookingCheck,
            DraftEventDeletion deletion) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.venueRequests = venueRequests;
        this.bookingCheck = bookingCheck;
        this.deletion = deletion;
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

    /**
     * Updates a draft event. When capacity changes, syncs expected attendance on any open
     * ({@code DRAFT}/{@code SUBMITTED}) venue request; decided requests are left alone.
     */
    public CapacityUpdateResult editEvent(
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
        requireTimesKeepBooking(current, validDetails);

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

        if (current.capacity() == edited.capacity()) {
            return CapacityUpdateResult.noOpenRequest(edited);
        }
        return syncOpenRequestAttendance(edited);
    }

    /**
     * Publishes an owned draft so Attendees can browse and register. Requires a future start and a
     * CONFIRMED booking at an ACTIVE venue matching the event times, mirroring registration eligibility.
     */
    public Event publishEvent(OrganizerIdentity actor, UUID eventId, long expectedVersion) {
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
            throw new ValidationException("Only draft events can be published");
        }
        Instant now = clock.instant();
        if (!now.isBefore(current.startsAt())) {
            throw new ValidationException("Events can only be published before they start");
        }
        if (bookingCheck == null) {
            throw new IllegalStateException("Venue booking check is not configured");
        }
        if (!bookingCheck.hasConfirmedActiveBooking(eventId, current.startsAt(), current.endsAt())) {
            throw new ValidationException(publishBookingProblem(current));
        }

        Event published = new Event(
                current.id(),
                current.clubId(),
                current.organizerId(),
                current.title(),
                current.description(),
                current.startsAt(),
                current.endsAt(),
                current.capacity(),
                EventStatus.PUBLISHED,
                current.version() + 1);
        repository.update(published, expectedVersion, new EventAuditRecord(
                now, actor.userId(), EventAuditRecord.Action.PUBLISH_EVENT, eventId, published.version()));
        return published;
    }

    /**
     * Soft-deletes an owned draft. Its submitted venue request is withdrawn and any approved booking
     * released in the same transaction; published events cannot be deleted.
     */
    public Event deleteEvent(OrganizerIdentity actor, UUID eventId, long expectedVersion) {
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
            throw new ValidationException("Only draft events can be deleted");
        }
        if (deletion == null) {
            throw new IllegalStateException("Event deletion is not configured");
        }

        Event deleted = new Event(
                current.id(),
                current.clubId(),
                current.organizerId(),
                current.title(),
                current.description(),
                current.startsAt(),
                current.endsAt(),
                current.capacity(),
                EventStatus.DELETED,
                current.version() + 1);
        deletion.deleteDraft(deleted, expectedVersion, new EventAuditRecord(
                clock.instant(), actor.userId(), EventAuditRecord.Action.DELETE_EVENT, eventId, deleted.version()),
                OrganizerIds.toUuid(actor.userId()));
        return deleted;
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
        return repository.findByClubIds(actor.ownedClubIds()).stream()
                .filter(event -> event.status() != EventStatus.DELETED)
                .toList();
    }

    private CapacityUpdateResult syncOpenRequestAttendance(Event edited) {
        if (venueRequests == null) {
            return CapacityUpdateResult.noOpenRequest(edited);
        }

        UUID eventId = edited.id();
        int newCapacity = edited.capacity();
        Optional<VenueRequest> open = venueRequests.findOpenByEventId(eventId);
        if (open.isPresent()) {
            VenueRequest currentRequest = open.get();
            boolean changed = venueRequests.updateExpectedAttendance(
                    currentRequest.requestId(), newCapacity);
            if (!changed) {
                return CapacityUpdateResult.decidedUnchanged(edited);
            }
            VenueRequest after = new VenueRequest(
                    currentRequest.requestId(),
                    currentRequest.eventId(),
                    currentRequest.venueId(),
                    currentRequest.organizerId(),
                    currentRequest.startsAt(),
                    currentRequest.endsAt(),
                    newCapacity,
                    currentRequest.status());
            return CapacityUpdateResult.synced(edited, after);
        }

        Optional<VenueRequest> latest = venueRequests.findLatestByEventId(eventId);
        if (CapacityUpdateResult.classifyWithoutOpen(latest)
                == CapacityUpdateResult.SyncStatus.DECIDED_REQUEST_UNCHANGED) {
            return CapacityUpdateResult.decidedUnchanged(edited);
        }
        return CapacityUpdateResult.noOpenRequest(edited);
    }

    /** Once a venue is approved, times may only stay the same or return to the booked window. */
    private void requireTimesKeepBooking(Event current, EventDetails details) {
        boolean timesUnchanged = details.startsAt().equals(current.startsAt())
                && details.endsAt().equals(current.endsAt());
        if (timesUnchanged || bookingCheck == null) {
            return;
        }
        bookingCheck.findActiveBooking(current.id())
                .filter(booking -> !booking.startsAt().equals(details.startsAt())
                        || !booking.endsAt().equals(details.endsAt()))
                .ifPresent(booking -> {
                    throw new ValidationException("The venue is booked for " + window(booking)
                            + ". To change the times, first release the venue under Request venue.");
                });
    }

    private String publishBookingProblem(Event event) {
        Optional<EventBookingCheck.ActiveBooking> booking = bookingCheck.findActiveBooking(event.id());
        if (booking.isEmpty()) {
            return "Publishing needs a confirmed venue booking at an active venue matching the event times";
        }
        EventBookingCheck.ActiveBooking current = booking.get();
        if (!current.startsAt().equals(event.startsAt()) || !current.endsAt().equals(event.endsAt())) {
            return "The venue is booked for " + window(current)
                    + ". Change the event times back to match before publishing.";
        }
        if (!current.venueActive()) {
            return "The booked venue is not active, so the event cannot be published";
        }
        return "The venue booking is not confirmed, so the event cannot be published";
    }

    private static String window(EventBookingCheck.ActiveBooking booking) {
        return DISPLAY_TIME.format(booking.startsAt()) + " – " + DISPLAY_TIME.format(booking.endsAt());
    }

    private Event findExisting(UUID eventId) {
        return repository.findById(eventId)
                .filter(event -> event.status() != EventStatus.DELETED)
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
