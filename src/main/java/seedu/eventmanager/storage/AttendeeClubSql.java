package seedu.eventmanager.storage;

/** Shared read-only join. Cast the UUID side: legacy event IDs need not be valid UUIDs. */
final class AttendeeClubSql {
    private AttendeeClubSql() { }

    static final String JOIN = " LEFT JOIN organizer_club c ON c.id::text=e.club_id ";
}
