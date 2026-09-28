package seedu.eventmanager.attendee;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import seedu.eventmanager.event.Event;

/** Read-only projection of Organizer events; never grants organizer write access. */
public interface EventCatalogueRepository {
    record Entry(Event event, String clubName) {
        public Entry(Event event) { this(event, null); }
    }

    List<Entry> findPublishedNotEnded(Instant now);

    Optional<Entry> findPublishedById(UUID id);

    List<CatalogueClub> findClubs();
}
