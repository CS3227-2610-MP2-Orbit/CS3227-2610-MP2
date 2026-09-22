package seedu.eventmanager.venue;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VenueRequest(UUID requestId, UUID eventId, UUID venueId, UUID organizerId,
        OffsetDateTime startsAt, OffsetDateTime endsAt, int expectedAttendance,
        VenueRequestStatus status) { }
