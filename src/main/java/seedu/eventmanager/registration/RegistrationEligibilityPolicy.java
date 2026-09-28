package seedu.eventmanager.registration;

import java.time.Instant;

/** Pure eligibility rules shared by read previews and locked registration commands. */
public final class RegistrationEligibilityPolicy {
    public enum Booking { CONFIRMED_ACTIVE, VENUE_INACTIVE, UNCONFIRMED }
    public enum Result { AVAILABLE, EVENT_NOT_REGISTERABLE, VENUE_NOT_CONFIRMED, VENUE_INACTIVE, EVENT_FULL }

    private RegistrationEligibilityPolicy() { }

    public static boolean eventOpen(RegistrationEvent event, Instant now) {
        return "PUBLISHED".equals(event.status()) && now.isBefore(event.startsAt());
    }

    public static boolean hasCapacity(int capacity, int occupied) {
        return occupied < capacity;
    }

    public static Result evaluate(RegistrationEvent event, Instant now, Booking booking, int occupied) {
        if (!eventOpen(event, now)) return Result.EVENT_NOT_REGISTERABLE;
        if (booking == Booking.UNCONFIRMED) return Result.VENUE_NOT_CONFIRMED;
        if (booking == Booking.VENUE_INACTIVE) return Result.VENUE_INACTIVE;
        if (!hasCapacity(event.capacity(), occupied)) return Result.EVENT_FULL;
        return Result.AVAILABLE;
    }
}
