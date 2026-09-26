package seedu.eventmanager.club;

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

/** PostgreSQL club repository that commits each club with its audit record atomically. */
public final class JdbcClubRepository implements ClubRepository {
    private static final String UNIQUE_VIOLATION = "23505";

    private final DataSource dataSource;

    public JdbcClubRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public List<Club> findByOwner(UUID ownerId) {
        String sql = """
                SELECT id, name, owner_id, created_at
                FROM organizer_club
                WHERE owner_id = ?
                ORDER BY lower(name), id
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, ownerId);
            try (ResultSet results = statement.executeQuery()) {
                List<Club> clubs = new ArrayList<>();
                while (results.next()) {
                    clubs.add(new Club(
                            results.getObject("id", UUID.class),
                            results.getString("name"),
                            results.getObject("owner_id", UUID.class),
                            results.getTimestamp("created_at").toInstant()));
                }
                return List.copyOf(clubs);
            }
        } catch (SQLException exception) {
            throw new EventPersistenceException("Unable to list clubs", exception);
        }
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        String sql = "SELECT 1 FROM organizer_club WHERE lower(name) = lower(?)";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        } catch (SQLException exception) {
            throw new EventPersistenceException("Unable to check club name", exception);
        }
    }

    @Override
    public void create(Club club, ClubAuditRecord auditRecord) {
        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                insertClub(connection, club);
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
            if (UNIQUE_VIOLATION.equals(exception.getSQLState())) {
                throw ClubService.duplicateName(club.name());
            }
            throw new EventPersistenceException("Unable to create club", exception);
        }
    }

    private static void insertClub(Connection connection, Club club) throws SQLException {
        String sql = "INSERT INTO organizer_club (id, name, owner_id, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, club.id());
            statement.setString(2, club.name());
            statement.setObject(3, club.ownerId());
            statement.setTimestamp(4, Timestamp.from(club.createdAt()));
            statement.executeUpdate();
        }
    }

    private static void insertAudit(Connection connection, ClubAuditRecord audit) throws SQLException {
        String sql = """
                INSERT INTO organizer_club_audit_record (occurred_at, actor_id, action, club_id)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(audit.occurredAt()));
            statement.setObject(2, audit.actorId());
            statement.setString(3, audit.action().name());
            statement.setObject(4, audit.clubId());
            statement.executeUpdate();
        }
    }
}
