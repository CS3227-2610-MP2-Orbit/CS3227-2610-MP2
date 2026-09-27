package seedu.eventmanager.registration;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.attendee.*;
import seedu.eventmanager.common.*;
import seedu.eventmanager.notification.*;
import seedu.eventmanager.storage.*;

/** Real PostgreSQL, real sessions, real outbox worker; isolated schema per test. */
class InboxIntegrationTest extends RegistrationDatabaseTest {
    JdbcInboxRepository inbox;
    @BeforeEach void initializeInbox() {
        InboxDatabaseMigration.migrate(configuration);
        inbox = new JdbcInboxRepository(database);
    }

    InboxService service() {
        return new InboxService(inbox, sessions::resolve, new JdbcTransactionManager(database),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test void registrationDeliveryIsDurableIdempotentAndPreservesReadStateOnRetry() throws Exception {
        UUID alice = account("alice", Role.ATTENDEE);
        String token = login("alice");
        UUID event = event("PUBLISHED", 5, NOW.plusSeconds(3600)); booking(event);
        RegistrationServiceFactory.create(configuration, Clock.fixed(NOW, ZoneOffset.UTC)).register(token, event, -1);
        var outbox = new JdbcNotificationOutboxRepository(database);
        var claimed = outbox.claimNext();
        var delivery = new InboxNotificationDelivery(inbox::deliver);
        delivery.deliver(claimed);
        var messages = service().list(token);
        assertEquals(1, messages.unreadCount());
        assertTrue(messages.messages().getFirst().body().contains("1 Jan 2030, 9:00 AM SGT"));
        service().markRead(token, claimed.notificationId());
        delivery.deliver(claimed); // Simulate successful inbox write followed by lost markSent response.
        assertEquals(1, count("attendee_notification_inbox"));
        var reopened = new InboxService(new JdbcInboxRepository(new JdbcDatabase(configuration)), sessions::resolve,
                new JdbcTransactionManager(database), Clock.systemUTC()).list(token);
        assertEquals(0, reopened.unreadCount());
        assertEquals(NOW, reopened.messages().getFirst().readAt());
        assertEquals(alice, claimed.recipientId());
    }

    NotificationOutboxWorker worker() {
        return new NotificationOutboxWorker(new JdbcNotificationOutboxRepository(database, InboxNotificationDelivery.EVENT_TYPES),
                new InboxNotificationDelivery(inbox::deliver));
    }

    UUID announcement(UUID event, String message) throws Exception {
        UUID id = UUID.randomUUID();
        try (var c = connection(); var p = c.prepareStatement("""
                INSERT INTO event_announcement(id,event_id,author_id,message,created_at) VALUES (?,?,'synthetic',?,now())
                """)) {
            p.setObject(1, id); p.setObject(2, event); p.setString(3, message); p.executeUpdate();
        }
        return id;
    }

    void announce(UUID owner, UUID event, UUID announcement) {
        new JdbcNotificationService(database).notify(owner, "EVENT_ANNOUNCEMENT",
                Map.of("eventId", event.toString(), "announcementId", announcement.toString()));
    }

    @Test void resolvesCurrentTitleAndAnnouncementTextAndHandlesDeletionBeforeOrAfterDelivery() throws Exception {
        UUID alice = account("alice", Role.ATTENDEE);
        UUID event = event("PUBLISHED", 3, NOW.plusSeconds(3600));
        UUID first = announcement(event, "Bring your laptop");
        announce(alice, event, first);
        assertTrue(worker().processOnce());
        String token = login("alice");
        assertEquals("Bring your laptop", service().list(token).messages().getFirst().body());
        sql("UPDATE organizer_event SET title='Updated title'");
        assertEquals("Updated title", service().list(token).messages().getFirst().title());
        sql("DELETE FROM event_announcement");
        assertEquals(1, count("notification_outbox"), "Deleting the source must not withdraw its notification");
        assertEquals("Announcement removed.", service().list(token).messages().getFirst().body());
        UUID removed = announcement(event, "Will be removed before delivery");
        announce(alice, event, removed);
        sql("DELETE FROM event_announcement");
        assertEquals(2, count("notification_outbox"), "Queued notification remains deliverable after deletion");
        assertTrue(worker().processOnce());
        var rows = service().list(token).messages();
        assertEquals(2, rows.size());
        assertEquals("Announcement removed.", rows.getFirst().body());
        assertFalse(rows.getFirst().createdAt().isBefore(rows.getLast().createdAt()));
        InboxDatabaseMigration.migrate(configuration); // Applied stream can be re-run without changing data.
        assertEquals(2, count("attendee_notification_inbox"));
    }

    @Test void ownerOnlyReadAndMarkCommandsRejectOtherAccountsAndInvalidSessions() throws Exception {
        UUID alice = account("alice", Role.ATTENDEE), bob = account("bob", Role.ATTENDEE);
        account("organizer", Role.CLUB_ORGANIZER);
        UUID event = event("PUBLISHED", 3, NOW.plusSeconds(3600));
        UUID announcement = announcement(event, "Hello registrants");
        announce(alice, event, announcement); announce(bob, event, announcement);
        assertTrue(worker().processOnce()); assertTrue(worker().processOnce());
        String a = login("alice"), b = login("bob");
        var bobMessage = service().list(b).messages().getFirst();
        assertEquals(1, service().list(a).messages().size());
        var failure = assertThrows(ApplicationException.class, () -> service().markRead(a, bobMessage.id()));
        assertEquals("NOTIFICATION_NOT_FOUND", failure.code());
        service().markAllRead(a);
        service().markAllRead(a);
        assertEquals(0, service().list(a).unreadCount());
        assertEquals(1, service().list(b).unreadCount());
        assertThrows(ApplicationException.class, () -> service().list(null));
        assertThrows(ApplicationException.class, () -> service().markAllRead(login("organizer")));
        sessions.revoke(a);
        assertThrows(ApplicationException.class, () -> service().markAllRead(a));
        sql("UPDATE user_sessions SET expires_at=now()-INTERVAL '1 minute'");
        assertThrows(ApplicationException.class, () -> service().list(b));
        String fresh = login("bob");
        sql("UPDATE users SET active=false WHERE username='bob'");
        assertThrows(ApplicationException.class, () -> service().markRead(fresh, bobMessage.id()));
        assertEquals(1, inbox.list(bob).stream().filter(row -> row.readAt() == null).count());
    }

    @Test void repeatedConcurrentDeliveryCreatesOnlyOneEntry() throws Exception {
        UUID alice = account("alice", Role.ATTENDEE);
        UUID event = event("PUBLISHED", 3, NOW.plusSeconds(3600));
        announce(alice, event, announcement(event, "Concurrent delivery"));
        var claimed = new JdbcNotificationOutboxRepository(database).claimNext();
        var delivery = new InboxNotificationDelivery(inbox::deliver);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> delivery.deliver(claimed));
            var second = executor.submit(() -> delivery.deliver(claimed));
            first.get(5, java.util.concurrent.TimeUnit.SECONDS);
            second.get(5, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertEquals(1, count("attendee_notification_inbox"));
    }

    @Test void malformedPayloadFailsAfterFiveAttemptsAndScopedWorkerLeavesVenueEventsAlone() throws Exception {
        UUID alice = account("alice", Role.ATTENDEE);
        var producer = new JdbcNotificationService(database);
        producer.notify(alice, "VENUE_REQUEST_APPROVED", Map.of());
        producer.notify(alice, "REGISTRATION_CONFIRMED", Map.of("registrationId", "not-a-uuid"));
        for (int attempt = 0; attempt < 5; attempt++) {
            assertTrue(worker().processOnce());
            sql("UPDATE notification_outbox SET next_attempt_at=now() WHERE event='REGISTRATION_CONFIRMED'");
        }
        assertFalse(worker().processOnce());
        assertEquals(0, count("attendee_notification_inbox"));
        try (var c = connection(); var s = c.createStatement();
                var rows = s.executeQuery("SELECT event,status,attempts,last_error FROM notification_outbox")) {
            while (rows.next()) {
                if (rows.getString("event").equals("REGISTRATION_CONFIRMED")) {
                    assertEquals("FAILED", rows.getString("status")); assertEquals(5, rows.getInt("attempts"));
                    assertEquals("Notification recipient or payload is invalid.", rows.getString("last_error"));
                } else {
                    assertEquals("PENDING", rows.getString("status")); assertEquals(0, rows.getInt("attempts"));
                }
            }
        }
        // If an unsupported event is explicitly routed to this adapter, it fails instead of being marked sent.
        var allTypes = new NotificationOutboxWorker(new JdbcNotificationOutboxRepository(database),
                new InboxNotificationDelivery(inbox::deliver));
        assertTrue(allTypes.processOnce());
        assertEquals(0, count("attendee_notification_inbox"));
    }

    @Test void lateSessionRevocationRollsBackReadMutationAndDiscardsList() throws Exception {
        UUID alice = account("alice", Role.ATTENDEE);
        UUID event = event("PUBLISHED", 3, NOW.plusSeconds(3600));
        announce(alice, event, announcement(event, "Private entry")); worker().processOnce();
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var revokedMidCall = new InboxService(inbox, token -> {
            if (calls.incrementAndGet() > 1) throw new IllegalArgumentException("Revoked");
            return new Actor(alice, Role.ATTENDEE);
        }, new JdbcTransactionManager(database), Clock.fixed(NOW, ZoneOffset.UTC));
        assertThrows(ApplicationException.class, () -> revokedMidCall.markAllRead("synthetic"));
        assertEquals(1, service().list(login("alice")).unreadCount());
        calls.set(0);
        assertThrows(ApplicationException.class, () -> revokedMidCall.list("synthetic"));
    }

    @Test void registrationUpdatesKeepHistoricalKindAndReadCurrentEventDetails() throws Exception {
        account("alice", Role.ATTENDEE);
        String token = login("alice");
        UUID event = event("PUBLISHED", 4, NOW.plusSeconds(3600)); booking(event);
        var commands = RegistrationServiceFactory.create(configuration, Clock.fixed(NOW, ZoneOffset.UTC));
        commands.register(token, event, -1); commands.cancel(token, event, 0);
        assertTrue(worker().processOnce()); assertTrue(worker().processOnce());
        sql("UPDATE organizer_event SET title='Updated workshop',starts_at=starts_at+INTERVAL '1 minute'");
        var rows = service().list(token).messages();
        assertEquals(2, rows.size());
        assertTrue(rows.stream().anyMatch(row -> row.body().startsWith("Registration confirmed:")));
        assertTrue(rows.stream().anyMatch(row -> row.body().startsWith("Registration cancelled:")));
        assertTrue(rows.stream().allMatch(row -> row.body().contains("Updated workshop") && row.body().contains("9:01 AM SGT")));
        assertEquals(2, count("audit_logs"));
    }

    @Test void rejectsRegistrationPayloadForAnotherOwnerButRetainsDeliveryForInactiveAttendees() throws Exception {
        UUID alice = account("alice", Role.ATTENDEE), bob = account("bob", Role.ATTENDEE);
        String token = login("alice");
        UUID event = event("PUBLISHED", 4, NOW.plusSeconds(3600)); booking(event);
        var registration = RegistrationServiceFactory.create(configuration, Clock.fixed(NOW, ZoneOffset.UTC))
                .register(token, event, -1);
        assertTrue(worker().processOnce());
        new JdbcNotificationService(database).notify(bob, "REGISTRATION_CONFIRMED",
                Map.of("registrationId", registration.id().toString(), "version", "0"));
        assertTrue(worker().processOnce());
        assertTrue(inbox.list(bob).isEmpty());
        assertEquals(1, inbox.list(alice).size());

        sql("UPDATE users SET active=false WHERE username='bob'");
        announce(bob, event, announcement(event, "Retained until the account can log in again"));
        assertTrue(worker().processOnce());
        assertEquals(1, inbox.list(bob).size());
    }
}
