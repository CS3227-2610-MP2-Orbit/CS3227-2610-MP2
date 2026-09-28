package seedu.eventmanager.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.event.EventPersistenceException;
import seedu.eventmanager.event.VenueRelease;

/** Organizer release of an approved booking in the Venue Administrator tables. */
public final class JdbcVenueRelease implements VenueRelease {
    static final String REASON = "Released by organizer to change event times";

    private final DataSource dataSource;

    public JdbcVenueRelease(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public boolean releaseApprovedBooking(UUID eventId, UUID organizerId) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                boolean released = release(connection, eventId, organizerId);
                if (released) {
                    connection.commit();
                } else {
                    connection.rollback();
                }
                return released;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new EventPersistenceException("Could not release the venue booking", exception);
        }
    }

    private static boolean release(Connection connection, UUID eventId, UUID organizerId) throws SQLException {
        // Lock the event first. Publish also locks this row before re-checking the booking, so the
        // two cannot interleave a PUBLISHED status with a cancelled booking.
        try (var statement = connection.prepareStatement(
                "SELECT 1 FROM organizer_event WHERE id=? AND status='DRAFT' FOR UPDATE")) {
            statement.setObject(1, eventId);
            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    return false;
                }
            }
        }
        return cancelActiveBooking(connection, eventId, organizerId, REASON, "ORGANIZER_RELEASED");
    }

    /** Cancels the event's active booking, withdraws its approved request and audits it; caller owns the transaction. */
    static boolean cancelActiveBooking(Connection connection, UUID eventId, UUID organizerId, String reason,
            String reasonCode) throws SQLException {
        UUID bookingId;
        UUID requestId;
        String previousStatus;
        try (var statement = connection.prepareStatement("""
                SELECT booking_id, request_id, status FROM venue_bookings
                WHERE event_id=? AND status IN ('CONFIRMED','AT_RISK') FOR UPDATE""")) {
            statement.setObject(1, eventId);
            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    return false;
                }
                bookingId = result.getObject("booking_id", UUID.class);
                requestId = result.getObject("request_id", UUID.class);
                previousStatus = result.getString("status");
            }
        }

        try (var statement = connection.prepareStatement("""
                UPDATE venue_bookings
                SET status='CANCELLED', cancelled_at=CURRENT_TIMESTAMP, cancelled_by=?,
                    cancellation_reason=?, updated_at=CURRENT_TIMESTAMP
                WHERE booking_id=?""")) {
            statement.setObject(1, organizerId);
            statement.setString(2, reason);
            statement.setObject(3, bookingId);
            statement.executeUpdate();
        }
        try (var statement = connection.prepareStatement("""
                UPDATE venue_requests SET status='WITHDRAWN', updated_at=CURRENT_TIMESTAMP
                WHERE request_id=? AND status='APPROVED'""")) {
            statement.setObject(1, requestId);
            statement.executeUpdate();
        }
        try (var statement = connection.prepareStatement("""
                INSERT INTO audit_logs
                    (audit_log_id, actor_id, actor_role, action, entity_type, entity_id,
                     previous_state, new_state, reason_code, created_at)
                VALUES (?, ?, ?, 'VENUE_BOOKING_RELEASED', 'VENUE_BOOKING', ?, ?, 'CANCELLED',
                        ?, CURRENT_TIMESTAMP)""")) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, organizerId);
            statement.setString(3, Role.CLUB_ORGANIZER.name());
            statement.setObject(4, bookingId);
            statement.setString(5, previousStatus);
            statement.setString(6, reasonCode);
            statement.executeUpdate();
        }
        return true;
    }
}
