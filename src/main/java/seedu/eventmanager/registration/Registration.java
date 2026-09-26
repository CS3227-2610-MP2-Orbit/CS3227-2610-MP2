package seedu.eventmanager.registration;

import java.time.Instant;
import java.util.UUID;

/** Owner-scoped record; this is not the Organizer-facing RegisteredAttendee DTO. */
public record Registration(UUID id, UUID eventId, UUID attendeeId, Status status,
        Instant registeredAt, Instant cancelledAt, Instant checkedInAt, long version) {
    public enum Status { CONFIRMED, CANCELLED, CHECKED_IN }
}
