package seedu.eventmanager.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
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
import seedu.eventmanager.registration.EventRegistrations;
import seedu.eventmanager.registration.RegisteredAttendee;

class RegistrationOverviewServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-25T04:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000a11c");
    private static final UUID BEN = UUID.fromString("00000000-0000-0000-0000-000000000be1");
    private static final UUID CHARLIE = UUID.fromString("00000000-0000-0000-0000-00000000c4a1");
    private static final OrganizerIdentity ORGANIZER =
            new OrganizerIdentity("organizer-1", Set.of("club-1"));
    private static final OrganizerIdentity OTHER_ORGANIZER =
            new OrganizerIdentity("organizer-2", Set.of("club-2"));

    private FakeRegistrations registrations;
    private InMemoryEventRepository events;
    private RegistrationOverviewService service;

    @BeforeEach
    void setUp() {
        events = new InMemoryEventRepository();
        EventService eventService = new EventService(events, () -> EVENT_ID, Clock.fixed(NOW, ZoneOffset.UTC));
        eventService.createEvent(ORGANIZER, "club-1", new EventDetails(
                "Campus Night",
                "Demo",
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T12:00:00Z"),
                80));
        events.writes = 0;
        registrations = new FakeRegistrations();
        service = new RegistrationOverviewService(eventService, registrations);
    }

    @Test
    void overview_ownedEvent_listsRegistrantsSortedByNameIgnoringCase() {
        registrations.register(EVENT_ID, CHARLIE, "charlie");
        registrations.register(EVENT_ID, BEN, "Ben");
        registrations.register(EVENT_ID, ALICE, "alice");

        RegistrationOverview overview = service.overview(ORGANIZER, EVENT_ID);

        assertEquals(
                List.of(
                        new RegisteredAttendee(ALICE, "alice"),
                        new RegisteredAttendee(BEN, "Ben"),
                        new RegisteredAttendee(CHARLIE, "charlie")),
                overview.attendees());
    }

    @Test
    void overview_reportsRegisteredCountAndEventCapacity() {
        registrations.register(EVENT_ID, ALICE, "Alice");
        registrations.register(EVENT_ID, BEN, "Ben");

        RegistrationOverview overview = service.overview(ORGANIZER, EVENT_ID);

        assertEquals(EVENT_ID, overview.eventId());
        assertEquals(2, overview.registeredCount());
        assertEquals(80, overview.capacity());
    }

    @Test
    void overview_noRegistrants_emptyListWithZeroCount() {
        RegistrationOverview overview = service.overview(ORGANIZER, EVENT_ID);

        assertTrue(overview.attendees().isEmpty());
        assertEquals(0, overview.registeredCount());
        assertEquals(80, overview.capacity());
    }

    @Test
    void overview_registrationsExceedCapacity_reportedAsIs() {
        events.shrinkCapacity(EVENT_ID, 1);
        registrations.register(EVENT_ID, ALICE, "Alice");
        registrations.register(EVENT_ID, BEN, "Ben");

        RegistrationOverview overview = service.overview(ORGANIZER, EVENT_ID);

        assertEquals(2, overview.registeredCount());
        assertEquals(1, overview.capacity());
    }

    @Test
    void overview_unownedEvent_rejectedWithoutReadingRegistrations() {
        registrations.register(EVENT_ID, ALICE, "Alice");

        assertThrows(AccessDeniedException.class, () -> service.overview(OTHER_ORGANIZER, EVENT_ID));

        assertEquals(0, registrations.queries);
    }

    @Test
    void overview_unknownEvent_rejectedWithoutReadingRegistrations() {
        UUID unknown = UUID.fromString("00000000-0000-0000-0000-00000000dead");

        assertThrows(EntityNotFoundException.class, () -> service.overview(ORGANIZER, unknown));

        assertEquals(0, registrations.queries);
    }

    @Test
    void overview_registrationSourceFails_errorPropagates() {
        IllegalStateException failure = new IllegalStateException("registrations unavailable");
        registrations.failWith = failure;

        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> service.overview(ORGANIZER, EVENT_ID));

        assertSame(failure, thrown);
    }

    @Test
    void overview_isReadOnly() {
        registrations.register(EVENT_ID, ALICE, "Alice");

        service.overview(ORGANIZER, EVENT_ID);

        assertEquals(0, events.writes);
        assertEquals(List.of(new RegisteredAttendee(ALICE, "Alice")), registrations.registeredAttendees(EVENT_ID));
    }

    private static final class FakeRegistrations implements EventRegistrations {
        private final Map<UUID, List<RegisteredAttendee>> byEvent = new HashMap<>();
        private int queries;
        private RuntimeException failWith;

        void register(UUID eventId, UUID attendeeId, String name) {
            byEvent.computeIfAbsent(eventId, ignored -> new ArrayList<>())
                    .add(new RegisteredAttendee(attendeeId, name));
        }

        @Override
        public List<RegisteredAttendee> registeredAttendees(UUID eventId) {
            queries++;
            if (failWith != null) {
                throw failWith;
            }
            return List.copyOf(byEvent.getOrDefault(eventId, List.of()));
        }
    }

    private static final class InMemoryEventRepository implements EventRepository {
        private final Map<UUID, Event> store = new HashMap<>();
        private int writes;

        void shrinkCapacity(UUID eventId, int capacity) {
            Event e = store.get(eventId);
            store.put(eventId, new Event(e.id(), e.clubId(), e.organizerId(), e.title(), e.description(),
                    e.startsAt(), e.endsAt(), capacity, e.status(), e.version()));
        }

        @Override
        public Optional<Event> findById(UUID eventId) {
            return Optional.ofNullable(store.get(eventId));
        }

        @Override
        public List<Event> findByClubIds(Set<String> clubIds) {
            return store.values().stream().filter(e -> clubIds.contains(e.clubId())).toList();
        }

        @Override
        public void create(Event event, EventAuditRecord auditRecord) {
            writes++;
            store.put(event.id(), event);
        }

        @Override
        public void update(Event event, long expectedVersion, EventAuditRecord auditRecord) {
            writes++;
            store.put(event.id(), event);
        }
    }
}
