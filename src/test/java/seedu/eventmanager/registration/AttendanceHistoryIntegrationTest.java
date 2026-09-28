package seedu.eventmanager.registration;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.attendee.*;
import seedu.eventmanager.common.*;
import seedu.eventmanager.storage.JdbcAttendanceHistoryRepository;

class AttendanceHistoryIntegrationTest extends RegistrationDatabaseTest {
    AttendanceHistoryService history() {
        return new AttendanceHistoryService(new JdbcAttendanceHistoryRepository(database), sessions::resolve);
    }

    @Test void listsOnlyOwnCheckInsNewestFirstIncludingCompletedAndOngoingEvents() throws Exception {
        UUID alice = account("alice", Role.ATTENDEE), bob = account("bob", Role.ATTENDEE);
        UUID past = event("COMPLETED", 20, NOW.minusSeconds(86400));
        UUID ongoing = event("PUBLISHED", 20, NOW);
        registration(past, alice, "CHECKED_IN"); registration(ongoing, alice, "CHECKED_IN");
        registration(past, bob, "CHECKED_IN");
        registration(event("PUBLISHED", 20, NOW.plusSeconds(86400)), alice, "CONFIRMED");
        registration(event("PUBLISHED", 20, NOW.minusSeconds(172800)), alice, "CANCELLED");
        sql("UPDATE event_registrations SET checked_in_at=TIMESTAMPTZ '2030-01-01 00:00:05Z' WHERE event_id='" + ongoing + "'");
        sql("UPDATE event_registrations SET checked_in_at=TIMESTAMPTZ '2029-12-31 00:00:05Z' WHERE event_id='" + past + "'");
        var rows = history().list(login("alice"));
        assertEquals(List.of(ongoing, past), rows.stream().map(AttendanceRecord::eventId).toList());
        assertEquals(NOW.plusSeconds(5), rows.getFirst().checkedInAt());
        assertEquals("COMPLETED", rows.getLast().eventStatus());
        assertEquals("Synthetic event", rows.getLast().title());
        assertEquals("test-club", rows.getLast().clubId());
        assertEquals("", rows.getLast().venue());
        assertEquals(List.of(past), history().list(login("bob")).stream().map(AttendanceRecord::eventId).toList());
        assertEquals(0, count("audit_logs")); assertEquals(0, count("notification_outbox"));
    }

    @Test void realCheckInAppearsOnceAndVenueChangesDoNotEraseHistory() throws Exception {
        UUID alice = account("alice", Role.ATTENDEE), event = event("PUBLISHED", 20, NOW);
        booking(event, "Original room"); registration(event, alice, "CONFIRMED");
        String token = login("alice");
        assertTrue(history().list(token).isEmpty());
        RegistrationServiceFactory.create(configuration, Clock.fixed(NOW, ZoneOffset.UTC)).checkIn(token, event, 0);
        var row = history().list(token).getFirst();
        assertEquals(NOW, row.checkedInAt()); assertEquals("Original room · Level 2", row.venue());
        sql("UPDATE venue_bookings SET status='CANCELLED',cancelled_at=now(),cancelled_by='00000000-0000-0000-0000-000000000001',cancellation_reason='Synthetic change'");
        booking(event, "New room");
        sql("UPDATE venues SET status='MAINTENANCE'");
        sql("UPDATE organizer_event SET status='COMPLETED',description='Updated description'");
        var rows = history().list(token);
        assertEquals(1, rows.size()); assertEquals("New room · Level 2", rows.getFirst().venue());
        assertEquals("Updated description", rows.getFirst().description());
        sql("UPDATE venue_bookings SET status='CANCELLED',cancelled_at=now(),cancelled_by='00000000-0000-0000-0000-000000000001',cancellation_reason='Synthetic change',confirmed_at=confirmed_at+INTERVAL '1 hour' WHERE venue_id IN (SELECT venue_id FROM venues WHERE name='New room')");
        assertEquals("New room · Level 2", history().list(token).getFirst().venue());
        assertEquals(1, count("audit_logs")); assertEquals(0, count("notification_outbox"));
    }

    @Test void rejectsMissingRevokedExpiredInactiveAndWrongRoleSessions() throws Exception {
        UUID alice = account("alice", Role.ATTENDEE);
        account("organizer", Role.CLUB_ORGANIZER);
        registration(event("COMPLETED", 20, NOW), alice, "CHECKED_IN");
        assertEquals("UNAUTHENTICATED", assertThrows(ApplicationException.class, () -> history().list(null)).code());
        assertEquals("FORBIDDEN", assertThrows(ApplicationException.class, () -> history().list(login("organizer"))).code());
        String revoked = login("alice"); sessions.revoke(revoked);
        assertThrows(ApplicationException.class, () -> history().list(revoked));
        String expired = login("alice"); sql("UPDATE user_sessions SET expires_at=now()-INTERVAL '1 minute'");
        assertThrows(ApplicationException.class, () -> history().list(expired));
        String inactive = login("alice"); sql("UPDATE users SET active=false WHERE username='alice'");
        assertThrows(ApplicationException.class, () -> history().list(inactive));
        assertTrue(new JdbcAttendanceHistoryRepository(database).findByAttendee(alice).isEmpty());
        assertEquals(0, count("audit_logs")); assertEquals(0, count("notification_outbox"));
    }
}
