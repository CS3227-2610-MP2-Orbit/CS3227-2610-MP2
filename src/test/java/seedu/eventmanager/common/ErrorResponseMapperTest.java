package seedu.eventmanager.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ErrorResponseMapperTest {
    @Test
    void mapsAuthenticationAndAuthorizationFailures() {
        assertEquals(401, ErrorResponseMapper.map(
                new ApplicationException("UNAUTHENTICATED", "internal detail"), "c1").httpStatus());
        assertEquals(403, ErrorResponseMapper.map(
                new ApplicationException("FORBIDDEN", "internal detail"), "c2").httpStatus());
    }

    @Test
    void mapsConflictAndValidationFailuresWithoutLeakingDetails() {
        ErrorResponse conflict = ErrorResponseMapper.map(
                new ApplicationException("BOOKING_CONFLICT", "SQL constraint detail"), "c3");
        assertEquals(409, conflict.httpStatus());
        assertEquals("The venue is unavailable for the requested time.", conflict.message());
    }

    @Test
    void mapsUnexpectedFailuresToSafeInternalError() {
        ErrorResponse response = ErrorResponseMapper.map(new IllegalStateException("password=secret"), "c4");
        assertEquals(500, response.httpStatus());
        assertEquals("INTERNAL_ERROR", response.code());
        assertEquals("The request could not be completed.", response.message());
    }
}
