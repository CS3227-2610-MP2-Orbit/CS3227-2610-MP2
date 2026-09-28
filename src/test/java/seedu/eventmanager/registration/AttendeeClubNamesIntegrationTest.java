package seedu.eventmanager.registration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.attendee.*;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.storage.*;

/** Real joins in isolated schemas; no Organizer writes or migrations added by the feature. */
class AttendeeClubNamesIntegrationTest extends RegistrationDatabaseTest {
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private EventCatalogueService catalogue() {
        return new EventCatalogueService(new JdbcEventCatalogueRepository(
                new DriverManagerDataSource(new DatabaseConfig(configuration.url(), user, password))), clock);
    }

    private void assertNames(String token, UUID event, String expected) {
        assertEquals(expected, catalogue().search(CatalogueQuery.all()).getFirst().clubName());
        assertEquals(expected, catalogue().getEvent(event).clubName());
        assertEquals(expected, new AttendeeEventDetailsService(new JdbcAttendeeEventDetailsRepository(database),
                sessions::resolve, clock).getEvent(token, event).event().clubName());
        var commands = RegistrationServiceFactory.create(configuration, clock);
        assertEquals(expected, new MyRegistrationsService(commands::myRegistrations,
                new JdbcRegistrationEventInfoRepository(database), sessions::resolve, clock)
                .list(token).getFirst().clubName());
        assertEquals(expected, new AttendanceHistoryService(new JdbcAttendanceHistoryRepository(database),
                sessions::resolve).list(token).getFirst().clubName());
    }

    @Test void resolvesNamesAcrossAllAttendeeReadsWithoutChangingIdentityOrWriting() throws Exception {
        UUID attendee = account("alice", Role.ATTENDEE);
        UUID event = event("PUBLISHED", 10, NOW);
        UUID club = UUID.randomUUID();
        sql("INSERT INTO organizer_club VALUES ('" + club + "','Campus Arts','" + UUID.randomUUID() + "',now())");
        sql("UPDATE organizer_event SET club_id='" + club + "'");
        registration(event, attendee, "CHECKED_IN");
        String token = login("alice");
        assertNames(token, event, "Campus Arts");
        assertEquals(List.of(event), catalogue().search(new CatalogueQuery("", club.toString(), null, null))
                .stream().map(CatalogueEvent::id).toList());
        assertTrue(catalogue().search(new CatalogueQuery("", "Campus Arts", null, null)).isEmpty());
        sql("UPDATE organizer_club SET name='Renamed Arts'");
        assertNames(token, event, "Renamed Arts");
        assertEquals(1, count("event_registrations"));
        assertEquals(0, count("audit_logs"));
        assertEquals(0, count("notification_outbox"));
        assertEquals(0, count("organizer_club_audit_record"));
    }

    @Test void unmatchedUuidAndNonUuidLegacyIdsRemainVisibleWithNeutralFallback() throws Exception {
        UUID attendee = account("alice", Role.ATTENDEE);
        UUID event = event("PUBLISHED", 10, NOW);
        registration(event, attendee, "CHECKED_IN");
        String token = login("alice");
        assertNames(token, event, "Unknown club");
        sql("UPDATE organizer_event SET club_id='" + UUID.randomUUID() + "'");
        assertNames(token, event, "Unknown club");
    }

    @Test void dropdownListsAllSharedClubsByNameEvenWithoutPublishedEvents() throws Exception {
        assertTrue(catalogue().clubs().isEmpty());
        UUID first = UUID.randomUUID();
        UUID last = UUID.randomUUID();
        sql("INSERT INTO organizer_club VALUES ('" + last + "','Zulu Club','" + UUID.randomUUID() + "',now()),"
                + "('" + first + "','alpha Club','" + UUID.randomUUID() + "',now())");
        assertEquals(List.of(new CatalogueClub(first.toString(), "alpha Club"),
                new CatalogueClub(last.toString(), "Zulu Club")), catalogue().clubs());
        assertTrue(catalogue().search(CatalogueQuery.all()).isEmpty());
        assertEquals(0, count("organizer_club_audit_record"));
    }
}
