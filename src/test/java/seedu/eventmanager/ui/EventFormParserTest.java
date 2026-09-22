package seedu.eventmanager.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.ValidationException;

class EventFormParserTest {
    @Test
    void parse_validFields_returnsTypedDetails() {
        var details = EventFormParser.parse(
                "Campus Night",
                "Description",
                "2026-10-01T10:00:00Z",
                "2026-10-01T12:00:00Z",
                "80");

        assertEquals(Instant.parse("2026-10-01T10:00:00Z"), details.startsAt());
        assertEquals(Instant.parse("2026-10-01T12:00:00Z"), details.endsAt());
        assertEquals(80, details.capacity());
    }

    @Test
    void parse_invalidTimeOrCapacity_reportsSafeValidationMessage() {
        assertThrows(
                ValidationException.class,
                () -> EventFormParser.parse("Title", "", "tomorrow", "later", "many"));
    }
}
