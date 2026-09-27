package seedu.eventmanager.registration;

import static org.junit.jupiter.api.Assertions.*;
import static seedu.eventmanager.registration.RegistrationServiceTest.code;

import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.*;
import seedu.eventmanager.storage.*;

class RegistrationWorkflowIntegrationTest extends RegistrationDatabaseTest {
    RegistrationService service() {
        return RegistrationServiceFactory.create(configuration, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void realLifecycleFeedsJosephAndPersistsAtomicEffectsAcrossNewServiceInstances() throws Exception {
        UUID event = eligibleEvent(2);
        UUID alice = account("alice", Role.ATTENDEE);
        account("bob", Role.ATTENDEE);
        String token = login("alice");
        Registration registered = service().register(token, event, -1);
        assertEquals(List.of(new RegisteredAttendee(alice, "alice")),
                new JdbcEventRegistrations(database).registeredAttendees(event));
        assertEquals(registered, service().register(token, event, -1));
        assertTrue(service().myRegistrations(login("bob")).isEmpty());
        code("REGISTRATION_NOT_FOUND", () -> service().cancel(login("bob"), event, 0));
        var cancelled = service().cancel(token, event, registered.version());
        assertTrue(new JdbcEventRegistrations(database).registeredAttendees(event).isEmpty());
        assertEquals(cancelled, service().cancel(token, event, registered.version()));
        var again = service().register(token, event, cancelled.version());
        code("REGISTRATION_CHANGED", () -> service().cancel(token, event, 0));
        assertEquals(registered.id(), again.id());
        assertEquals(List.of(again), service().myRegistrations(token));
        assertEquals(1, count("event_registrations"));
        assertEquals(3, count("audit_logs"));
        assertEquals(3, count("notification_outbox"));
        assertEquals(1, new JdbcEventRegistrations(database).registeredAttendees(event).size());
    }

    @Test
    void liveSessionsRejectRevokedExpiredInactiveAndWrongRoleAccounts() throws Exception {
        UUID event = eligibleEvent(10);
        account("alice", Role.ATTENDEE);
        account("organizer", Role.CLUB_ORGANIZER);
        String token = login("alice");
        sessions.revoke(token);
        code("UNAUTHENTICATED", () -> service().register(token, event, -1));
        String expired = login("alice");
        sql("UPDATE user_sessions SET expires_at=CURRENT_TIMESTAMP - INTERVAL '1 minute'");
        code("UNAUTHENTICATED", () -> service().register(expired, event, -1));
        code("FORBIDDEN", () -> service().register(login("organizer"), event, -1));
        String inactive = login("alice");
        sql("UPDATE users SET active=false WHERE username='alice'");
        code("UNAUTHENTICATED", () -> service().register(inactive, event, -1));
        assertEquals(0, count("event_registrations"));
        assertEquals(0, count("audit_logs"));
        assertEquals(0, count("notification_outbox"));
    }

    @Test
    void allThreeEligibilityStatesAndMatchingBookingTimesAreRequired() throws Exception {
        UUID event = event("PUBLISHED", 3, NOW.plusSeconds(3600));
        account("alice", Role.ATTENDEE);
        String token = login("alice");
        code("VENUE_NOT_CONFIRMED", () -> service().register(token, event, -1));
        booking(event);
        sql("UPDATE venues SET status='MAINTENANCE'");
        code("VENUE_NOT_CONFIRMED", () -> service().register(token, event, -1));
        sql("UPDATE venues SET status='ACTIVE'");
        sql("UPDATE venue_bookings SET status='AT_RISK'");
        code("VENUE_NOT_CONFIRMED", () -> service().register(token, event, -1));
        sql("UPDATE venue_bookings SET status='CONFIRMED',ends_at=ends_at + INTERVAL '1 minute'");
        code("VENUE_NOT_CONFIRMED", () -> service().register(token, event, -1));
        sql("UPDATE venue_bookings SET ends_at=ends_at - INTERVAL '1 minute'");
        sql("UPDATE organizer_event SET status='DRAFT'");
        code("EVENT_NOT_REGISTERABLE", () -> service().register(token, event, -1));
        sql("UPDATE organizer_event SET status='PUBLISHED'");
        assertNotNull(service().register(token, event, -1));
    }

    @Test
    void simultaneousLastSeatRequestsUseSeparateConnectionsWithoutOverbooking() throws Exception {
        UUID event = eligibleEvent(1);
        account("alice", Role.ATTENDEE); account("bob", Role.ATTENDEE);
        List<Object> outcomes = race(event, login("alice"), login("bob"));
        assertEquals(1, outcomes.stream().filter(Registration.class::isInstance).count());
        assertEquals(1, outcomes.stream().filter("EVENT_FULL"::equals).count());
        assertEquals(1, count("event_registrations"));
        assertEquals(1, count("audit_logs"));
        assertEquals(1, count("notification_outbox"));
    }

    @Test
    void simultaneousDuplicateRequestsAreIdempotent() throws Exception {
        UUID event = eligibleEvent(1);
        account("alice", Role.ATTENDEE);
        String token = login("alice");
        List<Object> outcomes = race(event, token, token);
        assertInstanceOf(Registration.class, outcomes.getFirst());
        assertEquals(outcomes.getFirst(), outcomes.getLast());
        assertEquals(1, count("event_registrations"));
        assertEquals(1, count("audit_logs"));
        assertEquals(1, count("notification_outbox"));
    }

    @Test
    void auditOrOutboxFailureRollsBackRegistrationAndEveryEffect() throws Exception {
        UUID event = eligibleEvent(1);
        account("alice", Role.ATTENDEE);
        String token = login("alice");
        sql("ALTER TABLE audit_logs ADD CONSTRAINT test_fail CHECK (action NOT LIKE 'REGISTRATION_%')");
        assertThrows(IllegalStateException.class, () -> service().register(token, event, -1));
        assertEquals(0, count("event_registrations"));
        assertEquals(0, count("audit_logs"));
        assertEquals(0, count("notification_outbox"));
        sql("ALTER TABLE audit_logs DROP CONSTRAINT test_fail");
        sql("ALTER TABLE notification_outbox ADD CONSTRAINT test_fail CHECK (event NOT LIKE 'REGISTRATION_%')");
        assertThrows(IllegalStateException.class, () -> service().register(token, event, -1));
        assertEquals(0, count("event_registrations"));
        assertEquals(0, count("audit_logs"));
        assertEquals(0, count("notification_outbox"));
        sql("ALTER TABLE notification_outbox DROP CONSTRAINT test_fail");
        assertNotNull(service().register(token, event, -1));
    }

    @Test
    void cancellationFailureRetainsTheSeatAndExistingEffects() throws Exception {
        UUID event = eligibleEvent(1);
        account("alice", Role.ATTENDEE);
        String token = login("alice");
        var before = service().register(token, event, -1);
        sql("ALTER TABLE notification_outbox ADD CONSTRAINT test_fail CHECK (event <> 'REGISTRATION_CANCELLED')");
        assertThrows(IllegalStateException.class, () -> service().cancel(token, event, before.version()));
        assertEquals(List.of(before), service().myRegistrations(token));
        assertEquals(1, count("audit_logs"));
        assertEquals(1, count("notification_outbox"));
        assertEquals(1, new JdbcEventRegistrations(database).registeredAttendees(event).size());
    }

    @Test
    void deactivationDoesNotSilentlyReleaseASeatAndCheckedInCannotCancel() throws Exception {
        UUID event = eligibleEvent(1);
        account("alice", Role.ATTENDEE); account("bob", Role.ATTENDEE);
        String alice = login("alice");
        String bob = login("bob");
        service().register(alice, event, -1);
        sql("UPDATE users SET active=false WHERE username='alice'");
        assertTrue(new JdbcEventRegistrations(database).registeredAttendees(event).isEmpty());
        code("EVENT_FULL", () -> service().register(bob, event, -1));
        sql("UPDATE users SET active=true WHERE username='alice'");
        sql("UPDATE event_registrations SET status='CHECKED_IN',checked_in_at=CURRENT_TIMESTAMP,version=1");
        assertEquals(1, new JdbcEventRegistrations(database).registeredAttendees(event).size());
        code("ALREADY_CHECKED_IN", () -> service().cancel(alice, event, 1));
        assertEquals(1, count("audit_logs"));
    }

    UUID eligibleEvent(int capacity) throws Exception {
        UUID event = event("PUBLISHED", capacity, NOW.plusSeconds(3600));
        booking(event);
        return event;
    }

    void booking(UUID event) throws Exception {
        UUID venue = UUID.randomUUID(), request = UUID.randomUUID();
        try (var c = connection()) {
            c.setAutoCommit(false);
            try (var p = c.prepareStatement("""
                    INSERT INTO venues (venue_id,name,location,capacity,status,created_at,updated_at)
                    VALUES (?,?,'Synthetic',100,'ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                    """)) {
                p.setObject(1, venue); p.setString(2, venue.toString()); p.executeUpdate();
            }
            try (var p = c.prepareStatement("""
                    INSERT INTO venue_requests (request_id,event_id,venue_id,organizer_id,requested_starts_at,
                      requested_ends_at,expected_attendance,status,submitted_at,decided_at,decided_by,created_at,updated_at)
                    SELECT ?,id,?,?,starts_at,ends_at,capacity,'APPROVED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?,
                      CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM organizer_event WHERE id=?
                    """)) {
                p.setObject(1, request); p.setObject(2, venue); p.setObject(3, UUID.randomUUID());
                p.setObject(4, UUID.randomUUID()); p.setObject(5, event); p.executeUpdate();
            }
            try (var p = c.prepareStatement("""
                    INSERT INTO venue_bookings (booking_id,request_id,event_id,venue_id,status,starts_at,ends_at,
                      confirmed_at,created_at,updated_at)
                    SELECT ?,?,id,?,'CONFIRMED',starts_at,ends_at,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
                    FROM organizer_event WHERE id=?
                    """)) {
                p.setObject(1, UUID.randomUUID()); p.setObject(2, request); p.setObject(3, venue);
                p.setObject(4, event); p.executeUpdate();
            }
            c.commit();
        }
    }

    long count(String table) throws Exception {
        try (var c = connection(); var s = c.createStatement(); var r = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            r.next(); return r.getLong(1);
        }
    }

    List<Object> race(UUID event, String left, String right) throws Exception {
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Object>> futures = new ArrayList<>();
            for (String token : List.of(left, right)) {
                futures.add(executor.submit(() -> {
                    // Each factory creates a separate JdbcDatabase/connection boundary.
                    var service = service();
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("Race did not start");
                    try { return service.register(token, event, -1); }
                    catch (ApplicationException rejection) { return rejection.code(); }
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            return List.of(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
        }
    }
}
