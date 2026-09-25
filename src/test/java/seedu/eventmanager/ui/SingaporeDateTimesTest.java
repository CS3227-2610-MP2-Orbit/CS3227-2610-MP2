package seedu.eventmanager.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.Test;

class SingaporeDateTimesTest {
    @Test
    void toInstant_singaporeEvening_convertsToUtcStorageInstant() {
        assertEquals(
                Instant.parse("2026-10-01T10:00:00Z"),
                SingaporeDateTimes.toInstant(LocalDate.of(2026, 10, 1), "18:00"));
    }

    @Test
    void dateAndTime_storedInstant_convertBackToSingaporeValues() {
        Instant stored = Instant.parse("2026-10-01T16:30:00Z");

        assertEquals(LocalDate.of(2026, 10, 2), SingaporeDateTimes.dateOf(stored));
        assertEquals("00:30", SingaporeDateTimes.timeOf(stored));
        assertEquals("2 Oct 2026, 12:30 AM SGT", SingaporeDateTimes.display(stored));
    }

    @Test
    void toInstant_nonTwentyFourHourTime_rejected() {
        assertThrows(
                DateTimeParseException.class,
                () -> SingaporeDateTimes.toInstant(LocalDate.of(2026, 10, 1), "6:00 PM"));
    }
}
