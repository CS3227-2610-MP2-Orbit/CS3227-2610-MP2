package seedu.eventmanager.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import seedu.eventmanager.attendee.EventCatalogueRepository;
import seedu.eventmanager.attendee.CatalogueClub;
import seedu.eventmanager.event.Event;
import seedu.eventmanager.event.EventPersistenceException;
import seedu.eventmanager.event.EventStatus;

/** Read-only access to the canonical Organizer table; does not migrate or publish events. */
public final class JdbcEventCatalogueRepository implements EventCatalogueRepository {
    private static final String PUBLIC_EVENTS = """
            SELECT e.id, e.club_id, e.organizer_id, e.title, e.description, e.starts_at, e.ends_at,
                   e.capacity, e.status, e.version, c.name AS club_name
            FROM organizer_event e
            """ + AttendeeClubSql.JOIN + " WHERE e.status = 'PUBLISHED' ";
    private final DataSource dataSource;

    public JdbcEventCatalogueRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public List<Entry> findPublishedNotEnded(Instant now) {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(
                        PUBLIC_EVENTS + " AND e.ends_at > ? ORDER BY e.starts_at, e.id")) {
            statement.setQueryTimeout(15);
            statement.setTimestamp(1, Timestamp.from(now));
            try (var rows = statement.executeQuery()) {
                List<Entry> events = new ArrayList<>();
                while (rows.next()) {
                    events.add(read(rows));
                }
                return List.copyOf(events);
            }
        } catch (SQLException exception) {
            throw new EventPersistenceException("Unable to load the event catalogue.", exception);
        }
    }

    @Override
    public Optional<Entry> findPublishedById(UUID id) {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(PUBLIC_EVENTS + " AND e.id = ?")) {
            statement.setQueryTimeout(15);
            statement.setObject(1, id);
            try (var rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(read(rows)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new EventPersistenceException("Unable to load event details.", exception);
        }
    }

    @Override
    public List<CatalogueClub> findClubs() {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("SELECT id, name FROM organizer_club")) {
            statement.setQueryTimeout(15);
            try (var rows = statement.executeQuery()) {
                List<CatalogueClub> clubs = new ArrayList<>();
                while (rows.next()) {
                    clubs.add(new CatalogueClub(rows.getString("id"), rows.getString("name")));
                }
                return List.copyOf(clubs);
            }
        } catch (SQLException exception) {
            throw new EventPersistenceException("Unable to load clubs.", exception);
        }
    }

    private static Entry read(ResultSet rows) throws SQLException {
        return new Entry(new Event(rows.getObject("id", UUID.class), rows.getString("club_id"),
                rows.getString("organizer_id"), rows.getString("title"), rows.getString("description"),
                rows.getTimestamp("starts_at").toInstant(), rows.getTimestamp("ends_at").toInstant(),
                rows.getInt("capacity"), EventStatus.valueOf(rows.getString("status")), rows.getLong("version")),
                rows.getString("club_name"));
    }
}
