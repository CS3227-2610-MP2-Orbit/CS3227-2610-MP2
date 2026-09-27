package seedu.eventmanager.attendee;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.registration.AttendeeSessionGuard;

/** Owner-only history reads with live-session validation before and after the read. */
public final class AttendanceHistoryService {
    private final AttendanceHistoryRepository repository;
    private final AttendeeSessionGuard sessions;

    public AttendanceHistoryService(AttendanceHistoryRepository repository, Function<String, Actor> sessions) {
        this.repository = Objects.requireNonNull(repository);
        this.sessions = new AttendeeSessionGuard(sessions);
    }

    public List<AttendanceRecord> list(String sessionToken) {
        Actor actor = sessions.require(sessionToken);
        var result = repository.findByAttendee(actor.userId());
        if (!actor.equals(sessions.require(sessionToken))) {
            throw new ApplicationException("UNAUTHENTICATED", "Please log in again.");
        }
        return List.copyOf(result);
    }
}
