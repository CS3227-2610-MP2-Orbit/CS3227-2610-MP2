package seedu.eventmanager.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import seedu.eventmanager.service.VenueRequestRepository;
import seedu.eventmanager.service.VenueRequestDisplay;
import seedu.eventmanager.service.ApprovedBookingDisplay;
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
    public List<VenueRequestDisplay> findSubmittedDisplay() {
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT vr.request_id, vr.event_id, vr.venue_id, vr.organizer_id,
                           vr.requested_starts_at, vr.requested_ends_at,
                           vr.expected_attendance, vr.status,
                           v.name AS venue_name, v.location AS venue_location,
                           e.title AS event_title, u.username AS organizer_name
                    FROM venue_requests vr
                    JOIN venues v ON v.venue_id = vr.venue_id
                    LEFT JOIN organizer_event e ON e.id = vr.event_id
                    LEFT JOIN users u ON u.user_id = vr.organizer_id
                    WHERE vr.status = 'SUBMITTED'
                    ORDER BY vr.submitted_at, vr.created_at""")) {
                try (ResultSet result = statement.executeQuery()) {
                    List<VenueRequestDisplay> values = new ArrayList<>();
                    while (result.next()) {
                        values.add(new VenueRequestDisplay(map(result),
                                result.getString("venue_name"),
                                result.getString("venue_location"),
                                result.getString("event_title"),
                                result.getString("organizer_name")));
                    }
                    return values;
                }
            } catch (SQLException exception) {
                throw databaseFailure("Could not load readable submitted venue requests.", exception);
            }
        });
    }

    @Override
    public List<ApprovedBookingDisplay> findApprovedBookingDisplays() {
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT b.booking_id, b.request_id, b.starts_at, b.ends_at, b.status,
                           vr.expected_attendance,
                           v.name AS venue_name, v.location AS venue_location,
                           e.title AS event_title, u.username AS organizer_name
                    FROM venue_bookings b
                    JOIN venue_requests vr ON vr.request_id = b.request_id
                    JOIN venues v ON v.venue_id = b.venue_id
                    LEFT JOIN organizer_event e ON e.id = b.event_id
                    LEFT JOIN users u ON u.user_id = vr.organizer_id
                    WHERE b.status IN ('CONFIRMED', 'AT_RISK', 'COMPLETED')
                    ORDER BY b.starts_at""")) {
                try (ResultSet result = statement.executeQuery()) {
                    List<ApprovedBookingDisplay> values = new ArrayList<>();
                    while (result.next()) {
                        values.add(new ApprovedBookingDisplay(
                                result.getObject("booking_id", UUID.class),
                                result.getObject("request_id", UUID.class),
                                result.getString("venue_name"),
                                result.getString("venue_location"),
                                result.getString("event_title"),
                                result.getString("organizer_name"),
                                result.getObject("starts_at", OffsetDateTime.class),
                                result.getObject("ends_at", OffsetDateTime.class),
                                result.getInt("expected_attendance"),
                                result.getString("status")));
                    }
                    return values;
                }
            } catch (SQLException exception) {
                throw databaseFailure("Could not load approved venue bookings.", exception);
            }
        });
    }

    @Override
    public Optional<VenueRequest> findOpenByEventId(UUID eventId) {
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT request_id, event_id, venue_id, organizer_id,
                           requested_starts_at, requested_ends_at,
                           expected_attendance, status
                    FROM venue_requests
                    WHERE event_id = ?
                      AND status IN ('DRAFT', 'SUBMITTED')
                    ORDER BY created_at
                    LIMIT 1""")) {
                statement.setObject(1, eventId);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next()
                            ? Optional.of(map(result))
                            : Optional.empty();
                }
            } catch (SQLException exception) {
                throw databaseFailure("Could not load open venue request for event.", exception);
            }
        });
    }

    @Override
    public Optional<VenueRequest> findLatestByEventId(UUID eventId) {
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT request_id, event_id, venue_id, organizer_id,
                           requested_starts_at, requested_ends_at,
                           expected_attendance, status
                    FROM venue_requests
                    WHERE event_id = ?
                    ORDER BY COALESCE(updated_at, created_at) DESC, created_at DESC
                    LIMIT 1""")) {
                statement.setObject(1, eventId);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next()
                            ? Optional.of(map(result))
                            : Optional.empty();
                }
            } catch (SQLException exception) {
                throw databaseFailure("Could not load latest venue request for event.", exception);
            }
        });
    }

    @Override
    public boolean updateExpectedAttendance(UUID requestId, int expectedAttendance) {
        if (expectedAttendance <= 0) {
            throw new IllegalArgumentException("Expected attendance must be positive");
        }
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    UPDATE venue_requests
                    SET expected_attendance = ?, updated_at = ?
                    WHERE request_id = ?
                      AND status IN ('DRAFT', 'SUBMITTED')""")) {
                statement.setInt(1, expectedAttendance);
                statement.setObject(2, OffsetDateTime.now());
                statement.setObject(3, requestId);
                return statement.executeUpdate() > 0;
            } catch (SQLException exception) {
                throw databaseFailure("Could not update venue request attendance.", exception);
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
