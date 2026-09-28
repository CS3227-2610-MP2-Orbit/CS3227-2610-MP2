package seedu.eventmanager.storage;

import java.util.Optional;
import java.util.UUID;
import java.util.Objects;
import java.sql.SQLException;
import seedu.eventmanager.attendee.AttendeeEventDetails;
import seedu.eventmanager.attendee.AttendeeEventDetailsRepository;
import seedu.eventmanager.attendee.CatalogueEvent;
import seedu.eventmanager.registration.Registration;
import seedu.eventmanager.registration.RegistrationEligibilityPolicy.Booking;

/** Read-only personalized snapshot over the canonical event/venue/registration tables. */
public final class JdbcAttendeeEventDetailsRepository implements AttendeeEventDetailsRepository {
    private static final String QUERY = """
            SELECT e.id,e.club_id,e.title,e.description,e.starts_at,e.ends_at,e.capacity,e.status,
                   v.name AS venue_name,v.location,v.status AS venue_status,b.status AS booking_status,
                   own.status AS own_status,
                   (%s) AS occupied_seats,
                   CASE WHEN %s THEN CASE WHEN %s THEN 'CONFIRMED_ACTIVE'
                        ELSE 'VENUE_INACTIVE' END ELSE 'UNCONFIRMED' END AS booking_eligibility
            FROM organizer_event e
            JOIN users viewer ON viewer.user_id=? AND viewer.active=TRUE AND viewer.role='ATTENDEE'
            LEFT JOIN venue_bookings b ON b.event_id=e.id AND b.status IN ('CONFIRMED','AT_RISK')
            LEFT JOIN venues v ON v.venue_id=b.venue_id
            LEFT JOIN event_registrations own ON own.event_id=e.id AND own.attendee_id=viewer.user_id
            WHERE e.id=? AND e.status='PUBLISHED'
            """.formatted(RegistrationReadSql.occupiedSeats("e.id"),
                    RegistrationReadSql.matchingConfirmedBooking("e.starts_at", "e.ends_at"),
                    RegistrationReadSql.ACTIVE_VENUE);
    private final JdbcDatabase database;

    public JdbcAttendeeEventDetailsRepository(JdbcDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public Optional<Snapshot> find(UUID eventId, UUID attendeeId) {
        // The partial booking unique index and event/attendee key keep each join one-to-one.
        // All facts use one PostgreSQL statement snapshot, without acquiring write locks.
        return database.withConnection(c -> {
            try (var p = c.prepareStatement(QUERY)) {
                p.setObject(1, attendeeId); p.setObject(2, eventId); p.setQueryTimeout(15);
                try (var r = p.executeQuery()) {
                    if (!r.next()) return Optional.empty();
                    var event = new CatalogueEvent(r.getObject("id", UUID.class), r.getString("club_id"),
                            r.getString("title"), r.getString("description"), r.getTimestamp("starts_at").toInstant(),
                            r.getTimestamp("ends_at").toInstant(), r.getInt("capacity"));
                    var venue = r.getString("venue_name") == null ? Optional.<AttendeeEventDetails.Venue>empty()
                            : Optional.of(new AttendeeEventDetails.Venue(r.getString("venue_name"), r.getString("location"),
                                    r.getString("booking_status"), r.getString("venue_status")));
                    return Optional.of(new Snapshot(event, r.getString("status"), venue,
                            Booking.valueOf(r.getString("booking_eligibility")), r.getInt("occupied_seats"),
                            Optional.ofNullable(r.getString("own_status")).map(Registration.Status::valueOf)));
                }
            } catch (SQLException failure) {
                throw new IllegalStateException("Unable to load attendee event details.", failure);
            }
        });
    }
}
