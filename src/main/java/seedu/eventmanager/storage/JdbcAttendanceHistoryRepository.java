package seedu.eventmanager.storage;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.ArrayList;
import java.sql.SQLException;
import seedu.eventmanager.attendee.AttendanceHistoryRepository;
import seedu.eventmanager.attendee.AttendanceRecord;

/** Single-statement history snapshot, with no public-catalogue visibility filter or write locks. */
public final class JdbcAttendanceHistoryRepository implements AttendanceHistoryRepository {
    private static final String QUERY = """
            SELECT e.id,e.title,e.description,e.club_id,e.starts_at,e.ends_at,e.status,
                   v.name,v.location,r.checked_in_at
            FROM event_registrations r
            JOIN users viewer ON viewer.user_id=r.attendee_id AND viewer.active=TRUE AND viewer.role='ATTENDEE'
            JOIN organizer_event e ON e.id=r.event_id
            LEFT JOIN LATERAL (%s) booking ON TRUE
            LEFT JOIN venues v ON v.venue_id=booking.venue_id
            WHERE r.attendee_id=? AND r.status='CHECKED_IN'
            ORDER BY r.checked_in_at DESC,r.registration_id
            """.formatted(RegistrationReadSql.displayBooking("e.id"));
    private final JdbcDatabase database;

    public JdbcAttendanceHistoryRepository(JdbcDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override public List<AttendanceRecord> findByAttendee(UUID attendeeId) {
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement(QUERY)) {
                statement.setObject(1, attendeeId);
                statement.setQueryTimeout(15);
                try (var rows = statement.executeQuery()) {
                    List<AttendanceRecord> result = new ArrayList<>();
                    while (rows.next()) {
                        String venue = rows.getString("name");
                        result.add(new AttendanceRecord(rows.getObject("id", UUID.class), rows.getString("title"),
                                rows.getString("description"), rows.getString("club_id"),
                                rows.getTimestamp("starts_at").toInstant(), rows.getTimestamp("ends_at").toInstant(),
                                rows.getString("status"), venue == null ? "" : venue + " · " + rows.getString("location"),
                                rows.getTimestamp("checked_in_at").toInstant()));
                    }
                    return List.copyOf(result);
                }
            } catch (SQLException failure) {
                throw new IllegalStateException("Unable to load attendance history.", failure);
            }
        });
    }
}
