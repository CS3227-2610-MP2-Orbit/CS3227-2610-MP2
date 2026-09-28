package seedu.eventmanager.attendee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.EntityNotFoundException;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.event.Event;
import seedu.eventmanager.event.EventStatus;

class EventCatalogueServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-25T00:00:00Z");
    private static final Instant TOMORROW = Instant.parse("2026-09-26T10:00:00Z");

    @Test
    void listsOnlyUpcomingPublishedEventsAndSortsByStartThenId() {
        Event early = event(1, "Talk", "", "tech", TOMORROW, EventStatus.PUBLISHED);
        Event tied = event(2, "Games", "", "games", TOMORROW, EventStatus.PUBLISHED);
        Event later = event(3, "Music", "", "music", TOMORROW.plusSeconds(3600), EventStatus.PUBLISHED);
        var service = service(List.of(later, tied,
                event(4, "Draft", "", "tech", TOMORROW, EventStatus.DRAFT),
                event(5, "Done", "", "tech", TOMORROW, EventStatus.COMPLETED),
                event(6, "Started", "", "tech", NOW, EventStatus.PUBLISHED),
                event(7, "Past", "", "tech", NOW.minusSeconds(1), EventStatus.PUBLISHED), early));

        assertEquals(List.of(early.id(), tied.id(), later.id()),
                service.search(CatalogueQuery.all()).stream().map(CatalogueEvent::id).toList());
    }

    @Test
    void combinesLiteralCaseInsensitiveSearchClubAndInclusiveSingaporeDates() {
        Event wanted = event(1, "Learning", "An AI workshop", "tech",
                Instant.parse("2026-09-25T16:00:00Z"), EventStatus.PUBLISHED);
        var service = service(List.of(wanted,
                event(2, "AI", "", "other", wanted.startsAt(), EventStatus.PUBLISHED),
                event(3, "AI", "", "tech", wanted.startsAt().minusSeconds(1), EventStatus.PUBLISHED),
                event(4, "AI", "", "tech", wanted.startsAt().plusSeconds(86400), EventStatus.PUBLISHED)));
        LocalDate day = LocalDate.of(2026, 9, 26);

        assertEquals(List.of(wanted.id()), service.search(new CatalogueQuery(" ai ", " tech ", day, day))
                .stream().map(CatalogueEvent::id).toList());
        assertTrue(service.search(new CatalogueQuery("%", "", null, null)).isEmpty());
    }

    @Test
    void blankFiltersAndOpenDateBoundsAreSupported() {
        var service = service(List.of(event(1, "Open", "", "tech", TOMORROW, EventStatus.PUBLISHED)));
        assertEquals(1, service.search(new CatalogueQuery("  ", null, null, null)).size());
        assertEquals(1, service.search(new CatalogueQuery(null, "", LocalDate.of(2026, 9, 26), null)).size());
        assertEquals(1, service.search(new CatalogueQuery(null, "", null, LocalDate.of(2026, 9, 26))).size());
    }

    @Test
    void reversedDateRangeIsRejectedRatherThanSilentlyReturningEmpty() {
        assertThrows(ValidationException.class, () -> service(List.of()).search(
                new CatalogueQuery("", "", LocalDate.of(2026, 9, 27), LocalDate.of(2026, 9, 26))));
    }

    @Test
    void detailsReturnPublicFieldsWithoutOrganizerIdentity() {
        Event event = event(1, "Talk", "Description", "tech", TOMORROW, EventStatus.PUBLISHED);
        assertEquals(new CatalogueEvent(event.id(), "tech", "Talk", "Description",
                TOMORROW, event.endsAt(), 20), service(List.of(event)).getEvent(event.id()));
    }

    @Test
    void directIdLookupCannotExposeDraftCompletedStartedOrMissingEvents() {
        for (Event event : List.of(
                event(1, "Draft", "", "tech", TOMORROW, EventStatus.DRAFT),
                event(2, "Done", "", "tech", TOMORROW, EventStatus.COMPLETED),
                event(3, "Started", "", "tech", NOW, EventStatus.PUBLISHED))) {
            assertThrows(EntityNotFoundException.class, () -> service(List.of(event)).getEvent(event.id()));
        }
        assertThrows(EntityNotFoundException.class, () -> service(List.of()).getEvent(UUID.randomUUID()));
    }

    private static EventCatalogueService service(List<Event> events) {
        // Deliberately includes hidden events: service must enforce its own public boundary.
        return new EventCatalogueService(new EventCatalogueRepository() {
            public List<Event> findUpcomingPublished(Instant now) { return events; }
            public Optional<Event> findPublishedById(UUID id) {
                return events.stream().filter(event -> event.id().equals(id)).findFirst();
            }
        }, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Event event(int id, String title, String description, String club,
            Instant start, EventStatus status) {
        return new Event(new UUID(0, id), club, "private-organizer", title, description,
                start, start.plusSeconds(3600), 20, status, 0);
    }
}
