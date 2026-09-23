package seedu.eventmanager.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.AccessDeniedException;
import seedu.eventmanager.common.EntityNotFoundException;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.service.VenueRepository;
import seedu.eventmanager.service.VenueRequestRepository;
import seedu.eventmanager.venue.Venue;
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;
import seedu.eventmanager.venue.VenueStatus;

class OrganizerVenueRequestServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-23T10:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID VENUE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID REQUEST_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final OrganizerIdentity ORGANIZER =
            new OrganizerIdentity("demo-organizer", Set.of("club-1"));
    private static final OrganizerIdentity OTHER =
            new OrganizerIdentity("other-organizer", Set.of("club-2"));

    private InMemoryEventRepository events;
    private FakeVenueRepository venues;
    private FakeVenueRequestRepository requests;
    private EventService eventService;
    private OrganizerVenueRequestService service;

    @BeforeEach
    void setUp() {
        events = new InMemoryEventRepository();
        venues = new FakeVenueRepository();
        requests = new FakeVenueRequestRepository();
        eventService = new EventService(events, () -> EVENT_ID, Clock.fixed(NOW, ZoneOffset.UTC));
        service = new OrganizerVenueRequestService(
                eventService, venues, requests, () -> REQUEST_ID);

        EventDetails details = new EventDetails(
                "Campus Night",
                "An evening event",
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T12:00:00Z"),
                80);
        eventService.createEvent(ORGANIZER, "club-1", details);
        venues.save(new Venue(VENUE_ID, "LT1", "COM1", 120, "Lecture theatre", VenueStatus.ACTIVE));
    }

    @Test
    void submit_ownedEventActiveVenue_persistsSubmittedRequest() {
        VenueRequest saved = service.submit(ORGANIZER, EVENT_ID, VENUE_ID);

        assertEquals(REQUEST_ID, saved.requestId());
        assertEquals(EVENT_ID, saved.eventId());
        assertEquals(VENUE_ID, saved.venueId());
        assertEquals(OrganizerIds.toUuid("demo-organizer"), saved.organizerId());
        assertEquals(OffsetDateTime.ofInstant(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC),
                saved.startsAt());
        assertEquals(OffsetDateTime.ofInstant(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC),
                saved.endsAt());
        assertEquals(80, saved.expectedAttendance());
        assertEquals(VenueRequestStatus.SUBMITTED, saved.status());
        assertEquals(1, requests.saved.size());
        assertEquals(saved, requests.saved.getFirst());
    }

    @Test
    void submit_unownedEvent_rejectedWithoutPersistence() {
        assertThrows(AccessDeniedException.class,
                () -> service.submit(OTHER, EVENT_ID, VENUE_ID));
        assertTrue(requests.saved.isEmpty());
    }

    @Test
    void submit_unknownVenue_rejectedWithoutPersistence() {
        assertThrows(EntityNotFoundException.class,
                () -> service.submit(ORGANIZER, EVENT_ID, UUID.randomUUID()));
        assertTrue(requests.saved.isEmpty());
    }

    @Test
    void submit_inactiveVenue_rejectedWithoutPersistence() {
        UUID inactiveId = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
        venues.save(new Venue(inactiveId, "Closed", "COM2", 50, "", VenueStatus.INACTIVE));

        assertThrows(ValidationException.class,
                () -> service.submit(ORGANIZER, EVENT_ID, inactiveId));
        assertTrue(requests.saved.isEmpty());
    }

    @Test
    void submit_missingEvent_rejectedWithoutPersistence() {
        assertThrows(EntityNotFoundException.class,
                () -> service.submit(ORGANIZER, UUID.randomUUID(), VENUE_ID));
        assertTrue(requests.saved.isEmpty());
    }

    @Test
    void submit_duplicateOpenSubmitted_rejectedWithoutSecondSave() {
        service.submit(ORGANIZER, EVENT_ID, VENUE_ID);
        VenueRequest first = requests.saved.getFirst();

        assertThrows(ValidationException.class,
                () -> service.submit(ORGANIZER, EVENT_ID, VENUE_ID));
        assertEquals(1, requests.saved.size());
        assertEquals(first, requests.saved.getFirst());
    }

    private static final class InMemoryEventRepository implements EventRepository {
        private final Map<UUID, Event> store = new HashMap<>();
        private final List<EventAuditRecord> auditRecords = new ArrayList<>();

        @Override
        public Optional<Event> findById(UUID eventId) {
            return Optional.ofNullable(store.get(eventId));
        }

        @Override
        public List<Event> findByClubIds(Set<String> clubIds) {
            return store.values().stream()
                    .filter(event -> clubIds.contains(event.clubId()))
                    .toList();
        }

        @Override
        public void create(Event event, EventAuditRecord auditRecord) {
            store.put(event.id(), event);
            auditRecords.add(auditRecord);
        }

        @Override
        public void update(Event event, long expectedVersion, EventAuditRecord auditRecord) {
            store.put(event.id(), event);
            auditRecords.add(auditRecord);
        }
    }

    private static final class FakeVenueRepository implements VenueRepository {
        private final Map<UUID, Venue> byId = new HashMap<>();

        @Override
        public List<Venue> findAll() {
            return List.copyOf(byId.values());
        }

        @Override
        public Venue findById(UUID venueId) {
            return byId.get(venueId);
        }

        @Override
        public void save(Venue venue) {
            byId.put(venue.venueId(), venue);
        }
    }

    private static final class FakeVenueRequestRepository implements VenueRequestRepository {
        private final List<VenueRequest> saved = new ArrayList<>();
        private final Map<UUID, VenueRequest> byId = new HashMap<>();

        @Override
        public VenueRequest get(UUID requestId) {
            return byId.get(requestId);
        }

        @Override
        public void save(VenueRequest request) {
            saved.add(request);
            byId.put(request.requestId(), request);
        }

        @Override
        public Optional<VenueRequest> findOpenByEventId(UUID eventId) {
            return saved.stream()
                    .filter(request -> request.eventId().equals(eventId))
                    .filter(request -> request.status() == VenueRequestStatus.SUBMITTED
                            || request.status() == VenueRequestStatus.DRAFT)
                    .findFirst();
        }
    }
}
