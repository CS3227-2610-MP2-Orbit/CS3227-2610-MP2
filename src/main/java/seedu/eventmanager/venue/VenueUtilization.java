package seedu.eventmanager.venue;

import java.util.UUID;

public record VenueUtilization(UUID venueId, String venueName, long bookingCount,
        double bookedHours, double utilizationPercentage) { }
