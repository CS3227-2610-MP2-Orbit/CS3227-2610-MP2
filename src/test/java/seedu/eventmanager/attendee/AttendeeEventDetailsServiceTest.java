package seedu.eventmanager.attendee;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.*;
import seedu.eventmanager.registration.*;
import static seedu.eventmanager.registration.RegistrationEligibilityPolicy.*;

class AttendeeEventDetailsServiceTest {
    final Instant now = Instant.parse("2030-01-01T00:00:00Z");
    final UUID alice = UUID.randomUUID();
    final UUID bob = UUID.randomUUID();
    final UUID eventId = UUID.randomUUID();
    int reads;
    String status = "PUBLISHED";
    Instant start = now.plusSeconds(60);
    int occupied = 2;
    Booking booking = Booking.CONFIRMED_ACTIVE;
    boolean missing;

    AttendeeEventDetailsService service() {
        return new AttendeeEventDetailsService((event, attendee) -> {
            reads++;
            if (missing) return Optional.empty();
            return Optional.of(new AttendeeEventDetailsRepository.Snapshot(
                    new CatalogueEvent(eventId, "club", "Workshop", "Description", start, start.plusSeconds(3600), 3),
                    status, Optional.of(new AttendeeEventDetails.Venue("Room", "Level 1", "CONFIRMED", "ACTIVE")),
                    booking, occupied, attendee.equals(alice) ? Optional.of(Registration.Status.CONFIRMED) : Optional.empty()));
        }, token -> switch (token) {
            case "alice" -> new Actor(alice, Role.ATTENDEE);
            case "bob" -> new Actor(bob, Role.ATTENDEE);
            case "organizer" -> new Actor(bob, Role.CLUB_ORGANIZER);
            default -> throw new IllegalArgumentException("Synthetic expired session");
        }, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test void returnsVenueSeatsAndOnlyOwnStatus() {
        var details = service().getEvent("alice", eventId);
        assertNotNull(details);
        assertEquals("Room", details.venue().orElseThrow().name());
        assertEquals(1, details.remainingSeats());
        assertEquals(Optional.of(Registration.Status.CONFIRMED), details.ownStatus());
        assertTrue(service().getEvent("bob", eventId).ownStatus().isEmpty());
    }

    @Test void rejectsMissingExpiredAndWrongRoleBeforeReading() {
        for (String token : Arrays.asList(null, "", "expired", "organizer")) {
            var failure = assertThrows(ApplicationException.class, () -> service().getEvent(token, eventId));
            assertEquals("organizer".equals(token) ? "FORBIDDEN" : "UNAUTHENTICATED", failure.code());
        }
        assertEquals(0, reads);
    }

    @Test void revalidatesVisibilityIncludingExactStartAndMissingEvent() {
        for (String hidden : List.of("DRAFT", "COMPLETED")) {
            status = hidden;
            assertThrows(EntityNotFoundException.class, () -> service().getEvent("alice", eventId));
        }
        status = "PUBLISHED";
        start = now;
        assertThrows(EntityNotFoundException.class, () -> service().getEvent("alice", eventId));
        start = now.plusSeconds(1); missing = true;
        assertThrows(EntityNotFoundException.class, () -> service().getEvent("alice", eventId));
    }

    @Test void fullAndUnconfirmedEventsRemainVisibleWithReasonsAndNonnegativeSeats() {
        occupied = 4;
        var details = service().getEvent("alice", eventId);
        assertNotNull(details);
        assertEquals(0, details.remainingSeats());
        assertEquals(Result.EVENT_FULL, details.eligibility());
        assertEquals(Optional.of(Registration.Status.CONFIRMED), details.ownStatus());
        booking = Booking.UNCONFIRMED;
        assertEquals(Result.VENUE_NOT_CONFIRMED, service().getEvent("bob", eventId).eligibility());
        booking = Booking.VENUE_INACTIVE;
        assertEquals(Result.VENUE_INACTIVE, service().getEvent("bob", eventId).eligibility());
    }

    @Test void policyHasDeterministicBoundaryAndReasonPriority() {
        var event = new RegistrationEvent(eventId, "PUBLISHED", 3, start, start.plusSeconds(3600));
        assertEquals(Result.AVAILABLE, evaluate(event, now, Booking.CONFIRMED_ACTIVE, 2));
        assertEquals(Result.EVENT_FULL, evaluate(event, now, Booking.CONFIRMED_ACTIVE, 3));
        assertEquals(Result.VENUE_INACTIVE, evaluate(event, now, Booking.VENUE_INACTIVE, 2));
        assertEquals(Result.VENUE_NOT_CONFIRMED, evaluate(event, now, Booking.UNCONFIRMED, 3));
        assertEquals(Result.EVENT_NOT_REGISTERABLE, evaluate(event, start, Booking.CONFIRMED_ACTIVE, 0));
    }

    @Test void revocationDuringReadDoesNotReturnPersonalDetails() {
        var service = new AttendeeEventDetailsService((event, attendee) -> {
            reads++;
            return Optional.empty();
        }, token -> {
            if (reads > 0) throw new IllegalArgumentException("Revoked");
            return new Actor(alice, Role.ATTENDEE);
        }, Clock.fixed(now, ZoneOffset.UTC));
        assertEquals("UNAUTHENTICATED", assertThrows(ApplicationException.class,
                () -> service.getEvent("alice", eventId)).code());
    }
}
