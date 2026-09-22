package seedu.eventmanager.venue;

import java.util.UUID;

public record Venue(UUID venueId, String name, String location, int capacity,
        String description, VenueStatus status) { }
