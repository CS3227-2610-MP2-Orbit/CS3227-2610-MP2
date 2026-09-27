package seedu.eventmanager.storage;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.Objects;
import java.sql.SQLException;
import seedu.eventmanager.attendee.RegistrationEventInfoRepository;

/** Batch event/venue metadata for authorized registrations, without a public-catalogue filter. */
public final class JdbcRegistrationEventInfoRepository implements RegistrationEventInfoRepository {
    private static final String QUERY = """
            SELECT e.id,e.title,e.starts_at,e.ends_at,e.club_id,e.description,e.status,v.name,v.location,
                   EXISTS (SELECT 1 FROM venue_bookings b JOIN venues v ON v.venue_id=b.venue_id
                           WHERE b.event_id=e.id AND %s AND %s) AS confirmed_active_booking
            FROM organizer_event e
            LEFT JOIN LATERAL (%s) booking ON TRUE
            LEFT JOIN venues v ON v.venue_id=booking.venue_id
            WHERE e.id=ANY(?)
            """.formatted(RegistrationReadSql.matchingConfirmedBooking("e.starts_at", "e.ends_at"),
                    RegistrationReadSql.ACTIVE_VENUE, RegistrationReadSql.displayBooking("e.id"));
    private final JdbcDatabase database;

    public JdbcRegistrationEventInfoRepository(JdbcDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override public Map<UUID, EventInfo> findAll(Set<UUID> eventIds) {
        if (eventIds.isEmpty()) return Map.of();
        return database.withConnection(c -> {
            try (var p = c.prepareStatement(QUERY)) {
                var ids = c.createArrayOf("uuid", eventIds.toArray(UUID[]::new));
                try {
                    p.setArray(1, ids); p.setQueryTimeout(15);
                    try (var r = p.executeQuery()) {
                        Map<UUID, EventInfo> result = new HashMap<>();
                        while (r.next()) {
                            String venue = r.getString("name");
                            result.put(r.getObject("id", UUID.class), new EventInfo(r.getString("title"),
                                    r.getTimestamp("starts_at").toInstant(),
                                    venue == null ? "" : venue + " · " + r.getString("location"),
                                    r.getTimestamp("ends_at").toInstant(), r.getString("club_id"),
                                    r.getString("description"), r.getString("status"), r.getBoolean("confirmed_active_booking")));
                        }
                        return Map.copyOf(result);
                    }
                } finally {
                    ids.free();
                }
            } catch (SQLException failure) {
                throw new IllegalStateException("Unable to load registered event information.", failure);
            }
        });
    }
}
