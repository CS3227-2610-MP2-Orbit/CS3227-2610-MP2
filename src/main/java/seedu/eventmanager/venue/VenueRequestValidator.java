package seedu.eventmanager.venue;

import seedu.eventmanager.common.ApplicationException;

public final class VenueRequestValidator {
    private VenueRequestValidator() { }

    public static void validate(VenueRequest request) {
        if (request == null || request.requestId() == null || request.eventId() == null
                || request.venueId() == null || request.organizerId() == null) {
            throw new ApplicationException("INVALID_REQUEST", "Request identifiers are required.");
        }
        if (request.startsAt() == null || request.endsAt() == null
                || !request.endsAt().isAfter(request.startsAt())) {
            throw new ApplicationException("INVALID_TIME_RANGE", "The booking end must be after its start.");
        }
        if (request.expectedAttendance() <= 0) {
            throw new ApplicationException("INVALID_ATTENDANCE", "Expected attendance must be positive.");
        }
    }
}
