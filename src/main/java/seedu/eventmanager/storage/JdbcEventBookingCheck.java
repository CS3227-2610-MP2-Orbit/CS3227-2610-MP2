package seedu.eventmanager.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import seedu.eventmanager.event.EventBookingCheck;
import seedu.eventmanager.event.EventPersistenceException;

/** Publish-time booking check sharing the Attendee registration booking predicate. */
public final class JdbcEventBookingCheck implements EventBookingCheck {
    private static final String QUERY = "SELECT EXISTS (SELECT 1 FROM venue_bookings b "
            + "JOIN venues v ON v.venue_id=b.venue_id WHERE b.event_id=? AND "
            + RegistrationReadSql.matchingConfirmedBooking("?", "?") + " AND " + RegistrationReadSql.ACTIVE_VENUE + ")";
    // Venue schema allows at most one CONFIRMED/AT_RISK booking per event.
    private static final String ACTIVE_BOOKING = "SELECT b.starts_at, b.ends_at, " + RegistrationReadSql.ACTIVE_VENUE
            + " AS venue_active FROM venue_bookings b JOIN venues v ON v.venue_id=b.venue_id"
            + " WHERE b.event_id=? AND b.status IN ('CONFIRMED','AT_RISK')";

    private final DataSource dataSource;

    public JdbcEventBookingCheck(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public boolean hasConfirmedActiveBooking(UUID eventId, Instant startsAt, Instant endsAt) {
        try (var connection = dataSource.getConnection()) {
            return hasConfirmedActiveBooking(connection, eventId, startsAt, endsAt);
        } catch (SQLException exception) {
            throw new EventPersistenceException("Could not check the event's venue booking", exception);
        }
    }

    /**
     * Same matching-booking predicate as the DataSource check, using the caller's transaction so a
     * publish can re-check after locking {@code organizer_event}.
     */
    public static boolean hasConfirmedActiveBooking(
            Connection connection, UUID eventId, Instant startsAt, Instant endsAt) throws SQLException {
        try (var statement = connection.prepareStatement(QUERY)) {
            statement.setObject(1, eventId);
            statement.setTimestamp(2, Timestamp.from(startsAt));
            statement.setTimestamp(3, Timestamp.from(endsAt));
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    @Override
    public Optional<ActiveBooking> findActiveBooking(UUID eventId) {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(ACTIVE_BOOKING)) {
            statement.setObject(1, eventId);
            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ActiveBooking(
                        result.getTimestamp("starts_at").toInstant(),
                        result.getTimestamp("ends_at").toInstant(),
                        result.getBoolean("venue_active")));
            }
        } catch (SQLException exception) {
            throw new EventPersistenceException("Could not load the event's venue booking", exception);
        }
    }
}
