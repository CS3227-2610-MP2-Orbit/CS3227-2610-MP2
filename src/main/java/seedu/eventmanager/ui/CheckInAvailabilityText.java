package seedu.eventmanager.ui;

import java.time.Instant;
import seedu.eventmanager.registration.CheckInPolicy;

/** Shared advisory presentation; never authorizes a command. */
final class CheckInAvailabilityText {
    private CheckInAvailabilityText() { }

    static String atDisplayTime(CheckInPolicy.Result result, Instant startsAt, Instant endsAt, Instant now) {
        // Mirror the existing button's time guard, without promoting a stale unavailable preview.
        if (result == CheckInPolicy.Result.AVAILABLE) {
            if (now.isBefore(startsAt)) result = CheckInPolicy.Result.TOO_EARLY;
            else if (!now.isBefore(endsAt)) result = CheckInPolicy.Result.CLOSED;
        }
        return message(result, startsAt, endsAt);
    }

    static String message(CheckInPolicy.Result result, Instant startsAt, Instant endsAt) {
        String explanation = result == null ? "Check-in availability is not included in this preview. Refresh for details."
                : switch (result) {
                    case AVAILABLE -> "Check-in is open. You can check in now.";
                    case NOT_REGISTERED -> "You are not registered for this event, so you cannot check in.";
                    case CANCELLED -> "Your registration is cancelled, so you cannot check in.";
                    case ALREADY_CHECKED_IN -> "You are already checked in. No further action is needed.";
                    case TOO_EARLY -> "Check-in opens at " + SingaporeDateTimes.display(startsAt) + ".";
                    case CLOSED -> "Check-in is closed: the event has ended or is no longer published.";
                    case VENUE_UNAVAILABLE -> "Check-in unavailable: a matching confirmed booking at an active venue is required.";
                };
        return explanation + "\nCheck-in window: " + SingaporeDateTimes.display(startsAt)
                + " (inclusive) to " + SingaporeDateTimes.display(endsAt) + " (exclusive)."
                + "\nAvailability is a snapshot. Refresh for the latest information; check-in rechecks every rule.";
    }
}
