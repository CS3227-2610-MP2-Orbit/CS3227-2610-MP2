package seedu.eventmanager.ui;

import java.time.DateTimeException;
import java.time.Instant;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.event.EventDetails;

/** Converts organizer form fields into an event command without depending on JavaFX controls. */
public final class EventFormParser {
    private EventFormParser() {
    }

    public static EventDetails parse(
            String title,
            String description,
            String startsAt,
            String endsAt,
            String capacity) {
        try {
            return new EventDetails(
                    title,
                    description,
                    Instant.parse(startsAt.strip()),
                    Instant.parse(endsAt.strip()),
                    Integer.parseInt(capacity.strip()));
        } catch (NullPointerException | DateTimeException | NumberFormatException exception) {
            throw new ValidationException(
                    "Use ISO-8601 UTC times (for example 2026-10-01T10:00:00Z) and a whole-number capacity");
        }
    }
}
