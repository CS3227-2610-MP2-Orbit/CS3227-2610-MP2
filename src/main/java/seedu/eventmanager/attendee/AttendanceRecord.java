package seedu.eventmanager.attendee;

import java.time.Instant;
import java.util.UUID;

/** Read-only attendance projection, not a registration command or a historical event snapshot. */
public record AttendanceRecord(UUID eventId, String title, String description, String clubId,
        Instant startsAt, Instant endsAt, String eventStatus, String venue, Instant checkedInAt) { }
