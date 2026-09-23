package seedu.eventmanager.event;

import java.time.Instant;
import java.util.UUID;

/** Immutable event aggregate. */
public record Event(
        UUID id,
        String clubId,
        String organizerId,
        String title,
        String description,
        Instant startsAt,
        Instant endsAt,
        int capacity,
        EventStatus status,
        long version) {
}
