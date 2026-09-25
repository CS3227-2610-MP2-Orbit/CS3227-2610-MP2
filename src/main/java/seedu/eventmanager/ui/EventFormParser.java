package seedu.eventmanager.ui;

import java.time.DateTimeException;
import java.time.LocalDate;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.event.EventDetails;

/** Converts organizer form fields into an event command without depending on JavaFX controls. */
public final class EventFormParser {
    private EventFormParser() {
    }

    public static EventDetails parse(
            String title,
            String description,
            LocalDate startDate,
            String startTime,
            LocalDate endDate,
            String endTime,
            String capacity) {
        try {
            return new EventDetails(
                    title,
                    description,
                    SingaporeDateTimes.toInstant(startDate, startTime),
                    SingaporeDateTimes.toInstant(endDate, endTime),
                    Integer.parseInt(capacity.strip()));
        } catch (NullPointerException | DateTimeException | NumberFormatException exception) {
            throw new ValidationException(
                    "Choose both dates, enter times as HH:mm (for example 18:00), "
                            + "and use a whole-number capacity");
        }
    }
}
