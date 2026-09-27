package seedu.eventmanager.service;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Read-only, human-readable projection of an approved venue booking. */
public record ApprovedBookingDisplay(
        UUID bookingId,
        UUID requestId,
        String venueName,
        String venueLocation,
        String eventTitle,
        String organizerName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        int expectedAttendance,
        String status) {
    public String venueLabel() {
        return venueName + " — " + venueLocation;
    }
}
