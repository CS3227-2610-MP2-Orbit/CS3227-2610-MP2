package seedu.eventmanager.storage;

/** Shared SQL predicates for locked commands and non-locking detail snapshots.
 * Expressions are developer-owned SQL, never user input; values remain bound parameters.
 */
final class RegistrationReadSql {
    private RegistrationReadSql() { }

    static String occupiedSeats(String eventExpression) {
        // Deactivation never implicitly frees a reserved seat. Do not join users here.
        return "SELECT COUNT(*) FROM event_registrations r WHERE r.event_id=" + eventExpression
                + " AND r.status IN ('CONFIRMED','CHECKED_IN')";
    }

    static String matchingConfirmedBooking(String startExpression, String endExpression) {
        return "b.status='CONFIRMED' AND b.starts_at=" + startExpression + " AND b.ends_at=" + endExpression;
    }

    static final String ACTIVE_VENUE = "v.status='ACTIVE'";
}
