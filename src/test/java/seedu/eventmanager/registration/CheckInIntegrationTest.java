package seedu.eventmanager.registration;

import static org.junit.jupiter.api.Assertions.*;
import static seedu.eventmanager.registration.RegistrationServiceTest.code;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.*;
import seedu.eventmanager.storage.*;

class CheckInIntegrationTest extends RegistrationDatabaseTest {
    RegistrationService service(Instant at) {
        return RegistrationServiceFactory.create(configuration, Clock.fixed(at, ZoneOffset.UTC));
    }
    record Fixture(UUID event, UUID owner, String token) { }
    Fixture fixture() throws Exception {
        UUID owner = account("alice", Role.ATTENDEE);
        UUID event = event("PUBLISHED", 1, NOW); booking(event);
        registration(event, owner, "CONFIRMED");
        return new Fixture(event, owner, login("alice"));
    }
    @Test void exactStartPersistsOneTransitionAndJosephStillSeesAttendee() throws Exception {
        var f = fixture();
        var before = service(NOW).myRegistrations(f.token()).getFirst();
        var row = service(NOW).checkIn(f.token(), f.event(), 0);
        assertEquals(Registration.Status.CHECKED_IN, row.status());
        assertEquals(NOW, row.checkedInAt());
        assertEquals(before.registeredAt(), row.registeredAt());
        assertEquals(1, row.version());
        assertEquals(row, service(NOW).myRegistrations(f.token()).getFirst());
        assertEquals(List.of(new RegisteredAttendee(f.owner(), "alice")),
                new JdbcEventRegistrations(database).registeredAttendees(f.event()));
        code("REGISTRATION_CHANGED", () -> service(NOW).checkIn(f.token(), f.event(), 0));
        code("ALREADY_CHECKED_IN", () -> service(NOW).checkIn(f.token(), f.event(), 1));
        code("ALREADY_CHECKED_IN", () -> service(NOW).cancel(f.token(), f.event(), 1));
        assertEquals(row, service(NOW).myRegistrations(f.token()).getFirst());
        assertEquals(1, count("audit_logs")); assertEquals(0, count("notification_outbox"));
        try (var c = connection(); var s = c.createStatement();
                var r = s.executeQuery("SELECT actor_id,action,previous_state,new_state FROM audit_logs")) {
            assertTrue(r.next()); assertEquals(f.owner(), r.getObject("actor_id", UUID.class));
            assertEquals("REGISTRATION_CHECKED_IN", r.getString("action"));
            assertEquals("CONFIRMED", r.getString("previous_state"));
            assertEquals("CHECKED_IN", r.getString("new_state"));
        }
    }
    @Test void beforeStartAndExactEndAreRejectedButLastInstantBeforeEndIsAllowed() throws Exception {
        var f = fixture();
        code("CHECK_IN_TOO_EARLY", () -> service(NOW.minusNanos(1)).checkIn(f.token(), f.event(), 0));
        code("CHECK_IN_CLOSED", () -> service(NOW.plusSeconds(7200)).checkIn(f.token(), f.event(), 0));
        code("CHECK_IN_CLOSED", () -> service(NOW.plusSeconds(7201)).checkIn(f.token(), f.event(), 0));
        assertEquals(0, count("audit_logs"));
        assertNull(service(NOW).myRegistrations(f.token()).getFirst().checkedInAt());
        assertEquals(Registration.Status.CHECKED_IN,
                service(NOW.plusSeconds(7200).minusNanos(1000)).checkIn(f.token(), f.event(), 0).status());
    }
    @Test void unpublishedInactiveUnconfirmedOrMismatchedVenueCannotCheckIn() throws Exception {
        var f = fixture();
        sql("UPDATE organizer_event SET status='DRAFT'");
        code("CHECK_IN_CLOSED", () -> service(NOW).checkIn(f.token(), f.event(), 0));
        sql("UPDATE organizer_event SET status='PUBLISHED'");
        sql("UPDATE venues SET status='MAINTENANCE'");
        code("CHECK_IN_VENUE_UNAVAILABLE", () -> service(NOW).checkIn(f.token(), f.event(), 0));
        sql("UPDATE venues SET status='ACTIVE'");
        sql("UPDATE venue_bookings SET status='AT_RISK'");
        code("CHECK_IN_VENUE_UNAVAILABLE", () -> service(NOW).checkIn(f.token(), f.event(), 0));
        sql("UPDATE venue_bookings SET status='CONFIRMED',ends_at=ends_at+INTERVAL '1 minute'");
        code("CHECK_IN_VENUE_UNAVAILABLE", () -> service(NOW).checkIn(f.token(), f.event(), 0));
        sql("DELETE FROM venue_bookings");
        code("CHECK_IN_VENUE_UNAVAILABLE", () -> service(NOW).checkIn(f.token(), f.event(), 0));
        assertEquals(0, count("audit_logs")); assertEquals(0, count("notification_outbox"));
    }
    @Test void invalidSessionsOwnershipCancellationAndStaleVersionsHaveNoEffects() throws Exception {
        var f = fixture();
        account("bob", Role.ATTENDEE); account("organizer", Role.CLUB_ORGANIZER);
        code("UNAUTHENTICATED", () -> service(NOW).checkIn(null, f.event(), 0));
        code("FORBIDDEN", () -> service(NOW).checkIn(login("organizer"), f.event(), 0));
        code("REGISTRATION_NOT_FOUND", () -> service(NOW).checkIn(login("bob"), f.event(), -1));
        code("REGISTRATION_CHANGED", () -> service(NOW).checkIn(f.token(), f.event(), 5));
        code("INVALID_VERSION", () -> service(NOW).checkIn(f.token(), f.event(), -2));
        code("EVENT_NOT_FOUND", () -> service(NOW).checkIn(f.token(), UUID.randomUUID(), 0));
        sql("UPDATE event_registrations SET status='CANCELLED',cancelled_at=now()");
        code("REGISTRATION_CANCELLED", () -> service(NOW).checkIn(f.token(), f.event(), 0));
        sessions.revoke(f.token());
        code("UNAUTHENTICATED", () -> service(NOW).checkIn(f.token(), f.event(), 0));
        String expired = login("alice");
        sql("UPDATE user_sessions SET expires_at=now()-INTERVAL '1 minute'");
        code("UNAUTHENTICATED", () -> service(NOW).checkIn(expired, f.event(), 0));
        String inactive = login("alice");
        sql("UPDATE users SET active=false WHERE username='alice'");
        code("UNAUTHENTICATED", () -> service(NOW).checkIn(inactive, f.event(), 0));
        assertEquals(0, count("audit_logs")); assertEquals(0, count("notification_outbox"));
    }
    @Test void concurrentCheckInsUseSeparateConnectionsAndOnlyOneWins() throws Exception {
        var f = fixture();
        var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Object>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) futures.add(executor.submit(() -> {
                var service = service(NOW); ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                try { return service.checkIn(f.token(), f.event(), 0); }
                catch (ApplicationException rejection) { return rejection.code(); }
            }));
            assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown();
            var results = List.of(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(Registration.class::isInstance).count());
            assertEquals(1, results.stream().filter("REGISTRATION_CHANGED"::equals).count());
        }
        assertEquals(1, count("audit_logs")); assertEquals(0, count("notification_outbox"));
        var row = service(NOW).myRegistrations(f.token()).getFirst();
        assertEquals(1, row.version()); assertEquals(NOW, row.checkedInAt());
    }
    @Test void auditFailureRollsBackTimestampStatusAndVersion() throws Exception {
        var f = fixture();
        var before = service(NOW).myRegistrations(f.token()).getFirst();
        sql("ALTER TABLE audit_logs ADD CONSTRAINT fail_check_in CHECK (action <> 'REGISTRATION_CHECKED_IN')");
        assertThrows(IllegalStateException.class, () -> service(NOW).checkIn(f.token(), f.event(), 0));
        assertEquals(before, service(NOW).myRegistrations(f.token()).getFirst());
        assertEquals(0, count("audit_logs")); assertEquals(0, count("notification_outbox"));
    }
}
