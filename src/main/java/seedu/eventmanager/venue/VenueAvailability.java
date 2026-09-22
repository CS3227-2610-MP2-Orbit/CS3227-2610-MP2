package seedu.eventmanager.venue;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VenueAvailability(UUID availabilityId, UUID venueId, VenueAvailabilityType type,
        OffsetDateTime startsAt, OffsetDateTime endsAt, String reason) { }
