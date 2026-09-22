package seedu.eventmanager.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import seedu.eventmanager.service.VenueAvailabilityRepository;
import seedu.eventmanager.venue.VenueAvailability;
import seedu.eventmanager.venue.VenueAvailabilityType;

/** PostgreSQL persistence for venue availability intervals. */
public final class JdbcVenueAvailabilityRepository implements VenueAvailabilityRepository {
    private final JdbcDatabase database;

    public JdbcVenueAvailabilityRepository(JdbcDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public List<VenueAvailability> findAll() {
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT availability_id, venue_id, availability_type,
                           starts_at, ends_at, reason
                    FROM venue_availability ORDER BY starts_at""")) {
                try (ResultSet result = statement.executeQuery()) {
                    List<VenueAvailability> values = new ArrayList<>();
                    while (result.next()) values.add(map(result));
                    return values;
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not load venue availability.", exception);
            }
        });
    }

    @Override
    public void save(VenueAvailability availability) {
        database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO venue_availability
                        (availability_id, venue_id, availability_type, starts_at,
                         ends_at, reason, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    ON CONFLICT (availability_id) DO UPDATE SET
                        availability_type = EXCLUDED.availability_type,
                        starts_at = EXCLUDED.starts_at, ends_at = EXCLUDED.ends_at,
                        reason = EXCLUDED.reason""")) {
                statement.setObject(1, availability.availabilityId());
                statement.setObject(2, availability.venueId());
                statement.setString(3, availability.type().name());
                statement.setObject(4, availability.startsAt());
                statement.setObject(5, availability.endsAt());
                statement.setString(6, availability.reason());
                statement.executeUpdate();
                return null;
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not save venue availability.", exception);
            }
        });
    }

    @Override
    public void delete(UUID availabilityId) {
        database.withConnection(connection -> {
            try (var statement = connection.prepareStatement(
                    "DELETE FROM venue_availability WHERE availability_id = ?")) {
                statement.setObject(1, availabilityId);
                statement.executeUpdate();
                return null;
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not delete venue availability.", exception);
            }
        });
    }

    private static VenueAvailability map(ResultSet result) throws SQLException {
        return new VenueAvailability(result.getObject("availability_id", UUID.class),
                result.getObject("venue_id", UUID.class),
                VenueAvailabilityType.valueOf(result.getString("availability_type")),
                result.getObject("starts_at", OffsetDateTime.class),
                result.getObject("ends_at", OffsetDateTime.class), result.getString("reason"));
    }
}
