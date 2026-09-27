package seedu.eventmanager.registration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.*;
import seedu.eventmanager.service.TransactionManager;

class RegistrationServiceTest {
    final Instant now = Instant.parse("2030-01-01T00:00:00Z");
    final UUID eventId = UUID.randomUUID();
    final UUID alice = UUID.randomUUID();
    final UUID bob = UUID.randomUUID();
    final MemoryStore store = new MemoryStore();
    final List<String> audits = new ArrayList<>();
    final List<Map<String, String>> notices = new ArrayList<>();
    RegistrationService service;
    Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        store.event = new RegistrationEvent(eventId, "PUBLISHED", 1, now.plusSeconds(1), now.plusSeconds(7200));
        service = new RegistrationService(store, token -> switch (token) {
            case "alice" -> new Actor(alice, Role.ATTENDEE);
            case "bob" -> new Actor(bob, Role.ATTENDEE);
            case "organizer" -> new Actor(bob, Role.CLUB_ORGANIZER);
            default -> throw new IllegalArgumentException("Synthetic expired session");
        }, new TransactionManager() {
            public <T> T execute(Supplier<T> action) { return action.get(); }
        }, (actor, action, type, id, previous, next, reason) -> audits.add(action),
                (recipient, event, data) -> notices.add(data), clock);
    }

    @Test
    void registerCancelAndReRegisterKeepIdentityAndProduceDistinctTransitionEffects() {
        Registration first = service.register("alice", eventId, -1);
        assertNotNull(first, "Successful registration must be returned");
        assertEquals(Registration.Status.CONFIRMED, first.status());
        assertEquals(alice, first.attendeeId());
        assertEquals(0, first.version());
        assertEquals(List.of(first), service.myRegistrations("alice"));
        assertTrue(service.myRegistrations("bob").isEmpty());
        assertEquals(first, service.register("alice", eventId, -1));
        assertEquals(1, audits.size());
        Registration cancelled = service.cancel("alice", eventId, 0);
        assertEquals(Registration.Status.CANCELLED, cancelled.status());
        assertEquals(now, cancelled.cancelledAt());
        assertEquals(cancelled, service.cancel("alice", eventId, 0));
        Registration again = service.register("alice", eventId, cancelled.version());
        assertEquals(first.id(), again.id());
        assertEquals(2, again.version());
        assertNull(again.cancelledAt());
        assertEquals(3, audits.size());
        assertEquals(3, new HashSet<>(notices).size(), "Re-registration must not deduplicate against old confirmation");
        assertEquals(1, store.records.size());
    }

    @Test
    void authenticationRoleAndOwnershipAreEnforcedBeforeChanges() {
        code("UNAUTHENTICATED", () -> service.register(null, eventId, -1));
        code("UNAUTHENTICATED", () -> service.register("expired", eventId, -1));
        code("FORBIDDEN", () -> service.register("organizer", eventId, -1));
        Registration own = service.register("alice", eventId, -1);
        code("REGISTRATION_NOT_FOUND", () -> service.cancel("bob", eventId, 0));
        assertEquals(own, store.find(eventId, alice));
        assertEquals(1, audits.size());
    }

    @Test
    void requiresPublishedFutureEventConfirmedBookingAndActiveVenue() {
        store.event = new RegistrationEvent(eventId, "DRAFT", 1, now.plusSeconds(1), now.plusSeconds(2));
        code("EVENT_NOT_REGISTERABLE", () -> service.register("alice", eventId, -1));
        store.event = new RegistrationEvent(eventId, "PUBLISHED", 1, now, now.plusSeconds(2));
        code("EVENT_NOT_REGISTERABLE", () -> service.register("alice", eventId, -1));
        store.event = new RegistrationEvent(eventId, "PUBLISHED", 1, now.plusSeconds(1), now.plusSeconds(2));
        store.booking = false;
        code("VENUE_NOT_CONFIRMED", () -> service.register("alice", eventId, -1));
        store.booking = true;
        store.active = false;
        code("FORBIDDEN", () -> service.register("alice", eventId, -1));
        assertTrue(store.records.isEmpty());
        assertTrue(audits.isEmpty());
        assertTrue(notices.isEmpty());
    }

    @Test
    void fullEventRejectsAnotherAttendeeAndCancellationFreesOnePlace() {
        service.register("alice", eventId, -1);
        code("EVENT_FULL", () -> service.register("bob", eventId, -1));
        service.cancel("alice", eventId, 0);
        assertNotNull(service.register("bob", eventId, -1));
        assertEquals(1, store.occupiedPlaces(eventId));
    }

    @Test
    void staleCancellationCannotUndoANewRegistration() {
        service.register("alice", eventId, -1);
        service.cancel("alice", eventId, 0);
        var latest = service.register("alice", eventId, 1);
        code("REGISTRATION_CHANGED", () -> service.cancel("alice", eventId, 0));
        assertEquals(latest, store.find(eventId, alice));
        assertEquals(3, audits.size());
    }

    @Test
    void cancellationAtStartAndCheckedInCancellationAreRejected() {
        var first = service.register("alice", eventId, -1);
        store.event = new RegistrationEvent(eventId, "PUBLISHED", 1, now, now.plusSeconds(2));
        code("CANCELLATION_CLOSED", () -> service.cancel("alice", eventId, 0));
        store.event = new RegistrationEvent(eventId, "PUBLISHED", 1, now.plusSeconds(1), now.plusSeconds(2));
        store.records.put(alice, new Registration(first.id(), eventId, alice, Registration.Status.CHECKED_IN,
                now, null, now, 1));
        code("ALREADY_CHECKED_IN", () -> service.cancel("alice", eventId, 1));
    }

    @Test
    void waitingForBookingLockCannotAdmitRegistrationAfterStart() {
        clock = new Clock() {
            int calls;
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return calls++ == 0 ? now : now.plusSeconds(2); }
        };
        setup();
        code("EVENT_NOT_REGISTERABLE", () -> service.register("alice", eventId, -1));
        assertTrue(store.records.isEmpty());
        assertTrue(audits.isEmpty());
    }

    @Test
    void invalidVersionAndUnknownEventFailWithoutEffects() {
        code("INVALID_VERSION", () -> service.register("alice", eventId, -2));
        code("EVENT_NOT_FOUND", () -> service.register("alice", UUID.randomUUID(), -1));
        code("REGISTRATION_CHANGED", () -> service.register("alice", eventId, 8));
        assertTrue(audits.isEmpty());
    }

    static void code(String expected, Runnable work) {
        assertEquals(expected, assertThrows(ApplicationException.class, work::run).code());
    }

    final class MemoryStore implements RegistrationStore {
        RegistrationEvent event;
        boolean booking = true;
        boolean active = true;
        final Map<UUID, Registration> records = new HashMap<>();
        public RegistrationEvent lockEvent(UUID id) { return eventId.equals(id) ? event : null; }
        public boolean lockActiveAttendee(UUID id) { return active; }
        public boolean lockConfirmedActiveBooking(RegistrationEvent event) { return booking; }
        public Registration find(UUID event, UUID attendee) { return records.get(attendee); }
        public int occupiedPlaces(UUID event) {
            return (int) records.values().stream().filter(r -> r.status() != Registration.Status.CANCELLED).count();
        }
        public void save(Registration value) { records.put(value.attendeeId(), value); }
        public List<Registration> findByAttendee(UUID attendee) {
            return records.containsKey(attendee) ? List.of(records.get(attendee)) : List.of();
        }
    }
}
