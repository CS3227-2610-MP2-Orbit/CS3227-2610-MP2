package seedu.eventmanager.registration;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.attendee.*;
import seedu.eventmanager.common.*;
import seedu.eventmanager.storage.*;

/** Real owner-only service + PostgreSQL projection; inherits isolated-schema fixtures only. */
class MyRegistrationsIntegrationTest extends RegistrationDatabaseTest {
    AttendeeEventDetailsService service() {
        return new AttendeeEventDetailsService(new JdbcAttendeeEventDetailsRepository(database),
                sessions::resolve, Clock.fixed(NOW, ZoneOffset.UTC));
    }
    MyRegistrationsService mine() {
        var commands = RegistrationServiceFactory.create(configuration, Clock.fixed(NOW, ZoneOffset.UTC));
        return new MyRegistrationsService(commands::myRegistrations,
                new JdbcRegistrationEventInfoRepository(database), sessions::resolve, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test void ongoingDetailsAndMyRegistrationsShareCheckInEligibilityButRegistrationStaysClosed() throws Exception {
        UUID alice = account("alice", Role.ATTENDEE);
        UUID event = event("PUBLISHED", 1, NOW);
        registration(event, alice, "CONFIRMED"); booking(event);
        String token = login("alice");
        assertTrue(service().getEvent(token, event).canCheckIn());
        assertTrue(mine().list(token).getFirst().canCheckIn());
        assertFalse(mine().list(token).getFirst().canCancel());
        assertEquals(RegistrationEligibilityPolicy.Result.EVENT_NOT_REGISTERABLE,
                service().getEvent(token, event).eligibility());
        var commands = RegistrationServiceFactory.create(configuration, Clock.fixed(NOW, ZoneOffset.UTC));
        account("bob", Role.ATTENDEE);
        RegistrationServiceTest.code("EVENT_NOT_REGISTERABLE", () -> commands.register(login("bob"), event, -1));
        sql("UPDATE venues SET status='MAINTENANCE'");
        assertFalse(service().getEvent(token, event).canCheckIn());
        assertFalse(mine().list(token).getFirst().canCheckIn());
        sql("UPDATE venues SET status='ACTIVE'");
        sql("UPDATE venue_bookings SET ends_at=ends_at+INTERVAL '1 minute'");
        assertFalse(service().getEvent(token, event).canCheckIn());
        assertFalse(mine().list(token).getFirst().canCheckIn());
        sql("UPDATE venue_bookings SET ends_at=ends_at-INTERVAL '1 minute'");
        commands.checkIn(token, event, 0);
        assertFalse(service().getEvent(token, event).canCheckIn());
        assertFalse(mine().list(token).getFirst().canCheckIn());
        assertEquals(1, count("audit_logs")); assertEquals(0, count("notification_outbox"));
    }

    @Test void listsOnlyOwnerIncludingPastUnpublishedCancelledAndCompletedVenue() throws Exception {
        UUID alice = account("alice", Role.ATTENDEE);
        UUID bob = account("bob", Role.ATTENDEE);
        UUID upcoming = event("PUBLISHED", 5, NOW.plusSeconds(3600));
        UUID past = event("COMPLETED", 5, NOW.minusSeconds(7200));
        UUID cancelled = event("DRAFT", 5, NOW.plusSeconds(7200));
        UUID other = event("PUBLISHED", 5, NOW.plusSeconds(9000));
        registration(upcoming, alice, "CONFIRMED");
        registration(past, alice, "CHECKED_IN"); booking(past);
        registration(cancelled, alice, "CANCELLED");
        registration(other, bob, "CONFIRMED");
        sql("UPDATE venue_bookings SET status='COMPLETED' WHERE event_id='" + past + "'");
        sql("UPDATE event_registrations SET version=4 WHERE attendee_id='" + alice + "'");
        sql("UPDATE organizer_event SET description='Past event full description' WHERE id='" + past + "'");
        var rows = mine().list(login("alice"));
        assertEquals(3, rows.size());
        assertEquals(Set.of(upcoming, past, cancelled), rows.stream().map(MyRegistration::eventId).collect(java.util.stream.Collectors.toSet()));
        var pastRow = rows.stream().filter(r -> r.eventId().equals(past)).findFirst().orElseThrow();
        assertEquals("Synthetic room · Level 2", pastRow.venue());
        assertFalse(pastRow.canCancel());
        assertEquals(4, pastRow.version());
        assertEquals(NOW, pastRow.endsAt());
        assertEquals("test-club", pastRow.clubId());
        assertEquals("Past event full description", pastRow.description());
        assertEquals("COMPLETED", pastRow.eventStatus());
        assertTrue(rows.stream().filter(r -> r.eventId().equals(upcoming)).findFirst().orElseThrow().canCancel());
        assertEquals("", rows.stream().filter(r -> r.eventId().equals(cancelled)).findFirst().orElseThrow().venue());
        assertEquals(1, mine().list(login("bob")).size());
        assertEquals(0, count("audit_logs")); assertEquals(0, count("notification_outbox"));
    }

    @Test void realRegisterCancelAndReregisterRefreshProjectionAndKeepOneRecord() throws Exception {
        account("alice", Role.ATTENDEE);
        String token = login("alice");
        UUID event = event("PUBLISHED", 5, NOW.plusSeconds(3600)); booking(event);
        var commands = RegistrationServiceFactory.create(configuration, Clock.fixed(NOW, ZoneOffset.UTC));
        commands.register(token, event, service().getEvent(token, event).ownRegistrationVersion());
        var displayed = mine().list(token).getFirst();
        assertEquals(Registration.Status.CONFIRMED, displayed.status());
        commands.cancel(token, displayed.eventId(), displayed.version());
        assertEquals(Registration.Status.CANCELLED, mine().list(token).getFirst().status());
        commands.register(token, event, service().getEvent(token, event).ownRegistrationVersion());
        assertEquals(2, mine().list(token).getFirst().version());
        assertEquals(1, count("event_registrations"));
        assertEquals(3, count("audit_logs")); assertEquals(3, count("notification_outbox"));
    }

    @Test void prefersCurrentVenueOverHistoricBookingsWithoutDuplicatingRegistration() throws Exception {
        UUID alice = account("alice", Role.ATTENDEE);
        UUID event = event("PUBLISHED", 5, NOW.plusSeconds(3600));
        registration(event, alice, "CONFIRMED"); booking(event, "Old room");
        sql("UPDATE venue_bookings SET status='CANCELLED',cancelled_at=now(),cancelled_by='" + UUID.randomUUID()
                + "',cancellation_reason='Synthetic cancellation',confirmed_at=now()+INTERVAL '1 day'");
        booking(event, "Current room");
        var rows = mine().list(login("alice"));
        assertEquals(1, rows.size());
        assertEquals("Current room · Level 2", rows.getFirst().venue());
    }
}
