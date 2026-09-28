package seedu.eventmanager.registration;

import java.time.Instant;

/** Deterministic check-in rules shared by the locked command and read previews. */
public final class CheckInPolicy {
    public enum Result { AVAILABLE, NOT_REGISTERED, CANCELLED, ALREADY_CHECKED_IN, TOO_EARLY, CLOSED, VENUE_UNAVAILABLE }
    private CheckInPolicy() { }

    public static Result evaluate(RegistrationEvent event, Registration.Status status,
            boolean confirmedActiveBooking, Instant now) {
        if (status == null) return Result.NOT_REGISTERED;
        if (status == Registration.Status.CANCELLED) return Result.CANCELLED;
        if (status == Registration.Status.CHECKED_IN) return Result.ALREADY_CHECKED_IN;
        if (!"PUBLISHED".equals(event.status()) || !now.isBefore(event.endsAt())) return Result.CLOSED;
        if (now.isBefore(event.startsAt())) return Result.TOO_EARLY;
        if (!confirmedActiveBooking) return Result.VENUE_UNAVAILABLE;
        return Result.AVAILABLE;
    }
}
