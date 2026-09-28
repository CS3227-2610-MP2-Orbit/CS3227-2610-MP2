package seedu.eventmanager.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.event.DraftEventDeletion;
import seedu.eventmanager.event.Event;
import seedu.eventmanager.event.EventAuditRecord;
import seedu.eventmanager.event.EventPersistenceException;
import seedu.eventmanager.event.EventVersionConflictException;

/** Soft-deletes a draft and clears its venue request/booking in the Venue Administrator tables atomically. */
public final class JdbcDraftEventDeletion implements DraftEventDeletion {
    static final String REASON = "Released because the organizer deleted the event";

    private final DataSource dataSource;

    public JdbcDraftEventDeletion(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public void deleteDraft(Event deleted, long expectedVersion, EventAuditRecord audit, UUID organizerId) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                markDeleted(connection, deleted, expectedVersion);
                withdrawSubmittedRequests(connection, deleted.id(), organizerId);
                JdbcVenueRelease.cancelActiveBooking(connection, deleted.id(), organizerId, REASON, "EVENT_DELETED");
                insertEventAudit(connection, audit);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new EventPersistenceException("Could not delete the event", exception);
        }
    }

    private static void markDeleted(Connection connection, Event deleted, long expectedVersion) throws SQLException {
        try (var statement = connection.prepareStatement("""
                UPDATE organizer_event SET status=?, version=?
                WHERE id=? AND version=? AND status='DRAFT'""")) {
            statement.setString(1, deleted.status().name());
            statement.setLong(2, deleted.version());
            statement.setObject(3, deleted.id());
            statement.setLong(4, expectedVersion);
            if (statement.executeUpdate() != 1) {
                throw new EventVersionConflictException("Event was modified by another operation");
            }
        }
    }

    private static void withdrawSubmittedRequests(Connection connection, UUID eventId, UUID organizerId)
            throws SQLException {
        try (var statement = connection.prepareStatement("""
                UPDATE venue_requests SET status='WITHDRAWN', updated_at=CURRENT_TIMESTAMP
                WHERE event_id=? AND status='SUBMITTED'
                RETURNING request_id""")) {
            statement.setObject(1, eventId);
            try (var withdrawn = statement.executeQuery()) {
                while (withdrawn.next()) {
                    auditWithdrawnRequest(connection, withdrawn.getObject(1, UUID.class), organizerId);
                }
            }
        }
    }

    private static void auditWithdrawnRequest(Connection connection, UUID requestId, UUID organizerId)
            throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO audit_logs
                    (audit_log_id, actor_id, actor_role, action, entity_type, entity_id,
                     previous_state, new_state, reason_code, created_at)
                VALUES (?, ?, ?, 'VENUE_REQUEST_WITHDRAWN', 'VENUE_REQUEST', ?, 'SUBMITTED', 'WITHDRAWN',
                        'EVENT_DELETED', CURRENT_TIMESTAMP)""")) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, organizerId);
            statement.setString(3, Role.CLUB_ORGANIZER.name());
            statement.setObject(4, requestId);
            statement.executeUpdate();
        }
    }

    private static void insertEventAudit(Connection connection, EventAuditRecord audit) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO organizer_event_audit_record
                    (occurred_at, actor_id, action, entity_type, entity_id, resulting_version)
                VALUES (?, ?, ?, 'EVENT', ?, ?)""")) {
            statement.setTimestamp(1, Timestamp.from(audit.occurredAt()));
            statement.setString(2, audit.actorId());
            statement.setString(3, audit.action().name());
            statement.setObject(4, audit.eventId());
            statement.setLong(5, audit.resultingVersion());
            statement.executeUpdate();
        }
    }
}
