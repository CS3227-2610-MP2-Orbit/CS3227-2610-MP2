package seedu.eventmanager.event;

import java.util.UUID;

/** Organizer-initiated release of an approved venue booking for an unpublished event. */
@FunctionalInterface
public interface VenueRelease {
    /**
     * Cancels the event's CONFIRMED/AT_RISK booking, withdraws the approved request and records a
     * business audit entry in one transaction, only while the event is still a draft.
     *
     * @return false when there was no approved booking to release or the event is no longer a draft
     */
    boolean releaseApprovedBooking(UUID eventId, UUID organizerId);
}
