package seedu.eventmanager.registration;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import seedu.eventmanager.attendee.*;
import seedu.eventmanager.common.*;
import seedu.eventmanager.storage.*;

/** Real PostgreSQL previews and commands with a controlled clock, in disposable per-test schemas. */
class CheckInPreviewIntegrationTest extends RegistrationDatabaseTest {
    static Stream<CheckInPreviewParityTest.Case> cases() { return CheckInPreviewParityTest.cases(); }

    @ParameterizedTest @MethodSource("cases")
    void jdbcPreviewsAgreeWithTheCommand(CheckInPreviewParityTest.Case scenario) throws Exception {
        var owner = account("alice", Role.ATTENDEE);
        var event = event(scenario.eventStatus(), 20, CheckInPreviewParityTest.START);
        // Base fixture has a two-hour event; align the explicit one-hour boundary under test.
        sql("UPDATE organizer_event SET ends_at=starts_at+INTERVAL '1 hour'");
        booking(event);
        if (!scenario.booking()) sql("UPDATE venues SET status='MAINTENANCE'");
        if (scenario.status() != null) registration(event, owner, scenario.status().name());
        var token = login("alice");
        var clock = Clock.fixed(scenario.now(), ZoneOffset.UTC);
        var commands = RegistrationServiceFactory.create(configuration, clock);
        var details = new AttendeeEventDetailsService(new JdbcAttendeeEventDetailsRepository(database), sessions::resolve, clock);
        if (scenario.eventStatus().equals("PUBLISHED") && scenario.now().isBefore(CheckInPreviewParityTest.END)) {
            assertEquals(scenario.expected(), details.getEvent(token, event).checkInAvailability());
        } else {
            assertThrows(EntityNotFoundException.class, () -> details.getEvent(token, event));
        }
        var rows = new MyRegistrationsService(commands::myRegistrations,
                new JdbcRegistrationEventInfoRepository(database), sessions::resolve, clock).list(token);
        if (scenario.status() == null) assertTrue(rows.isEmpty());
        else assertEquals(scenario.expected(), rows.getFirst().checkInAvailability());
        assertEquals(0, count("audit_logs"), "Preview must not write audits");
        if (scenario.code() == null) {
            assertEquals(Registration.Status.CHECKED_IN, commands.checkIn(token, event, 0).status());
            assertEquals(1, count("audit_logs"));
        } else {
            RegistrationServiceTest.code(scenario.code(), () -> commands.checkIn(token, event,
                    scenario.status() == null ? -1 : 0));
            assertEquals(0, count("audit_logs"));
            if (scenario.status() != null) assertEquals(scenario.status(), commands.myRegistrations(token).getFirst().status());
        }
        assertEquals(0, count("notification_outbox"));
    }
}
