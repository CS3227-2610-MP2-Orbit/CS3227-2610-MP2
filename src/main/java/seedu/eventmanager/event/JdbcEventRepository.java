package seedu.eventmanager.event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;

/** PostgreSQL event repository that commits an event change and its audit record atomically. */
public final class JdbcEventRepository implements EventRepository {
    private static final String SELECT_COLUMNS = """
            SELECT id, club_id, organizer_id, title, description, starts_at, ends_at,
                   capacity, status, version
            FROM organizer_event
            """;

    private final DataSource dataSource;

    public JdbcEventRepository(DataSource dataSource) {
        this.dataSource = java.util.Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public Optional<Event> findById(UUID eventId) {
        String sql = SELECT_COLUMNS + " WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, eventId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(readEvent(results)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw persistenceFailure("find event", exception);
        }
    }

    @Override
    public List<Event> findByClubIds(Set<String> clubIds) {
        if (clubIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(clubIds.size(), "?"));
        String sql = SELECT_COLUMNS + " WHERE club_id IN (" + placeholders + ") ORDER BY starts_at, id";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameter = 1;
            for (String clubId : clubIds.stream().sorted().toList()) {
                statement.setString(parameter++, clubId);
            }
            try (ResultSet results = statement.executeQuery()) {
                List<Event> events = new ArrayList<>();
                while (results.next()) {
                    events.add(readEvent(results));
                }
                return List.copyOf(events);
            }
        } catch (SQLException exception) {
            throw persistenceFailure("list events", exception);
        }
    }

    @Override
    public void create(Event event, EventAuditRecord auditRecord) {
        String sql = """
                INSERT INTO organizer_event
                    (id, club_id, organizer_id, title, description, starts_at, ends_at,
                     capacity, status, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindEvent(statement, event);
                statement.executeUpdate();
            }
            insertAudit(connection, auditRecord);
        }, "create event");
    }

    @Override
    public void update(Event event, long expectedVersion, EventAuditRecord auditRecord) {
        String sql = """
                UPDATE organizer_event
                SET title = ?, description = ?, starts_at = ?, ends_at = ?, capacity = ?,
                    status = ?, version = ?
                WHERE id = ? AND version = ?
                """;
        inTransaction(connection -> {
            int updated;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, event.title());
                statement.setString(2, event.description());
                statement.setTimestamp(3, Timestamp.from(event.startsAt()));
                statement.setTimestamp(4, Timestamp.from(event.endsAt()));
                statement.setInt(5, event.capacity());
                statement.setString(6, event.status().name());
                statement.setLong(7, event.version());
                statement.setObject(8, event.id());
                statement.setLong(9, expectedVersion);
                updated = statement.executeUpdate();
            }
            if (updated != 1) {
                throw new EventVersionConflictException("Event was modified by another operation");
            }
            insertAudit(connection, auditRecord);
        }, "update event");
    }

    private void inTransaction(SqlWork work, String operation) {
        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                work.run(connection);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (EventVersionConflictException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw persistenceFailure(operation, exception);
        }
    }

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    private static void bindEvent(PreparedStatement statement, Event event) throws SQLException {
        statement.setObject(1, event.id());
        statement.setString(2, event.clubId());
        statement.setString(3, event.organizerId());
        statement.setString(4, event.title());
        statement.setString(5, event.description());
        statement.setTimestamp(6, Timestamp.from(event.startsAt()));
        statement.setTimestamp(7, Timestamp.from(event.endsAt()));
        statement.setInt(8, event.capacity());
        statement.setString(9, event.status().name());
        statement.setLong(10, event.version());
    }

    private static void insertAudit(Connection connection, EventAuditRecord audit) throws SQLException {
        String sql = """
                INSERT INTO organizer_event_audit_record
                    (occurred_at, actor_id, action, entity_type, entity_id, resulting_version)
                VALUES (?, ?, ?, 'EVENT', ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(audit.occurredAt()));
            statement.setString(2, audit.actorId());
            statement.setString(3, audit.action().name());
            statement.setObject(4, audit.eventId());
            statement.setLong(5, audit.resultingVersion());
            statement.executeUpdate();
        }
    }

    private static Event readEvent(ResultSet results) throws SQLException {
        return new Event(
                results.getObject("id", UUID.class),
                results.getString("club_id"),
                results.getString("organizer_id"),
                results.getString("title"),
                results.getString("description"),
                results.getTimestamp("starts_at").toInstant(),
                results.getTimestamp("ends_at").toInstant(),
                results.getInt("capacity"),
                EventStatus.valueOf(results.getString("status")),
                results.getLong("version"));
    }

    private static EventPersistenceException persistenceFailure(String operation, SQLException cause) {
        return new EventPersistenceException("Unable to " + operation, cause);
    }

    @FunctionalInterface
    private interface SqlWork {
        void run(Connection connection) throws SQLException;
    }
}
