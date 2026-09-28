package seedu.eventmanager.attendee;

import java.time.Instant;
import java.util.UUID;

/** Read-only attendance projection, not a registration command or a historical event snapshot. */
public record AttendanceRecord(UUID eventId, String title, String description, String clubId,
        Instant startsAt, Instant endsAt, String eventStatus, String venue, Instant checkedInAt, String clubName) {
    public AttendanceRecord {
        clubName = CatalogueClub.displayName(clubName);
    }

    public AttendanceRecord(UUID eventId, String title, String description, String clubId,
            Instant startsAt, Instant endsAt, String eventStatus, String venue, Instant checkedInAt) {
        this(eventId, title, description, clubId, startsAt, endsAt, eventStatus, venue, checkedInAt, null);
    }
}
