package seedu.eventmanager.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import seedu.eventmanager.service.VenueRequestRepository;
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;

/** PostgreSQL persistence for venue requests. */
public final class JdbcVenueRequestRepository implements VenueRequestRepository {
    private final JdbcDatabase database;

    public JdbcVenueRequestRepository(JdbcDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public VenueRequest get(UUID requestId) {
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT request_id, event_id, venue_id, organizer_id,
                           requested_starts_at, requested_ends_at,
                           expected_attendance, status
                    FROM venue_requests
                    WHERE request_id = ?""")) {
                statement.setObject(1, requestId);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? map(result) : null;
                }
            } catch (SQLException exception) {
                throw databaseFailure("Could not load venue request.", exception);
            }
        });
    }

    @Override
    public void save(VenueRequest request) {
        save(request, null, null);
    }

    @Override
    public void save(VenueRequest request, UUID decidedBy, String decisionReason) {
        database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    UPDATE venue_requests
                    SET status = ?, decided_at = ?, decided_by = ?,
                        decision_reason = ?, updated_at = ?
                    WHERE request_id = ?""")) {
                OffsetDateTime now = OffsetDateTime.now();
                statement.setString(1, request.status().name());
                if (request.status() == VenueRequestStatus.APPROVED
                        || request.status() == VenueRequestStatus.REJECTED) {
                    statement.setObject(2, now);
                    statement.setObject(3, decidedBy);
                    statement.setString(4, decisionReason);
                } else {
                    statement.setObject(2, null);
                    statement.setObject(3, null);
                    statement.setString(4, null);
                }
                statement.setObject(5, now);
                statement.setObject(6, request.requestId());
                if (statement.executeUpdate() != 1) {
                    throw new IllegalStateException("Venue request was not found while saving.");
                }
                return null;
            } catch (SQLException exception) {
                throw databaseFailure("Could not save venue request.", exception);
            }
        });
    }

    private static VenueRequest map(ResultSet result) throws SQLException {
        return new VenueRequest(
                result.getObject("request_id", UUID.class),
                result.getObject("event_id", UUID.class),
                result.getObject("venue_id", UUID.class),
                result.getObject("organizer_id", UUID.class),
                result.getObject("requested_starts_at", OffsetDateTime.class),
                result.getObject("requested_ends_at", OffsetDateTime.class),
                result.getInt("expected_attendance"),
                VenueRequestStatus.valueOf(result.getString("status")));
    }

    private static IllegalStateException databaseFailure(String message, SQLException cause) {
        return new IllegalStateException(message, cause);
    }
}
