package seedu.eventmanager.attendee;

import java.time.Instant;
import java.util.UUID;
import seedu.eventmanager.registration.Registration;
import seedu.eventmanager.registration.CheckInPolicy;

/** Owner-only display projection. Version is the displayed record's version, never a refreshed substitute. */
public record MyRegistration(UUID eventId, String title, Instant startsAt, String venue,
        Registration.Status status, long version, boolean canCancel, Instant endsAt,
        String clubId, String description, String eventStatus, boolean canCheckIn, String clubName,
        CheckInPolicy.Result checkInAvailability) {
    public MyRegistration {
        clubName = CatalogueClub.displayName(clubName);
        if (checkInAvailability != null) canCheckIn = checkInAvailability == CheckInPolicy.Result.AVAILABLE;
    }

    public MyRegistration(UUID eventId, String title, Instant startsAt, String venue,
            Registration.Status status, long version, boolean canCancel, Instant endsAt,
            String clubId, String description, String eventStatus,
            CheckInPolicy.Result checkInAvailability, String clubName) {
        this(eventId, title, startsAt, venue, status, version, canCancel, endsAt, clubId,
                description, eventStatus, checkInAvailability == CheckInPolicy.Result.AVAILABLE,
                clubName, java.util.Objects.requireNonNull(checkInAvailability));
    }

    /** Compatibility for callers that have only a boolean preview, not an explanatory result. */
    public MyRegistration(UUID eventId, String title, Instant startsAt, String venue,
            Registration.Status status, long version, boolean canCancel, Instant endsAt,
            String clubId, String description, String eventStatus, boolean canCheckIn, String clubName) {
        this(eventId, title, startsAt, venue, status, version, canCancel, endsAt, clubId,
                description, eventStatus, canCheckIn, clubName, null);
    }

    public MyRegistration(UUID eventId, String title, Instant startsAt, String venue,
            Registration.Status status, long version, boolean canCancel, Instant endsAt,
            String clubId, String description, String eventStatus, boolean canCheckIn) {
        this(eventId, title, startsAt, venue, status, version, canCancel, endsAt, clubId,
                description, eventStatus, canCheckIn, null);
    }
    public MyRegistration(UUID eventId, String title, Instant startsAt, String venue,
            Registration.Status status, long version, boolean canCancel, Instant endsAt,
            String clubId, String description, String eventStatus) {
        this(eventId, title, startsAt, venue, status, version, canCancel, endsAt, clubId, description, eventStatus, false);
    }
}
