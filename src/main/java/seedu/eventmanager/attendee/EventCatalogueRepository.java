package seedu.eventmanager.attendee;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import seedu.eventmanager.event.Event;

/** Read-only projection of Organizer events; never grants organizer write access. */
public interface EventCatalogueRepository {
    List<Event> findUpcomingPublished(Instant now);

    Optional<Event> findPublishedById(UUID id);
}
