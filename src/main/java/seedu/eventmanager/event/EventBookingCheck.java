package seedu.eventmanager.event;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Read-only view of venue bookings used when editing and publishing events. */
public interface EventBookingCheck {
    /** The event's current (CONFIRMED or AT_RISK) booking window and whether its venue is ACTIVE. */
    record ActiveBooking(Instant startsAt, Instant endsAt, boolean venueActive) { }

    /**
     * Returns true when the event has a CONFIRMED booking at an ACTIVE venue whose start and end
     * exactly match the given times (the same rule Attendee registration applies).
     */
    boolean hasConfirmedActiveBooking(UUID eventId, Instant startsAt, Instant endsAt);

    /** Returns the event's current booking, if an approved venue request created one. */
    Optional<ActiveBooking> findActiveBooking(UUID eventId);
}
