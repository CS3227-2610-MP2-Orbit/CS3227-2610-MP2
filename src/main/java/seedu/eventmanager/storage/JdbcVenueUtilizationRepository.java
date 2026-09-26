package seedu.eventmanager.storage;

import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import seedu.eventmanager.service.VenueUtilizationRepository;
import seedu.eventmanager.venue.VenueUtilization;

/** PostgreSQL utilization report based on confirmed and at-risk bookings. */
public final class JdbcVenueUtilizationRepository implements VenueUtilizationRepository {
    private final JdbcDatabase database;

    public JdbcVenueUtilizationRepository(JdbcDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public List<VenueUtilization> findForWindow(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("Utilization window must end after it starts.");
        }
        double windowHours = Duration.between(startsAt, endsAt).toMinutes() / 60.0;
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT v.venue_id, v.name, COUNT(b.booking_id),
                           CASE WHEN COUNT(b.booking_id) = 0 THEN 0.0
                                ELSE COALESCE(SUM(EXTRACT(EPOCH FROM
                                    (LEAST(b.ends_at, ?) - GREATEST(b.starts_at, ?))) / 3600.0), 0.0)
                           END
                    FROM venues v
                    LEFT JOIN venue_bookings b ON b.venue_id = v.venue_id
                        AND b.status IN ('CONFIRMED', 'AT_RISK')
                        AND b.starts_at < ? AND b.ends_at > ?
                    GROUP BY v.venue_id, v.name
                    ORDER BY v.name""")) {
                statement.setObject(1, endsAt);
                statement.setObject(2, startsAt);
                statement.setObject(3, endsAt);
                statement.setObject(4, startsAt);
                try (var result = statement.executeQuery()) {
                    List<VenueUtilization> values = new ArrayList<>();
                    while (result.next()) {
                        double bookedHours = result.getDouble(4);
                        values.add(new VenueUtilization(result.getObject(1, java.util.UUID.class),
                                result.getString(2), result.getLong(3), bookedHours,
                                bookedHours / windowHours * 100.0));
                    }
                    return values;
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not load venue utilization.", exception);
            }
        });
    }
}
