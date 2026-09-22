package seedu.eventmanager.storage;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import seedu.eventmanager.service.VenueBookingRepository;
import seedu.eventmanager.venue.VenueRequest;

/** PostgreSQL persistence and conflict checks for venue bookings. */
public final class JdbcVenueBookingRepository implements VenueBookingRepository {
    private final JdbcDatabase database;

    public JdbcVenueBookingRepository(JdbcDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public boolean hasConflict(UUID venueId, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT EXISTS (
                        SELECT 1 FROM venue_bookings
                        WHERE venue_id = ?
                          AND status IN ('CONFIRMED', 'AT_RISK')
                          AND tstzrange(starts_at, ends_at, '[)')
                              && tstzrange(?, ?, '[)')
                    ) OR EXISTS (
                        SELECT 1 FROM venue_availability
                        WHERE venue_id = ?
                          AND availability_type = 'BLOCKED'
                          AND tstzrange(starts_at, ends_at, '[)')
                              && tstzrange(?, ?, '[)')
                    )""")) {
                statement.setObject(1, venueId);
                statement.setObject(2, startsAt);
                statement.setObject(3, endsAt);
                statement.setObject(4, venueId);
                statement.setObject(5, startsAt);
                statement.setObject(6, endsAt);
                try (var result = statement.executeQuery()) {
                    result.next();
                    return result.getBoolean(1);
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not check venue availability.", exception);
            }
        });
    }

    @Override
    public void createFromApprovedRequest(VenueRequest request, UUID approverId) {
        database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO venue_bookings
                        (booking_id, request_id, event_id, venue_id, status,
                         starts_at, ends_at, confirmed_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'CONFIRMED', ?, ?, ?, ?, ?)""")) {
                UUID bookingId = UUID.randomUUID();
                OffsetDateTime now = OffsetDateTime.now();
                statement.setObject(1, bookingId);
                statement.setObject(2, request.requestId());
                statement.setObject(3, request.eventId());
                statement.setObject(4, request.venueId());
                statement.setObject(5, request.startsAt());
                statement.setObject(6, request.endsAt());
                statement.setObject(7, now);
                statement.setObject(8, now);
                statement.setObject(9, now);
                statement.executeUpdate();
                return null;
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not create venue booking.", exception);
            }
        });
    }
}
