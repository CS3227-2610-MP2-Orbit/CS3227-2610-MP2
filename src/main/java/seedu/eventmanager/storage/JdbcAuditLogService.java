package seedu.eventmanager.storage;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.service.AuditLogService;

/** Persists venue workflow audit events to PostgreSQL. */
public final class JdbcAuditLogService implements AuditLogService {
    private final JdbcDatabase database;

    public JdbcAuditLogService(JdbcDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public void record(Actor actor, String action, String entityType, UUID entityId,
            String previousState, String newState, String reason) {
        database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO audit_logs
                        (audit_log_id, actor_id, actor_role, action, entity_type,
                         entity_id, previous_state, new_state, reason_code, details, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)""")) {
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, actor == null ? null : actor.userId());
                statement.setString(3, actor == null || actor.role() == null
                        ? Role.VENUE_ADMINISTRATOR.name() : actor.role().name());
                statement.setString(4, action);
                statement.setString(5, entityType);
                statement.setObject(6, entityId);
                statement.setString(7, previousState);
                statement.setString(8, newState);
                statement.setString(9, reason);
                statement.setString(10, (String) null);
                statement.setObject(11, OffsetDateTime.now());
                statement.executeUpdate();
                return null;
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not persist audit log entry.", exception);
            }
        });
    }
}
