package seedu.eventmanager.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.registration.CheckInPolicy.Result;

class CheckInAvailabilityTextTest {
    private final Instant start = Instant.parse("2030-01-01T10:00:00Z");
    private final Instant end = start.plusSeconds(3600);

    @Test void everyPolicyResultHasDistinctExplanationAndExplicitSingaporeWindow() {
        var expected = Map.of(
                Result.AVAILABLE, "Check-in is open",
                Result.NOT_REGISTERED, "You are not registered",
                Result.CANCELLED, "Your registration is cancelled",
                Result.ALREADY_CHECKED_IN, "You are already checked in",
                Result.TOO_EARLY, "Check-in opens at 1 Jan 2030, 6:00 PM SGT",
                Result.CLOSED, "Check-in is closed",
                Result.VENUE_UNAVAILABLE, "matching confirmed booking at an active venue");
        for (var entry : expected.entrySet()) {
            String message = CheckInAvailabilityText.message(entry.getKey(), start, end);
            assertTrue(message.contains(entry.getValue()), entry.getKey() + ": " + message);
            assertTrue(message.contains("1 Jan 2030, 6:00 PM SGT"), message);
            assertTrue(message.contains("1 Jan 2030, 7:00 PM SGT"), message);
        }
    }

    @Test void legacyBooleanOnlyPreviewDoesNotInventAReason() {
        assertTrue(CheckInAvailabilityText.message(null, start, end).contains("Refresh"));
    }

    @Test void displayTimeGuardMatchesExistingButtonAtAllFourBoundariesWithoutPromotingStalePreviews() {
        assertEquals(CheckInAvailabilityText.message(Result.TOO_EARLY, start, end),
                CheckInAvailabilityText.atDisplayTime(Result.AVAILABLE, start, end, start.minusNanos(1)));
        for (var now : new Instant[] {start, end.minusNanos(1)}) {
            assertEquals(CheckInAvailabilityText.message(Result.AVAILABLE, start, end),
                    CheckInAvailabilityText.atDisplayTime(Result.AVAILABLE, start, end, now));
        }
        assertEquals(CheckInAvailabilityText.message(Result.CLOSED, start, end),
                CheckInAvailabilityText.atDisplayTime(Result.AVAILABLE, start, end, end));
        assertEquals(CheckInAvailabilityText.message(Result.TOO_EARLY, start, end),
                CheckInAvailabilityText.atDisplayTime(Result.TOO_EARLY, start, end, start));
    }
}
