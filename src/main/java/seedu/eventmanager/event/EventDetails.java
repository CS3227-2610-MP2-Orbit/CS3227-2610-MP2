package seedu.eventmanager.event;

import java.time.Instant;

/** Organizer-editable details for an event. */
public record EventDetails(
        String title,
        String description,
        Instant startsAt,
        Instant endsAt,
        int capacity) {
}
