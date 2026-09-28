package seedu.eventmanager.attendee;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.*;
import seedu.eventmanager.service.TransactionManager;

class InboxServiceTest {
    final UUID owner = UUID.randomUUID();
    final Instant now = Instant.parse("2030-01-01T00:00:00Z");
    final TransactionManager transactions = new TransactionManager() {
        public <T> T execute(Supplier<T> work) { return work.get(); }
    };
    int reads;
    int writes;
    final InboxRepository repository = new InboxRepository() {
        public List<Entry> list(UUID user) {
            assertEquals(owner, user); reads++;
            return List.of(new Entry(UUID.randomUUID(), now, null, "REGISTRATION_CONFIRMED",
                    "Workshop", now, null));
        }
        public boolean markRead(UUID user, UUID id, Instant at) {
            assertEquals(owner, user); writes++; return false;
        }
        public void markAllRead(UUID user, Instant at) { assertEquals(owner, user); writes++; }
    };

    InboxService service() {
        return new InboxService(repository, token -> switch (token) {
            case "attendee" -> new Actor(owner, Role.ATTENDEE);
            case "organizer" -> new Actor(owner, Role.CLUB_ORGANIZER);
            default -> throw new IllegalArgumentException("Expired");
        }, transactions, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test void deniesMissingExpiredAndWrongRoleBeforeAnyReadOrWrite() {
        for (String token : Arrays.asList(null, "", "expired", "organizer")) {
            assertThrows(ApplicationException.class, () -> service().list(token));
            assertThrows(ApplicationException.class, () -> service().markRead(token, UUID.randomUUID()));
            assertThrows(ApplicationException.class, () -> service().markAllRead(token));
        }
        assertEquals(0, reads); assertEquals(0, writes);
    }

    @Test void derivesOwnerAndReturnsConsistentUnreadCountAndSgtText() {
        var snapshot = service().list("attendee");
        assertEquals(1, snapshot.unreadCount());
        assertTrue(snapshot.messages().getFirst().body().contains("1 Jan 2030, 8:00 AM SGT"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.messages().clear());
        service().markAllRead("attendee");
        assertEquals(1, writes);
    }

    @Test void nullOrMissingIdUsesSameNonDisclosingError() {
        assertEquals("NOTIFICATION_NOT_FOUND", assertThrows(ApplicationException.class,
                () -> service().markRead("attendee", null)).code());
        assertEquals(0, writes);
        assertEquals("NOTIFICATION_NOT_FOUND", assertThrows(ApplicationException.class,
                () -> service().markRead("attendee", UUID.randomUUID())).code());
    }
}
