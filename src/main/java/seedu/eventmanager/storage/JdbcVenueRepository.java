package seedu.eventmanager.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import seedu.eventmanager.service.VenueRepository;
import seedu.eventmanager.venue.Venue;
import seedu.eventmanager.venue.VenueStatus;

/** PostgreSQL persistence for venue records. */
public final class JdbcVenueRepository implements VenueRepository {
    private final JdbcDatabase database;

    public JdbcVenueRepository(JdbcDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public List<Venue> findAll() {
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT venue_id, name, location, capacity, description, status
                    FROM venues ORDER BY name, location""")) {
                try (ResultSet result = statement.executeQuery()) {
                    List<Venue> venues = new ArrayList<>();
                    while (result.next()) venues.add(map(result));
                    return venues;
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not load venues.", exception);
            }
        });
    }

    @Override
    public Venue findById(UUID venueId) {
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT venue_id, name, location, capacity, description, status
                    FROM venues WHERE venue_id = ?""")) {
                statement.setObject(1, venueId);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? map(result) : null;
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not load venue.", exception);
            }
        });
    }

    @Override
    public void save(Venue venue) {
        database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO venues (venue_id, name, location, capacity, description,
                        status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    ON CONFLICT (venue_id) DO UPDATE SET
                        name = EXCLUDED.name, location = EXCLUDED.location,
                        capacity = EXCLUDED.capacity, description = EXCLUDED.description,
                        status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP""")) {
                statement.setObject(1, venue.venueId());
                statement.setString(2, venue.name());
                statement.setString(3, venue.location());
                statement.setInt(4, venue.capacity());
                statement.setString(5, venue.description());
                statement.setString(6, venue.status().name());
                statement.executeUpdate();
                return null;
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not save venue.", exception);
            }
        });
    }

    private static Venue map(ResultSet result) throws SQLException {
        return new Venue(result.getObject("venue_id", UUID.class), result.getString("name"),
                result.getString("location"), result.getInt("capacity"),
                result.getString("description"), VenueStatus.valueOf(result.getString("status")));
    }
}
