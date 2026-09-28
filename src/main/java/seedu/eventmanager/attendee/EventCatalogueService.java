package seedu.eventmanager.attendee;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import seedu.eventmanager.common.EntityNotFoundException;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.event.Event;
import seedu.eventmanager.event.EventStatus;

/** Public, read-only catalogue; all state-changing attendee workflows are separate. */
public final class EventCatalogueService {
    private static final ZoneId CATALOGUE_ZONE = ZoneId.of("Asia/Singapore");
    private final EventCatalogueRepository repository;
    private final Clock clock;

    public EventCatalogueService(EventCatalogueRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    public List<CatalogueEvent> search(CatalogueQuery query) {
        Objects.requireNonNull(query, "query");
        if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
            throw new ValidationException("From date must be on or before To date.");
        }
        String text = normalize(query.text()).toLowerCase(Locale.ROOT);
        String club = normalize(query.clubId());
        Instant now = clock.instant();
        return repository.findPublishedNotEnded(now).stream()
                .filter(entry -> visible(entry.event(), now))
                .filter(entry -> club.isEmpty() || club.equals(entry.event().clubId()))
                .filter(entry -> entry.event().title().toLowerCase(Locale.ROOT).contains(text)
                        || entry.event().description().toLowerCase(Locale.ROOT).contains(text))
                .filter(event -> query.from() == null
                        || !event.event().startsAt().atZone(CATALOGUE_ZONE).toLocalDate().isBefore(query.from()))
                .filter(event -> query.to() == null
                        || !event.event().startsAt().atZone(CATALOGUE_ZONE).toLocalDate().isAfter(query.to()))
                .sorted(Comparator.comparing((EventCatalogueRepository.Entry entry) -> entry.event().startsAt())
                        .thenComparing(entry -> entry.event().id().toString()))
                .map(EventCatalogueService::publicView)
                .toList();
    }

    public CatalogueEvent getEvent(UUID id) {
        Objects.requireNonNull(id, "id");
        var event = repository.findPublishedById(id)
                .filter(value -> visible(value.event(), clock.instant()))
                .orElseThrow(() -> new EntityNotFoundException("Event is not available in the public catalogue."));
        return publicView(event);
    }

    private static boolean visible(Event event, Instant now) {
        return event.status() == EventStatus.PUBLISHED && event.endsAt().isAfter(now);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    public List<CatalogueClub> clubs() {
        return repository.findClubs().stream()
                .sorted(CatalogueClub.BY_NAME)
                .toList();
    }

    private static CatalogueEvent publicView(EventCatalogueRepository.Entry entry) {
        Event event = entry.event();
        return new CatalogueEvent(event.id(), event.clubId(), event.title(), event.description(),
                event.startsAt(), event.endsAt(), event.capacity(), entry.clubName());
    }
}
