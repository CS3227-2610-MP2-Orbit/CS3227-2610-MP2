package seedu.eventmanager.storage;

import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import seedu.eventmanager.registration.EventRegistrations;
import seedu.eventmanager.registration.RegisteredAttendee;

/** Internal read adapter; calling Organizer services must enforce event ownership. */
public final class JdbcEventRegistrations implements EventRegistrations {
    private final JdbcDatabase database;

    public JdbcEventRegistrations(JdbcDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public List<RegisteredAttendee> registeredAttendees(UUID eventId) {
        Objects.requireNonNull(eventId);
        return database.withConnection(connection -> {
            // The unique (event_id, attendee_id) key prevents duplicate lifecycles.
            // Select only the two fields in Joseph's contract, never users.*.
            try (var statement = connection.prepareStatement("""
                    SELECT u.user_id, u.username
                    FROM event_registrations r JOIN users u ON u.user_id = r.attendee_id
                    WHERE r.event_id = ? AND r.status IN ('CONFIRMED', 'CHECKED_IN')
                      AND u.active = TRUE AND u.role = 'ATTENDEE'
                    """)) {
                statement.setObject(1, eventId);
                statement.setQueryTimeout(15);
                try (var rows = statement.executeQuery()) {
                    List<RegisteredAttendee> attendees = new ArrayList<>();
                    while (rows.next()) {
                        attendees.add(new RegisteredAttendee(rows.getObject("user_id", UUID.class),
                                rows.getString("username")));
                    }
                    return List.copyOf(attendees);
                }
            } catch (SQLException failure) {
                throw new IllegalStateException("Could not load event registrants.", failure);
            }
        });
    }
}
