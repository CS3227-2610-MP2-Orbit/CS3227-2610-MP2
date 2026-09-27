package seedu.eventmanager.attendee;

import java.time.Instant;
import java.util.UUID;
import seedu.eventmanager.registration.Registration;

/** Owner-only display projection. Version is the displayed record's version, never a refreshed substitute. */
public record MyRegistration(UUID eventId, String title, Instant startsAt, String venue,
        Registration.Status status, long version, boolean canCancel, Instant endsAt,
        String clubId, String description, String eventStatus, boolean canCheckIn) {
    public MyRegistration(UUID eventId, String title, Instant startsAt, String venue,
            Registration.Status status, long version, boolean canCancel, Instant endsAt,
            String clubId, String description, String eventStatus) {
        this(eventId, title, startsAt, venue, status, version, canCancel, endsAt, clubId, description, eventStatus, false);
    }
}
