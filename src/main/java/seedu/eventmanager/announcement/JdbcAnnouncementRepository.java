package seedu.eventmanager.announcement;

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
import seedu.eventmanager.event.EventPersistenceException;

/** PostgreSQL announcement repository that commits each announcement with its audit record atomically. */
public final class JdbcAnnouncementRepository implements AnnouncementRepository {
    private final DataSource dataSource;

    public JdbcAnnouncementRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public List<Announcement> findByEventId(UUID eventId) {
        String sql = """
                SELECT id, event_id, author_id, message, created_at
                FROM event_announcement
                WHERE event_id = ?
                ORDER BY created_at DESC, id
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, eventId);
            try (ResultSet results = statement.executeQuery()) {
                List<Announcement> announcements = new ArrayList<>();
                while (results.next()) {
                    announcements.add(new Announcement(
                            results.getObject("id", UUID.class),
                            results.getObject("event_id", UUID.class),
                            results.getString("author_id"),
                            results.getString("message"),
                            results.getTimestamp("created_at").toInstant()));
                }
                return List.copyOf(announcements);
            }
        } catch (SQLException exception) {
            throw new EventPersistenceException("Unable to list announcements", exception);
        }
    }

    @Override
    public void add(Announcement announcement, AnnouncementAuditRecord auditRecord) {
        String sql = """
                INSERT INTO event_announcement (id, event_id, author_id, message, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setObject(1, announcement.id());
                    statement.setObject(2, announcement.eventId());
                    statement.setString(3, announcement.authorId());
                    statement.setString(4, announcement.message());
                    statement.setTimestamp(5, Timestamp.from(announcement.createdAt()));
                    statement.executeUpdate();
                }
                insertAudit(connection, auditRecord);
                connection.commit();
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
        } catch (SQLException exception) {
            throw new EventPersistenceException("Unable to post announcement", exception);
        }
    }

    private static void insertAudit(Connection connection, AnnouncementAuditRecord audit) throws SQLException {
        String sql = """
                INSERT INTO event_announcement_audit_record
                    (occurred_at, actor_id, action, event_id, announcement_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(audit.occurredAt()));
            statement.setString(2, audit.actorId());
            statement.setString(3, audit.action().name());
            statement.setObject(4, audit.eventId());
            statement.setObject(5, audit.announcementId());
            statement.executeUpdate();
        }
    }
}
