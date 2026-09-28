package seedu.eventmanager.attendee;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.*;

class AttendanceHistoryServiceTest {
    private final Actor alice = new Actor(UUID.randomUUID(), Role.ATTENDEE);
    private final Actor bob = new Actor(UUID.randomUUID(), Role.ATTENDEE);
    private final AttendanceRecord row = new AttendanceRecord(UUID.randomUUID(), "Workshop", "Description", "club",
            Instant.EPOCH, Instant.EPOCH.plusSeconds(3600), "COMPLETED", "Room", Instant.EPOCH.plusSeconds(10));

    @Test void resolvesOwnerAndReturnsImmutableHistoryIncludingEndedEvents() {
        var service = new AttendanceHistoryService(id -> {
            assertEquals(alice.userId(), id);
            return new ArrayList<>(List.of(row));
        }, token -> alice);
        var result = service.list("synthetic-session");
        assertEquals(List.of(row), result);
        assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }

    @Test void emptyHistoryIsNotAnError() {
        assertEquals(List.of(), new AttendanceHistoryService(id -> List.of(), token -> alice).list("session"));
    }

    @Test void rejectsInvalidAndWrongRoleSessionsBeforeReading() {
        var service = new AttendanceHistoryService(id -> { fail("Unauthorized repository read"); return List.of(); },
                token -> "organizer".equals(token) ? new Actor(bob.userId(), Role.CLUB_ORGANIZER) : null);
        for (String token : Arrays.asList(null, "", "expired", "organizer")) {
            assertEquals("organizer".equals(token) ? "FORBIDDEN" : "UNAUTHENTICATED",
                    assertThrows(ApplicationException.class, () -> service.list(token)).code());
        }
    }

    @Test void rejectsRevocationAndAccountSwitchDuringRead() {
        for (Actor after : Arrays.asList(null, bob)) {
            var read = new AtomicBoolean();
            var service = new AttendanceHistoryService(id -> { read.set(true); return List.of(row); },
                    token -> read.get() ? after : alice);
            assertEquals("UNAUTHENTICATED", assertThrows(ApplicationException.class, () -> service.list("session")).code());
        }
    }

    @Test void readFailureIsNotReportedAsAnEmptyHistory() {
        var service = new AttendanceHistoryService(id -> { throw new IllegalStateException("Unavailable"); }, token -> alice);
        assertThrows(IllegalStateException.class, () -> service.list("session"));
    }
}
