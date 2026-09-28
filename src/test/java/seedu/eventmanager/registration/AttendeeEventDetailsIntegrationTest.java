package seedu.eventmanager.registration;

import static org.junit.jupiter.api.Assertions.*;
import static seedu.eventmanager.registration.RegistrationServiceTest.code;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.attendee.*;
import seedu.eventmanager.common.*;
import seedu.eventmanager.storage.*;
import seedu.eventmanager.venue.*;
import static seedu.eventmanager.registration.RegistrationEligibilityPolicy.Result.*;

/** Reuses the isolated-schema registration fixtures; never uses the interactive demo DB. */
class AttendeeEventDetailsIntegrationTest extends RegistrationDatabaseTest {
    @Test void carriesOwnVersionIncludingCancelledInsteadOfTreatingItAsAbsent() throws Exception {
        UUID event = event("PUBLISHED", 5, NOW.plusSeconds(3600));
        UUID alice = account("alice", Role.ATTENDEE);
        String token = login("alice");
        assertEquals(-1, service().getEvent(token, event).ownRegistrationVersion());
        registration(event, alice, "CANCELLED");
        sql("UPDATE event_registrations SET version=7");
        assertEquals(7, service().getEvent(token, event).ownRegistrationVersion());
        account("bob", Role.ATTENDEE);
        assertEquals(-1, service().getEvent(login("bob"), event).ownRegistrationVersion());
    }
    AttendeeEventDetailsService service() {
        return new AttendeeEventDetailsService(new JdbcAttendeeEventDetailsRepository(database),
                sessions::resolve, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test void countsReservedSeatsIncludingInactiveAccountsButExcludesCancelledAndOtherEvents() throws Exception {
        UUID event = event("PUBLISHED", 5, NOW.plusSeconds(3600));
        UUID alice = account("alice", Role.ATTENDEE);
        UUID bob = account("bob", Role.ATTENDEE);
        UUID inactive = account("inactive", Role.ATTENDEE);
        UUID cancelled = account("cancelled", Role.ATTENDEE);
        registration(event, alice, "CONFIRMED"); registration(event, bob, "CHECKED_IN");
        registration(event, inactive, "CONFIRMED"); registration(event, cancelled, "CANCELLED");
        registration(event("PUBLISHED", 5, NOW.plusSeconds(3600)), alice, "CONFIRMED");
        sql("UPDATE users SET active=false WHERE username='inactive'");
        booking(event);
        var details = service().getEvent(login("alice"), event);
        assertEquals(3, details.occupiedSeats());
        assertEquals(2, details.remainingSeats());
        assertEquals("Synthetic room", details.venue().orElseThrow().name());
        assertEquals("Level 2", details.venue().orElseThrow().location());
        assertEquals(Optional.of(Registration.Status.CONFIRMED), details.ownStatus());
        assertEquals(Optional.of(Registration.Status.CHECKED_IN), service().getEvent(login("bob"), event).ownStatus());
        assertEquals(Optional.of(Registration.Status.CANCELLED), service().getEvent(login("cancelled"), event).ownStatus());
        assertEquals(2, new JdbcEventRegistrations(database).registeredAttendees(event).size());
        assertEquals(AVAILABLE, details.eligibility());
        assertEquals(5, count("event_registrations"));
        assertEquals(0, count("audit_logs")); assertEquals(0, count("notification_outbox"));
    }

    @Test void reportsMissingRiskyMismatchedInactiveAndCancelledBookingWithoutHidingEvent() throws Exception {
        UUID event = event("PUBLISHED", 2, NOW.plusSeconds(3600));
        account("alice", Role.ATTENDEE); String token = login("alice");
        assertEquals(VENUE_NOT_CONFIRMED, service().getEvent(token, event).eligibility());
        assertTrue(service().getEvent(token, event).venue().isEmpty());
        booking(event);
        assertEquals(AVAILABLE, service().getEvent(token, event).eligibility());
        sql("UPDATE venues SET status='INACTIVE'");
        assertEquals(VENUE_INACTIVE, service().getEvent(token, event).eligibility());
        sql("UPDATE venues SET status='ACTIVE'");
        sql("UPDATE venue_bookings SET status='AT_RISK'");
        assertEquals("AT_RISK", service().getEvent(token, event).venue().orElseThrow().bookingStatus());
        assertEquals(VENUE_NOT_CONFIRMED, service().getEvent(token, event).eligibility());
        sql("UPDATE venue_bookings SET status='CONFIRMED',ends_at=ends_at + INTERVAL '1 minute'");
        assertEquals(VENUE_NOT_CONFIRMED, service().getEvent(token, event).eligibility());
        sql("UPDATE venue_bookings SET status='CANCELLED',cancelled_at=now(),cancelled_by='" + UUID.randomUUID()
                + "',cancellation_reason='Synthetic cancellation'");
        assertTrue(service().getEvent(token, event).venue().isEmpty());
        assertEquals(VENUE_NOT_CONFIRMED, service().getEvent(token, event).eligibility());
    }

    @Test void refreshReflectsCapacityAndVisibilityChangesWithoutMutatingRecords() throws Exception {
        UUID event = event("PUBLISHED", 2, NOW.plusSeconds(3600));
        UUID alice = account("alice", Role.ATTENDEE);
        registration(event, alice, "CONFIRMED"); booking(event);
        String token = login("alice");
        assertEquals(1, service().getEvent(token, event).remainingSeats());
        sql("UPDATE organizer_event SET capacity=1");
        assertEquals(EVENT_FULL, service().getEvent(token, event).eligibility());
        assertEquals(0, service().getEvent(token, event).remainingSeats());
        sql("UPDATE organizer_event SET status='DRAFT'");
        assertThrows(EntityNotFoundException.class, () -> service().getEvent(token, event));
        sql("UPDATE organizer_event SET status='PUBLISHED',starts_at='2030-01-01T00:00:00Z'");
        assertEquals(EVENT_NOT_REGISTERABLE, service().getEvent(token, event).eligibility());
        sql("UPDATE organizer_event SET starts_at='2029-12-31T22:00:00Z',ends_at='2030-01-01T00:00:00Z'");
        assertThrows(EntityNotFoundException.class, () -> service().getEvent(token, event));
        assertEquals(1, count("event_registrations"));
        assertEquals(0, count("audit_logs")); assertEquals(0, count("notification_outbox"));
    }

    @Test void rejectsRevokedExpiredDeactivatedAndWrongRoleSessions() throws Exception {
        UUID event = event("PUBLISHED", 2, NOW.plusSeconds(3600));
        account("alice", Role.ATTENDEE); account("organizer", Role.CLUB_ORGANIZER);
        String token = login("alice"); sessions.revoke(token);
        code("UNAUTHENTICATED", () -> service().getEvent(token, event));
        String expired = login("alice");
        sql("UPDATE user_sessions SET expires_at=now() - INTERVAL '1 minute'");
        code("UNAUTHENTICATED", () -> service().getEvent(expired, event));
        code("FORBIDDEN", () -> service().getEvent(login("organizer"), event));
        String inactive = login("alice");
        sql("UPDATE users SET active=false WHERE username='alice'");
        code("UNAUTHENTICATED", () -> service().getEvent(inactive, event));
    }

    @Test void previewDoesNotAcquireWriteLocks() throws Exception {
        UUID event = event("PUBLISHED", 2, NOW.plusSeconds(3600));
        account("alice", Role.ATTENDEE); String token = login("alice"); booking(event);
        try (var c = connection(); var statement = c.createStatement()) {
            c.setAutoCommit(false);
            statement.executeQuery("SELECT * FROM organizer_event FOR UPDATE");
            statement.executeQuery("SELECT * FROM venue_bookings FOR UPDATE");
            assertTimeoutPreemptively(Duration.ofSeconds(3), () -> assertEquals(AVAILABLE, service().getEvent(token, event).eligibility()));
            c.rollback();
        }
    }

}
