package seedu.eventmanager.registration;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.storage.JdbcEventRegistrations;
import seedu.eventmanager.storage.RegistrationDatabaseMigration;

class JdbcEventRegistrationsIntegrationTest extends RegistrationDatabaseTest {
    @Test
    void returnsOnlyCurrentActiveAttendeesForRequestedEvent() throws Exception {
        UUID event = event("PUBLISHED", 10, NOW.plusSeconds(3600));
        UUID other = event("PUBLISHED", 10, NOW.plusSeconds(3600));
        UUID confirmed = account("confirmed", Role.ATTENDEE);
        UUID checked = account("checked", Role.ATTENDEE);
        UUID cancelled = account("cancelled", Role.ATTENDEE);
        UUID inactive = account("inactive", Role.ATTENDEE);
        UUID organizer = account("organizer", Role.CLUB_ORGANIZER);
        registration(event, confirmed, "CONFIRMED");
        registration(event, checked, "CHECKED_IN");
        registration(event, cancelled, "CANCELLED");
        registration(event, inactive, "CONFIRMED");
        registration(event, organizer, "CONFIRMED");
        registration(other, account("other-event", Role.ATTENDEE), "CONFIRMED");
        sql("UPDATE users SET active=false WHERE username='inactive'");
        assertEquals(Set.of(new RegisteredAttendee(confirmed, "confirmed"),
                new RegisteredAttendee(checked, "checked")),
                Set.copyOf(new JdbcEventRegistrations(database).registeredAttendees(event)));
    }

    @Test
    void reRegistrationAndUsernameChangesDoNotDuplicateOrCacheResults() throws Exception {
        UUID event = event("PUBLISHED", 10, NOW.plusSeconds(3600));
        UUID attendee = account("before", Role.ATTENDEE);
        registration(event, attendee, "CANCELLED");
        var reader = new JdbcEventRegistrations(database);
        assertTrue(reader.registeredAttendees(event).isEmpty());
        sql("UPDATE event_registrations SET status='CONFIRMED',cancelled_at=NULL,version=version+1");
        sql("UPDATE users SET username='after' WHERE username='before'");
        assertEquals(List.of(new RegisteredAttendee(attendee, "after")), reader.registeredAttendees(event));
        assertEquals(new RegisteredAttendee(attendee, "after"),
                reader.findRegisteredAttendee(event, attendee).orElseThrow());
        assertTrue(reader.findRegisteredAttendee(event, UUID.randomUUID()).isEmpty());
        assertTrue(reader.registeredAttendees(UUID.randomUUID()).isEmpty());
        assertThrows(SQLException.class, () -> registration(event, attendee, "CONFIRMED"));
    }

    @Test
    void deactivationHidesWithoutDeletingAndReactivationRestores() throws Exception {
        UUID event = event("COMPLETED", 10, NOW.minusSeconds(86400));
        UUID attendee = account("history", Role.ATTENDEE);
        registration(event, attendee, "CHECKED_IN");
        var reader = new JdbcEventRegistrations(database);
        assertEquals(1, reader.registeredAttendees(event).size());
        sql("UPDATE users SET active=false WHERE username='history'");
        assertTrue(reader.registeredAttendees(event).isEmpty());
        sql("UPDATE users SET active=true WHERE username='history'");
        assertEquals(List.of(new RegisteredAttendee(attendee, "history")), reader.registeredAttendees(event));
    }

    @Test
    void migrationIsRepeatableAndKeepsExistingRecordsAndDtoHasOnlyTwoFields() throws Exception {
        UUID event = event("PUBLISHED", 10, NOW.plusSeconds(3600));
        UUID attendee = account("retained", Role.ATTENDEE);
        registration(event, attendee, "CONFIRMED");
        RegistrationDatabaseMigration.migrate(configuration);
        assertEquals(List.of(new RegisteredAttendee(attendee, "retained")),
                new JdbcEventRegistrations(database).registeredAttendees(event));
        assertEquals(List.of("attendeeId", "displayName"), java.util.Arrays.stream(
                RegisteredAttendee.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).toList());
    }
}
