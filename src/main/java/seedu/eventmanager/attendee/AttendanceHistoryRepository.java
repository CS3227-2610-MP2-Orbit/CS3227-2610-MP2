package seedu.eventmanager.attendee;

import java.util.List;
import java.util.UUID;

/** Internal read boundary; caller identity must be resolved by AttendanceHistoryService. */
public interface AttendanceHistoryRepository {
    List<AttendanceRecord> findByAttendee(UUID attendeeId);
}
