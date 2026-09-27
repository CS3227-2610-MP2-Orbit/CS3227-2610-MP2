package seedu.eventmanager.service;

import java.util.UUID;
import seedu.eventmanager.venue.VenueRequest;

/** Read-only display projection for administrator venue-request lists. */
public record VenueRequestDisplay(
        VenueRequest request,
        String venueName,
        String venueLocation,
        String eventTitle,
        String organizerName) {
    public VenueRequestDisplay {
        if (request == null) throw new IllegalArgumentException("Request is required.");
        venueName = venueName == null ? "Unknown venue" : venueName;
        venueLocation = venueLocation == null ? "" : venueLocation;
        eventTitle = eventTitle == null ? "Unknown event" : eventTitle;
        organizerName = organizerName == null ? request.organizerId().toString() : organizerName;
    }

    public UUID requestId() { return request.requestId(); }
}
