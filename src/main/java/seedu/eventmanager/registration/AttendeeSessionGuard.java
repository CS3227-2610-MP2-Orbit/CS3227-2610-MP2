package seedu.eventmanager.registration;

import java.util.Objects;
import java.util.function.Function;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.common.Role;

/** Reuses shared session resolution for attendee commands and personalized reads. */
public final class AttendeeSessionGuard {
    private final Function<String, Actor> sessions;

    public AttendeeSessionGuard(Function<String, Actor> sessions) {
        this.sessions = Objects.requireNonNull(sessions);
    }

    public Actor require(String token) {
        if (token == null || token.isBlank()) throw unauthenticated();
        final Actor actor;
        try {
            actor = sessions.apply(token);
        } catch (IllegalArgumentException invalidSession) {
            throw unauthenticated();
        }
        if (actor == null || actor.userId() == null) throw unauthenticated();
        if (actor.role() != Role.ATTENDEE) {
            throw new ApplicationException("FORBIDDEN", "An attendee account is required.");
        }
        return actor;
    }

    private static ApplicationException unauthenticated() {
        return new ApplicationException("UNAUTHENTICATED", "Please log in again.");
    }
}
