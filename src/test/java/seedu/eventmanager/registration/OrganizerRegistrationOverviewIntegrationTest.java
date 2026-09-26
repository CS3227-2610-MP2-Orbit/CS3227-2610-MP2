package seedu.eventmanager.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.AccessDeniedException;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.event.EventService;
import seedu.eventmanager.event.JdbcEventRepository;
import seedu.eventmanager.event.OrganizerIdentity;
import seedu.eventmanager.event.RegistrationOverview;
import seedu.eventmanager.event.RegistrationOverviewService;
import seedu.eventmanager.storage.DatabaseConfig;
import seedu.eventmanager.storage.DriverManagerDataSource;
import seedu.eventmanager.storage.JdbcEventRegistrations;

/** Organizer View registrations wired exactly as the desktop app does, against real PostgreSQL rows. */
class OrganizerRegistrationOverviewIntegrationTest extends RegistrationDatabaseTest {
    private static final OrganizerIdentity OWNER = new OrganizerIdentity("test-organizer", Set.of("test-club"));

    private RegistrationOverviewService overviews;

    @BeforeEach
    void wireOrganizerService() {
        EventService events = new EventService(
                new JdbcEventRepository(new DriverManagerDataSource(new DatabaseConfig(
                        configuration.url(), configuration.username(), configuration.password()))),
                UUID::randomUUID,
                Clock.systemUTC());
        overviews = new RegistrationOverviewService(events, new JdbcEventRegistrations(database));
    }

    @Test
    void ownerSeesCurrentRegistrantsSortedWithCapacity() throws Exception {
        UUID event = event("PUBLISHED", 30, NOW.plusSeconds(3600));
        UUID zoe = account("zoe", Role.ATTENDEE);
        UUID amy = account("Amy", Role.ATTENDEE);
        UUID ben = account("ben", Role.ATTENDEE);
        registration(event, zoe, "CONFIRMED");
        registration(event, amy, "CHECKED_IN");
        registration(event, ben, "CANCELLED");

        RegistrationOverview overview = overviews.overview(OWNER, event);

        assertEquals(30, overview.capacity());
        assertEquals(2, overview.registeredCount());
        assertEquals(List.of(new RegisteredAttendee(amy, "Amy"), new RegisteredAttendee(zoe, "zoe")),
                overview.attendees());
    }

    @Test
    void eventWithoutRegistrationsShowsEmptyRoster() throws Exception {
        UUID event = event("DRAFT", 10, NOW.plusSeconds(3600));

        RegistrationOverview overview = overviews.overview(OWNER, event);

        assertEquals(0, overview.registeredCount());
        assertTrue(overview.attendees().isEmpty());
    }

    @Test
    void organizerWhoDoesNotOwnTheClubCannotReadRoster() throws Exception {
        UUID event = event("PUBLISHED", 10, NOW.plusSeconds(3600));
        registration(event, account("private", Role.ATTENDEE), "CONFIRMED");
        OrganizerIdentity stranger = new OrganizerIdentity("other-organizer", Set.of("other-club"));

        assertThrows(AccessDeniedException.class, () -> overviews.overview(stranger, event));
    }
}
