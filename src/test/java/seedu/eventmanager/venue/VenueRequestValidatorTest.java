package seedu.eventmanager.venue;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.ApplicationException;

class VenueRequestValidatorTest {
    private static final UUID ID = UUID.randomUUID();
    private static final OffsetDateTime START = OffsetDateTime.parse("2030-01-01T10:00:00Z");

    @Test
    void validPositiveDurationAndAttendancePasses() {
        assertDoesNotThrow(() -> VenueRequestValidator.validate(request(START, START.plusHours(1), 1)));
    }

    @Test
    void adjacentEndAndStartIsValidBoundary() {
        assertDoesNotThrow(() -> VenueRequestValidator.validate(request(START, START.plusNanos(1), 1)));
    }

    @Test
    void nullRequestIsRejected() {
        assertCode("INVALID_REQUEST", () -> VenueRequestValidator.validate(null));
    }

    @Test
    void missingIdentifiersAreRejected() {
        VenueRequest invalid = new VenueRequest(null, ID, ID, ID, START, START.plusHours(1), 1,
                VenueRequestStatus.SUBMITTED);
        assertCode("INVALID_REQUEST", () -> VenueRequestValidator.validate(invalid));
    }

    @Test
    void equalTimesAreRejected() {
        assertCode("INVALID_TIME_RANGE", () -> VenueRequestValidator.validate(request(START, START, 1)));
    }

    @Test
    void endBeforeStartIsRejected() {
        assertCode("INVALID_TIME_RANGE", () -> VenueRequestValidator.validate(
                request(START.plusHours(1), START, 1)));
    }

    @Test
    void zeroOrNegativeAttendanceIsRejected() {
        assertCode("INVALID_ATTENDANCE", () -> VenueRequestValidator.validate(
                request(START, START.plusHours(1), 0)));
        assertCode("INVALID_ATTENDANCE", () -> VenueRequestValidator.validate(
                request(START, START.plusHours(1), -1)));
    }

    private static VenueRequest request(OffsetDateTime startsAt, OffsetDateTime endsAt, int attendance) {
        return new VenueRequest(ID, ID, ID, ID, startsAt, endsAt, attendance, VenueRequestStatus.SUBMITTED);
    }

    private static void assertCode(String code, org.junit.jupiter.api.function.Executable action) {
        ApplicationException exception = assertThrows(ApplicationException.class, action);
        assertEquals(code, exception.code());
    }
}
