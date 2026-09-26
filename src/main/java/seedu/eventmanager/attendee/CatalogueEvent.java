package seedu.eventmanager.attendee;

import java.time.Instant;
import java.util.UUID;

/** Public event fields only. Capacity is the configured limit, not available seats. */
public record CatalogueEvent(UUID id, String clubId, String title, String description,
        Instant startsAt, Instant endsAt, int capacity) { }
