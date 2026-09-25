package seedu.eventmanager.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.ValidationException;

class EventFormParserTest {
    @Test
    void parse_validFields_returnsTypedDetails() {
        var details = EventFormParser.parse(
                "Campus Night",
                "Description",
                LocalDate.of(2026, 10, 1),
                "18:00",
                LocalDate.of(2026, 10, 1),
                "20:00",
                "80");

        assertEquals(Instant.parse("2026-10-01T10:00:00Z"), details.startsAt());
        assertEquals(Instant.parse("2026-10-01T12:00:00Z"), details.endsAt());
        assertEquals(80, details.capacity());
    }

    @Test
    void parse_invalidTimeOrCapacity_reportsSafeValidationMessage() {
        assertThrows(
                ValidationException.class,
                () -> EventFormParser.parse(
                        "Title",
                        "",
                        LocalDate.of(2026, 10, 1),
                        "6 PM",
                        LocalDate.of(2026, 10, 1),
                        "later",
                        "many"));
    }

    @Test
    void parse_missingDate_reportsValidationError() {
        assertThrows(
                ValidationException.class,
                () -> EventFormParser.parse(
                        "Title", "", null, "18:00", LocalDate.of(2026, 10, 1), "20:00", "80"));
    }
}
