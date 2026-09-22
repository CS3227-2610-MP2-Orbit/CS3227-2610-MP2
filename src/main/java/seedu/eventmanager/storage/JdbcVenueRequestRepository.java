package seedu.eventmanager.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
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
    public List<VenueRequest> findSubmitted() {
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT request_id, event_id, venue_id, organizer_id,
                           requested_starts_at, requested_ends_at,
                           expected_attendance, status
                    FROM venue_requests
                    WHERE status = 'SUBMITTED'
                    ORDER BY submitted_at, created_at""")) {
                try (ResultSet result = statement.executeQuery()) {
                    List<VenueRequest> requests = new ArrayList<>();
                    while (result.next()) {
                        requests.add(map(result));
                    }
                    return requests;
                }
            } catch (SQLException exception) {
                throw databaseFailure("Could not load submitted venue requests.", exception);
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
                    INSERT INTO venue_requests
                        (request_id, event_id, venue_id, organizer_id,
                         requested_starts_at, requested_ends_at, expected_attendance,
                         status, submitted_at, decided_at, decided_by, decision_reason,
                         created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (request_id) DO UPDATE SET
                        status = EXCLUDED.status,
                        decided_at = EXCLUDED.decided_at,
                        decided_by = EXCLUDED.decided_by,
                        decision_reason = EXCLUDED.decision_reason,
                        updated_at = EXCLUDED.updated_at""")) {
                OffsetDateTime now = OffsetDateTime.now();
                statement.setObject(1, request.requestId());
                statement.setObject(2, request.eventId());
                statement.setObject(3, request.venueId());
                statement.setObject(4, request.organizerId());
                statement.setObject(5, request.startsAt());
                statement.setObject(6, request.endsAt());
                statement.setInt(7, request.expectedAttendance());
                statement.setString(8, request.status().name());
                statement.setObject(9, request.status() == VenueRequestStatus.DRAFT ? null : now);
                if (request.status() == VenueRequestStatus.APPROVED
                        || request.status() == VenueRequestStatus.REJECTED) {
                    statement.setObject(10, now);
                    statement.setObject(11, decidedBy);
                    statement.setString(12, decisionReason);
                } else {
                    statement.setObject(10, null);
                    statement.setObject(11, null);
                    statement.setString(12, null);
                }
                statement.setObject(13, now);
                statement.setObject(14, now);
                statement.executeUpdate();
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
