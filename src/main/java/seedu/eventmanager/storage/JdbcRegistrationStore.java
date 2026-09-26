package seedu.eventmanager.storage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import seedu.eventmanager.registration.Registration;
import seedu.eventmanager.registration.RegistrationEvent;
import seedu.eventmanager.registration.RegistrationStore;

/** Transaction-aware registration storage; all writers must lock the event first. */
public final class JdbcRegistrationStore implements RegistrationStore {
    private final JdbcDatabase database;

    public JdbcRegistrationStore(JdbcDatabase database) { this.database = Objects.requireNonNull(database); }

    @Override
    public RegistrationEvent lockEvent(UUID eventId) {
        return execute(c -> {
            try (var p = c.prepareStatement("SELECT id,status,capacity,starts_at,ends_at FROM organizer_event WHERE id=? FOR UPDATE")) {
                p.setObject(1, eventId); p.setQueryTimeout(15);
                try (var r = p.executeQuery()) {
                    return r.next() ? new RegistrationEvent(eventId, r.getString("status"), r.getInt("capacity"),
                            instant(r, "starts_at"), instant(r, "ends_at")) : null;
                }
            }
        });
    }

    @Override
    public boolean lockActiveAttendee(UUID attendeeId) {
        return execute(c -> {
            try (var p = c.prepareStatement("SELECT user_id FROM users WHERE user_id=? AND active=TRUE AND role='ATTENDEE' FOR SHARE")) {
                p.setObject(1, attendeeId); p.setQueryTimeout(15);
                try (var r = p.executeQuery()) { return r.next(); }
            }
        });
    }

    @Override
    public boolean lockConfirmedActiveBooking(RegistrationEvent event) {
        return execute(c -> {
            try (var p = c.prepareStatement("""
                    SELECT b.booking_id FROM venue_bookings b JOIN venues v ON v.venue_id=b.venue_id
                    WHERE b.event_id=? AND b.status='CONFIRMED' AND v.status='ACTIVE'
                      AND b.starts_at=? AND b.ends_at=?
                    FOR SHARE OF b, v
                    """)) {
                p.setObject(1, event.id());
                p.setTimestamp(2, Timestamp.from(event.startsAt())); p.setTimestamp(3, Timestamp.from(event.endsAt()));
                p.setQueryTimeout(15);
                try (var r = p.executeQuery()) { return r.next(); }
            }
        });
    }

    @Override
    public Registration find(UUID eventId, UUID attendeeId) {
        return execute(c -> {
            try (var p = c.prepareStatement("SELECT * FROM event_registrations WHERE event_id=? AND attendee_id=?")) {
                p.setObject(1, eventId); p.setObject(2, attendeeId); p.setQueryTimeout(15);
                try (var r = p.executeQuery()) { return r.next() ? row(r) : null; }
            }
        });
    }

    @Override
    public int occupiedPlaces(UUID eventId) {
        return execute(c -> {
            // Deactivation does not implicitly cancel a stored seat reservation.
            try (var p = c.prepareStatement("SELECT COUNT(*) FROM event_registrations WHERE event_id=? AND status IN ('CONFIRMED','CHECKED_IN')")) {
                p.setObject(1, eventId); p.setQueryTimeout(15);
                try (var r = p.executeQuery()) { r.next(); return r.getInt(1); }
            }
        });
    }

    @Override
    public void save(Registration value) {
        execute(c -> {
            try (var p = c.prepareStatement("""
                    INSERT INTO event_registrations
                    (registration_id,event_id,attendee_id,status,registered_at,cancelled_at,checked_in_at,version)
                    VALUES (?,?,?,?,?,?,?,?)
                    ON CONFLICT (event_id,attendee_id) DO UPDATE SET
                      status=EXCLUDED.status,registered_at=EXCLUDED.registered_at,
                      cancelled_at=EXCLUDED.cancelled_at,checked_in_at=EXCLUDED.checked_in_at,version=EXCLUDED.version
                    """)) {
                p.setObject(1, value.id()); p.setObject(2, value.eventId()); p.setObject(3, value.attendeeId());
                p.setString(4, value.status().name()); p.setTimestamp(5, timestamp(value.registeredAt()));
                p.setTimestamp(6, timestamp(value.cancelledAt())); p.setTimestamp(7, timestamp(value.checkedInAt()));
                p.setLong(8, value.version()); p.setQueryTimeout(15); p.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public List<Registration> findByAttendee(UUID attendeeId) {
        return execute(c -> {
            try (var p = c.prepareStatement("SELECT * FROM event_registrations WHERE attendee_id=? ORDER BY registered_at DESC,registration_id")) {
                p.setObject(1, attendeeId); p.setQueryTimeout(15);
                try (var r = p.executeQuery()) {
                    List<Registration> values = new ArrayList<>();
                    while (r.next()) values.add(row(r));
                    return List.copyOf(values);
                }
            }
        });
    }

    private static Registration row(ResultSet r) throws SQLException {
        return new Registration(r.getObject("registration_id", UUID.class), r.getObject("event_id", UUID.class),
                r.getObject("attendee_id", UUID.class), Registration.Status.valueOf(r.getString("status")),
                instant(r, "registered_at"), instant(r, "cancelled_at"), instant(r, "checked_in_at"), r.getLong("version"));
    }

    private static Instant instant(ResultSet r, String name) throws SQLException {
        Timestamp value = r.getTimestamp(name);
        return value == null ? null : value.toInstant();
    }

    private static Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }

    @FunctionalInterface
    private interface SqlWork<T> { T run(Connection connection) throws SQLException; }

    private <T> T execute(SqlWork<T> work) {
        return database.withConnection(c -> {
            try { return work.run(c); }
            catch (SQLException failure) { throw new IllegalStateException("Registration storage operation failed.", failure); }
        });
    }
}
