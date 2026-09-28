package seedu.eventmanager.ui;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import seedu.eventmanager.attendee.MyRegistration;
import seedu.eventmanager.registration.Registration;

/** Session-bound callbacks supplied by composition, not by controls or user-entered identity. */
public record AttendeeRegistrationActions(BiFunction<UUID, Long, Registration> register,
        BiFunction<UUID, Long, Registration> cancel, Supplier<List<MyRegistration>> myRegistrations) {
    public AttendeeRegistrationActions {
        Objects.requireNonNull(register);
        Objects.requireNonNull(cancel);
        Objects.requireNonNull(myRegistrations);
    }
}
