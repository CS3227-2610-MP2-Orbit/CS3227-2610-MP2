package seedu.eventmanager.volunteer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.event.EventPersistenceException;

/** PostgreSQL volunteer repository that commits each change with its audit record atomically. */
public final class JdbcVolunteerRepository implements VolunteerRepository {
    private static final String UNIQUE_VIOLATION = "23505";

    private final DataSource dataSource;

    public JdbcVolunteerRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public List<VolunteerAssignment> findByEventId(UUID eventId) {
        String sql = """
                SELECT event_id, attendee_id, role, assigned_by, assigned_at
                FROM event_volunteer
                WHERE event_id = ?
                ORDER BY assigned_at, attendee_id
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, eventId);
            try (ResultSet results = statement.executeQuery()) {
                List<VolunteerAssignment> assignments = new ArrayList<>();
                while (results.next()) {
                    assignments.add(new VolunteerAssignment(
                            results.getObject("event_id", UUID.class),
                            results.getObject("attendee_id", UUID.class),
                            results.getString("role"),
                            results.getString("assigned_by"),
                            results.getTimestamp("assigned_at").toInstant()));
                }
                return List.copyOf(assignments);
            }
        } catch (SQLException exception) {
            throw persistenceFailure("list volunteers", exception);
        }
    }

    @Override
    public boolean exists(UUID eventId, UUID attendeeId) {
        String sql = "SELECT 1 FROM event_volunteer WHERE event_id = ? AND attendee_id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, eventId);
            statement.setObject(2, attendeeId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        } catch (SQLException exception) {
            throw persistenceFailure("check volunteer", exception);
        }
    }

    @Override
    public void add(VolunteerAssignment assignment, VolunteerAuditRecord auditRecord) {
        String sql = """
                INSERT INTO event_volunteer (event_id, attendee_id, role, assigned_by, assigned_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try {
            inTransaction(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setObject(1, assignment.eventId());
                    statement.setObject(2, assignment.attendeeId());
                    statement.setString(3, assignment.role());
                    statement.setString(4, assignment.assignedBy());
                    statement.setTimestamp(5, Timestamp.from(assignment.assignedAt()));
                    statement.executeUpdate();
                }
                insertAudit(connection, auditRecord);
                return null;
            });
        } catch (SQLException exception) {
            if (UNIQUE_VIOLATION.equals(exception.getSQLState())) {
                throw new ValidationException("This attendee is already a volunteer for this event");
            }
            throw persistenceFailure("assign volunteer", exception);
        }
    }

    @Override
    public boolean remove(UUID eventId, UUID attendeeId, VolunteerAuditRecord auditRecord) {
        String sql = "DELETE FROM event_volunteer WHERE event_id = ? AND attendee_id = ?";
        try {
            return inTransaction(connection -> {
                int deleted;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setObject(1, eventId);
                    statement.setObject(2, attendeeId);
                    deleted = statement.executeUpdate();
                }
                if (deleted == 0) {
                    return false;
                }
                insertAudit(connection, auditRecord);
                return true;
            });
        } catch (SQLException exception) {
            throw persistenceFailure("remove volunteer", exception);
        }
    }

    private <T> T inTransaction(SqlWork<T> work) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    private static void insertAudit(Connection connection, VolunteerAuditRecord audit) throws SQLException {
        String sql = """
                INSERT INTO event_volunteer_audit_record
                    (occurred_at, actor_id, action, event_id, attendee_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(audit.occurredAt()));
            statement.setString(2, audit.actorId());
            statement.setString(3, audit.action().name());
            statement.setObject(4, audit.eventId());
            statement.setObject(5, audit.attendeeId());
            statement.executeUpdate();
        }
    }

    private static EventPersistenceException persistenceFailure(String operation, SQLException cause) {
        return new EventPersistenceException("Unable to " + operation, cause);
    }

    @FunctionalInterface
    private interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }
}
