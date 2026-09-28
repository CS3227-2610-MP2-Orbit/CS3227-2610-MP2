package seedu.eventmanager.attendee;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.*;
import seedu.eventmanager.registration.Registration;

class MyRegistrationsServiceTest {
    final Instant now = Instant.parse("2030-01-01T00:00:00Z");
    final UUID owner = UUID.randomUUID();
    final UUID future = UUID.randomUUID();
    final UUID past = UUID.randomUUID();
    final UUID cancelled = UUID.randomUUID();
    final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    RegistrationEventInfoRepository.EventInfo info(String title, Instant start, String venue) {
        return new RegistrationEventInfoRepository.EventInfo(title, start, venue, start.plusSeconds(3600),
                "test-club", "Event description", "PUBLISHED");
    }

    Registration row(UUID event, Registration.Status status) {
        return new Registration(UUID.randomUUID(), event, owner, status, now, null, null, 7);
    }

    @Test void enrichesAllOwnRowsInOneBatchWithoutFilteringPastOrCancelledAndPreservesVersion() {
        var service = new MyRegistrationsService(token -> List.of(row(future, Registration.Status.CONFIRMED),
                row(past, Registration.Status.CHECKED_IN), row(cancelled, Registration.Status.CANCELLED)), ids -> {
                    assertEquals(Set.of(future, past, cancelled), ids);
                    return Map.of(future, info("Workshop", now.plusSeconds(1), "Room"),
                            past, info("Past event", now.minusSeconds(10), "Old room"),
                            cancelled, info("Cancelled booking", now.plusSeconds(60), ""));
                }, token -> new Actor(owner, Role.ATTENDEE), clock);
        var rows = service.list("synthetic-session");
        assertEquals(3, rows.size());
        assertEquals("Workshop", rows.getFirst().title());
        assertEquals("Room", rows.getFirst().venue());
        assertEquals(7, rows.getFirst().version());
        assertEquals(now.plusSeconds(3601), rows.getFirst().endsAt());
        assertEquals("test-club", rows.getFirst().clubId());
        assertEquals("Event description", rows.getFirst().description());
        assertEquals("PUBLISHED", rows.getFirst().eventStatus());
        assertTrue(rows.getFirst().canCancel());
        assertFalse(rows.get(1).canCancel());
        assertFalse(rows.get(2).canCancel());
    }

    @Test void exactStartCannotCancelAndEmptyListDoesNotQueryMetadata() {
        var service = new MyRegistrationsService(token -> List.of(row(future, Registration.Status.CONFIRMED)),
                ids -> Map.of(future, info("Starting now", now, "Room")),
                token -> new Actor(owner, Role.ATTENDEE), clock);
        assertFalse(service.list("synthetic-session").getFirst().canCancel());
        var empty = new MyRegistrationsService(token -> List.of(), ids -> {
            fail("No metadata query for an empty list"); return Map.of();
        }, token -> new Actor(owner, Role.ATTENDEE), clock);
        assertTrue(empty.list("synthetic-session").isEmpty());
    }

    @Test void rejectsMissingAndWrongRoleSessionsBeforeReading() {
        var service = new MyRegistrationsService(token -> { fail("Must not read"); return List.of(); },
                ids -> Map.of(), token -> new Actor(owner, Role.CLUB_ORGANIZER), clock);
        assertEquals("UNAUTHENTICATED", assertThrows(ApplicationException.class, () -> service.list(null)).code());
        assertEquals("FORBIDDEN", assertThrows(ApplicationException.class, () -> service.list("organizer")).code());
    }

    @Test void discardsPersonalRowsIfSessionRevokedDuringEnrichment() {
        var revoked = new AtomicBoolean();
        var service = new MyRegistrationsService(token -> List.of(row(future, Registration.Status.CONFIRMED)), ids -> {
            revoked.set(true);
            return Map.of(future, info("Private booking", now, "Room"));
        }, token -> {
            if (revoked.get()) throw new IllegalArgumentException("Expired");
            return new Actor(owner, Role.ATTENDEE);
        }, clock);
        assertEquals("UNAUTHENTICATED", assertThrows(ApplicationException.class, () -> service.list("synthetic")).code());
    }

    @Test void refusesAnotherOwnersRecordBeforeMetadataRead() {
        var service = new MyRegistrationsService(token -> List.of(row(future, Registration.Status.CONFIRMED)),
                ids -> { fail("Must not enrich another owner's rows"); return Map.of(); },
                token -> new Actor(UUID.randomUUID(), Role.ATTENDEE), clock);
        assertEquals("FORBIDDEN", assertThrows(ApplicationException.class, () -> service.list("synthetic")).code());
    }
}
