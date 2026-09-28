package seedu.eventmanager.event;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import seedu.eventmanager.common.EntityNotFoundException;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.service.VenueRepository;
import seedu.eventmanager.service.VenueRequestRepository;
import seedu.eventmanager.venue.Venue;
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;
import seedu.eventmanager.venue.VenueRequestValidator;
import seedu.eventmanager.venue.VenueStatus;

/** Organizer workflow: submit a venue booking request for an owned event. */
public final class OrganizerVenueRequestService {
    private final EventService eventService;
    private final VenueRepository venues;
    private final VenueRequestRepository requests;
    private final EventService.IdGenerator idGenerator;
    private final VenueRelease release;

    public OrganizerVenueRequestService(
            EventService eventService,
            VenueRepository venues,
            VenueRequestRepository requests,
            EventService.IdGenerator idGenerator) {
        this(eventService, venues, requests, idGenerator, null);
    }

    /** Without a {@code release}, releasing an approved venue fails closed. */
    public OrganizerVenueRequestService(
            EventService eventService,
            VenueRepository venues,
            VenueRequestRepository requests,
            EventService.IdGenerator idGenerator,
            VenueRelease release) {
        this.eventService = Objects.requireNonNull(eventService, "eventService");
        this.venues = Objects.requireNonNull(venues, "venues");
        this.requests = Objects.requireNonNull(requests, "requests");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.release = release;
    }

    /**
     * Gives up an owned draft's approved venue so its times can change and a new request can be
     * submitted. Published events keep their booking.
     */
    public void releaseApprovedVenue(OrganizerIdentity actor, UUID eventId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(eventId, "eventId");
        Event event = eventService.getEvent(actor, eventId);
        if (event.status() != EventStatus.DRAFT) {
            throw new ValidationException("Published events keep their venue booking");
        }
        if (release == null) {
            throw new IllegalStateException("Venue release is not configured");
        }
        if (!release.releaseApprovedBooking(eventId, OrganizerIds.toUuid(actor.userId()))) {
            throw new ValidationException("This event has no approved venue booking to release");
        }
    }

    public VenueRequest submit(OrganizerIdentity actor, UUID eventId, UUID venueId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(venueId, "venueId");

        Event event = eventService.getEvent(actor, eventId);
        Venue venue = venues.findById(venueId);
        if (venue == null) {
            throw new EntityNotFoundException("Venue not found: " + venueId);
        }
        if (venue.status() != VenueStatus.ACTIVE) {
            throw new ValidationException("Only ACTIVE venues can be requested");
        }
        if (requests.findOpenByEventId(eventId).isPresent()) {
            throw new ValidationException("This event already has an open venue request");
        }
        if (requests.findLatestByEventId(eventId)
                .filter(latest -> latest.status() == VenueRequestStatus.APPROVED)
                .isPresent()) {
            throw new ValidationException(
                    "This event already has an approved venue. Release it under Request venue first.");
        }

        VenueRequest request = new VenueRequest(
                idGenerator.nextId(),
                event.id(),
                venue.venueId(),
                OrganizerIds.toUuid(actor.userId()),
                OffsetDateTime.ofInstant(event.startsAt(), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(event.endsAt(), ZoneOffset.UTC),
                event.capacity(),
                VenueRequestStatus.SUBMITTED);
        VenueRequestValidator.validate(request);
        requests.save(request);
        return request;
    }

    /**
     * Latest venue-request status for an owned event, if any.
     * Read-only; does not invent publish/lifecycle side effects.
     */
    public Optional<VenueRequest> latestRequest(OrganizerIdentity actor, UUID eventId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(eventId, "eventId");
        eventService.getEvent(actor, eventId);
        return requests.findLatestByEventId(eventId);
    }
}
