package seedu.eventmanager.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.DriverManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import seedu.eventmanager.announcement.AnnouncementService;
import seedu.eventmanager.announcement.JdbcAnnouncementRepository;
import seedu.eventmanager.attendee.AttendanceHistoryService;
import seedu.eventmanager.attendee.CatalogueEvent;
import seedu.eventmanager.attendee.CatalogueQuery;
import seedu.eventmanager.attendee.EventCatalogueService;
import seedu.eventmanager.attendee.InboxMessage;
import seedu.eventmanager.attendee.InboxNotificationDelivery;
import seedu.eventmanager.attendee.InboxService;
import seedu.eventmanager.club.ClubService;
import seedu.eventmanager.club.JdbcClubRepository;
import seedu.eventmanager.common.AccessDeniedException;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.common.EntityNotFoundException;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.event.Event;
import seedu.eventmanager.event.EventDetails;
import seedu.eventmanager.event.EventService;
import seedu.eventmanager.event.EventStatus;
import seedu.eventmanager.event.JdbcEventRepository;
import seedu.eventmanager.event.OrganizerIdentity;
import seedu.eventmanager.event.OrganizerVenueRequestService;
import seedu.eventmanager.event.RegistrationOverviewService;
import seedu.eventmanager.notification.NotificationOutboxWorker;
import seedu.eventmanager.registration.Registration;
import seedu.eventmanager.registration.RegistrationService;
import seedu.eventmanager.registration.RegistrationServiceFactory;
import seedu.eventmanager.service.VenueAdministratorService;
import seedu.eventmanager.service.VenueAdministratorServiceFactory;
import seedu.eventmanager.storage.DatabaseConfig;
import seedu.eventmanager.storage.DatabaseConfiguration;
import seedu.eventmanager.storage.DriverManagerDataSource;
import seedu.eventmanager.storage.InboxDatabaseMigration;
import seedu.eventmanager.storage.JdbcAttendanceHistoryRepository;
import seedu.eventmanager.storage.JdbcAuthorizationService;
import seedu.eventmanager.storage.JdbcDatabase;
import seedu.eventmanager.storage.JdbcDraftEventDeletion;
import seedu.eventmanager.storage.JdbcEventBookingCheck;
import seedu.eventmanager.storage.JdbcEventCatalogueRepository;
import seedu.eventmanager.storage.JdbcEventRegistrations;
import seedu.eventmanager.storage.JdbcInboxRepository;
import seedu.eventmanager.storage.JdbcLocalSessionService;
import seedu.eventmanager.storage.JdbcNotificationOutboxRepository;
import seedu.eventmanager.storage.JdbcNotificationService;
import seedu.eventmanager.storage.JdbcTransactionManager;
import seedu.eventmanager.storage.JdbcVenueRelease;
import seedu.eventmanager.storage.JdbcVenueRepository;
import seedu.eventmanager.storage.JdbcVenueRequestRepository;
import seedu.eventmanager.storage.PasswordHasher;
import seedu.eventmanager.venue.Venue;
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;
import seedu.eventmanager.venue.VenueStatus;
import seedu.eventmanager.volunteer.JdbcVolunteerRepository;
import seedu.eventmanager.volunteer.VolunteerService;

/**
 * Whole-product integration: Club Organizer, Venue Administrator and Attendee workflows wired to the
 * real JDBC implementations in one isolated PostgreSQL schema (dropped after each test). Only the
 * clocks are controlled; no service or repository is faked. This is not a JavaFX/UI test.
 */
class CrossRoleWorkflowIntegrationTest {
    private static final Instant BEFORE_EVENT = Instant.parse("2030-01-01T00:00:00Z");
    private static final Instant STARTS = Instant.parse("2030-01-10T10:00:00Z");
    private static final Instant ENDS = STARTS.plus(Duration.ofHours(2));
    private static final Instant DURING_EVENT = STARTS.plus(Duration.ofMinutes(30));
    private static final String PASSWORD = "synthetic-test-account";

    private String baseUrl;
    private String user;
    private String password;
    private String schema;
    private DatabaseConfiguration configuration;
    private JdbcDatabase database;
    private DataSource dataSource;
    private JdbcLocalSessionService sessions;

    private ClubService clubs;
    private EventService events;
    private OrganizerVenueRequestService organizerVenues;
    private RegistrationOverviewService overview;
    private VolunteerService volunteers;
    private AnnouncementService announcements;
    private VenueAdministratorService venueAdmin;
    private EventCatalogueService catalogue;
    private InboxService inbox;
    private NotificationOutboxWorker inboxWorker;
    private AttendanceHistoryService history;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = System.getenv("EVENT_MANAGER_TEST_DB_URL");
        assumeTrue(baseUrl != null && !baseUrl.isBlank(), "Disposable test database not configured");
        user = System.getenv().getOrDefault("EVENT_MANAGER_TEST_DB_USER", "event_manager");
        password = System.getenv().getOrDefault("EVENT_MANAGER_TEST_DB_PASSWORD", "");
        String candidate = "cross_role_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(baseUrl, user, password);
                var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + candidate);
        }
        schema = candidate;
        configuration = new DatabaseConfiguration(
                baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema, user, password);
        InboxDatabaseMigration.migrate(configuration);
        database = new JdbcDatabase(configuration);
        dataSource = new DriverManagerDataSource(
                new DatabaseConfig(configuration.url(), configuration.username(), configuration.password()));
        sessions = new JdbcLocalSessionService(database, new PasswordHasher());

        Clock clock = Clock.fixed(BEFORE_EVENT, ZoneOffset.UTC);
        JdbcVenueRequestRepository venueRequests = new JdbcVenueRequestRepository(database);
        JdbcEventRegistrations registrations = new JdbcEventRegistrations(database);
        clubs = new ClubService(new JdbcClubRepository(dataSource), UUID::randomUUID, clock);
        events = new EventService(new JdbcEventRepository(dataSource), UUID::randomUUID, clock, venueRequests,
                new JdbcEventBookingCheck(dataSource), new JdbcDraftEventDeletion(dataSource));
        organizerVenues = new OrganizerVenueRequestService(events, new JdbcVenueRepository(database), venueRequests,
                UUID::randomUUID, new JdbcVenueRelease(dataSource));
        overview = new RegistrationOverviewService(events, registrations);
        volunteers = new VolunteerService(events, registrations, new JdbcVolunteerRepository(dataSource), clock);
        announcements = new AnnouncementService(events, registrations, new JdbcAnnouncementRepository(dataSource),
                new JdbcNotificationService(database), UUID::randomUUID, clock);
        venueAdmin = VenueAdministratorServiceFactory.create(configuration, new JdbcAuthorizationService(database));
        catalogue = new EventCatalogueService(new JdbcEventCatalogueRepository(dataSource), clock);
        JdbcInboxRepository inboxRepository = new JdbcInboxRepository(database);
        inbox = new InboxService(inboxRepository, sessions::resolve, new JdbcTransactionManager(database), clock);
        inboxWorker = new NotificationOutboxWorker(
                new JdbcNotificationOutboxRepository(database, InboxNotificationDelivery.EVENT_TYPES),
                new InboxNotificationDelivery(inboxRepository::deliver));
        history = new AttendanceHistoryService(new JdbcAttendanceHistoryRepository(database), sessions::resolve);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (schema != null) {
            try (var connection = DriverManager.getConnection(baseUrl, user, password);
                    var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    @Test
    void fullLifecycle_organizerAdminAndAttendeeWorkTogether() throws Exception {
        OrganizerIdentity organizer = organizerWithClub("org_amy", "Volleyball");
        Actor admin = account("venue_admin_it", Role.VENUE_ADMINISTRATOR);
        UUID attendeeId = account("attendee_ben", Role.ATTENDEE).userId();
        String attendee = login("attendee_ben");
        UUID venue = activeVenue("LT1", 120);

        Event draft = events.createEvent(organizer, club(organizer), details("Spike training", 80));
        assertTrue(visibleEventIds().isEmpty(), "drafts are hidden from attendees");
        assertEquals("EVENT_NOT_REGISTERABLE", code(() -> commandsAt(BEFORE_EVENT).register(attendee, draft.id(), -1)));
        assertThrows(ValidationException.class, () -> events.publishEvent(organizer, draft.id(), 0));

        VenueRequest request = organizerVenues.submit(organizer, draft.id(), venue);
        assertEquals(List.of(request.requestId()), venueRequestIdsWithStatus("SUBMITTED"));
        venueAdmin.approve(admin, request.requestId());
        assertEquals(VenueRequestStatus.APPROVED,
                organizerVenues.latestRequest(organizer, draft.id()).orElseThrow().status());

        Event published = events.publishEvent(organizer, draft.id(), draft.version());
        assertEquals(EventStatus.PUBLISHED, published.status());
        assertEquals(List.of(draft.id()), visibleEventIds());

        Registration registered = commandsAt(BEFORE_EVENT).register(attendee, draft.id(), -1);
        assertEquals(Registration.Status.CONFIRMED, registered.status());
        assertEquals(1, overview.overview(organizer, draft.id()).registeredCount());

        volunteers.assign(organizer, draft.id(), attendeeId, "Usher");
        assertEquals(1, volunteers.listVolunteers(organizer, draft.id()).size());
        assertEquals(1, announcements.post(organizer, draft.id(), "Doors open at 5:30pm").notificationsQueued());

        deliverInbox();
        List<String> bodies = inbox.list(attendee).messages().stream().map(InboxMessage::body).toList();
        assertTrue(bodies.contains("Doors open at 5:30pm"), bodies.toString());
        assertTrue(bodies.stream().anyMatch(body -> body.startsWith("Registration confirmed: Spike training")),
                bodies.toString());

        assertEquals("CHECK_IN_TOO_EARLY",
                code(() -> commandsAt(BEFORE_EVENT).checkIn(attendee, draft.id(), registered.version())));
        Registration checkedIn = commandsAt(DURING_EVENT).checkIn(attendee, draft.id(), registered.version());
        assertEquals(Registration.Status.CHECKED_IN, checkedIn.status());
        assertEquals(List.of(draft.id()), history.list(attendee).stream().map(r -> r.eventId()).toList());
        assertEquals(1, overview.overview(organizer, draft.id()).registeredCount(), "checked-in stays on roster");

        assertEquals(List.of("CREATE_EVENT", "PUBLISH_EVENT"),
                strings("SELECT action FROM organizer_event_audit_record ORDER BY resulting_version"));
        List<String> venueAndRegistrationAudit = strings("SELECT action FROM audit_logs ORDER BY created_at");
        assertTrue(venueAndRegistrationAudit.containsAll(
                List.of("VENUE_REQUEST_APPROVED", "REGISTRATION_CONFIRMED", "REGISTRATION_CHECKED_IN")),
                venueAndRegistrationAudit.toString());
    }

    @Test
    void releaseVenue_changeTimesAndResubmit_realAdminApprovesAgainAndEventPublishes() throws Exception {
        OrganizerIdentity organizer = organizerWithClub("org_amy", "Volleyball");
        Actor admin = account("venue_admin_it", Role.VENUE_ADMINISTRATOR);
        account("attendee_ben", Role.ATTENDEE);
        String attendee = login("attendee_ben");
        UUID venue = activeVenue("LT1", 120);
        Event draft = events.createEvent(organizer, club(organizer), details("Welcome tea", 80));
        venueAdmin.approve(admin, organizerVenues.submit(organizer, draft.id(), venue).requestId());

        EventDetails moved = new EventDetails("Welcome tea", "", STARTS.plus(Duration.ofDays(1)),
                ENDS.plus(Duration.ofDays(1)), 80);
        assertThrows(ValidationException.class, () -> events.editEvent(organizer, draft.id(), 0, moved));
        assertThrows(ValidationException.class, () -> organizerVenues.submit(organizer, draft.id(), venue));

        organizerVenues.releaseApprovedVenue(organizer, draft.id());
        assertEquals(VenueRequestStatus.WITHDRAWN,
                organizerVenues.latestRequest(organizer, draft.id()).orElseThrow().status());
        Event edited = events.editEvent(organizer, draft.id(), 0, moved).event();
        VenueRequest resubmitted = organizerVenues.submit(organizer, draft.id(), venue);
        venueAdmin.approve(admin, resubmitted.requestId());

        Event published = events.publishEvent(organizer, draft.id(), edited.version());
        assertEquals(moved.startsAt(), published.startsAt());
        assertEquals(Registration.Status.CONFIRMED,
                commandsAt(BEFORE_EVENT).register(attendee, draft.id(), -1).status());
        assertEquals(List.of("CANCELLED", "CONFIRMED"),
                strings("SELECT status FROM venue_bookings ORDER BY created_at"));
        assertTrue(strings("SELECT action FROM audit_logs").contains("VENUE_BOOKING_RELEASED"));
    }

    @Test
    void adminRejectionAndBookingConflict_keepEventsUnpublishable() throws Exception {
        OrganizerIdentity amy = organizerWithClub("org_amy", "Volleyball");
        OrganizerIdentity cal = organizerWithClub("org_cal", "Chess Club");
        Actor admin = account("venue_admin_it", Role.VENUE_ADMINISTRATOR);
        UUID venue = activeVenue("LT1", 120);
        Event amyEvent = events.createEvent(amy, club(amy), details("Spike training", 80));
        Event calEvent = events.createEvent(cal, club(cal), details("Chess night", 40));

        venueAdmin.approve(admin, organizerVenues.submit(amy, amyEvent.id(), venue).requestId());
        VenueRequest clash = organizerVenues.submit(cal, calEvent.id(), venue);
        assertEquals("BOOKING_CONFLICT", code(() -> venueAdmin.approve(admin, clash.requestId())));
        assertEquals(VenueRequestStatus.SUBMITTED,
                organizerVenues.latestRequest(cal, calEvent.id()).orElseThrow().status());

        venueAdmin.reject(admin, clash.requestId(), VenueAdministratorService.REASON_VENUE_BOOKED);
        assertEquals(VenueRequestStatus.REJECTED,
                organizerVenues.latestRequest(cal, calEvent.id()).orElseThrow().status());
        ValidationException refused = assertThrows(ValidationException.class,
                () -> events.publishEvent(cal, calEvent.id(), 0));
        assertTrue(refused.getMessage().contains("confirmed venue booking"), refused.getMessage());

        UUID otherVenue = activeVenue("LT2", 60);
        venueAdmin.approve(admin, organizerVenues.submit(cal, calEvent.id(), otherVenue).requestId());
        assertEquals(EventStatus.PUBLISHED, events.publishEvent(cal, calEvent.id(), 0).status());
    }

    @Test
    void organizerIsolation_otherOrganizerCannotTouchAnotherClubsEvent() throws Exception {
        OrganizerIdentity amy = organizerWithClub("org_amy", "Volleyball");
        OrganizerIdentity cal = organizerWithClub("org_cal", "Chess Club");
        Actor admin = account("venue_admin_it", Role.VENUE_ADMINISTRATOR);
        UUID venue = activeVenue("LT1", 120);
        Event event = events.createEvent(amy, club(amy), details("Spike training", 80));
        venueAdmin.approve(admin, organizerVenues.submit(amy, event.id(), venue).requestId());

        assertThrows(AccessDeniedException.class, () -> events.getEvent(cal, event.id()));
        assertThrows(AccessDeniedException.class, () -> events.publishEvent(cal, event.id(), 0));
        assertThrows(AccessDeniedException.class, () -> events.editEvent(cal, event.id(), 0, details("Hijack", 80)));
        assertThrows(AccessDeniedException.class, () -> organizerVenues.releaseApprovedVenue(cal, event.id()));
        assertThrows(AccessDeniedException.class, () -> overview.overview(cal, event.id()));
        assertThrows(AccessDeniedException.class, () -> announcements.post(cal, event.id(), "Spam"));
        assertTrue(events.listEvents(cal).isEmpty());

        assertEquals(EventStatus.DRAFT, events.getEvent(amy, event.id()).status());
        assertEquals(List.of("APPROVED"), strings("SELECT status FROM venue_requests"));
        assertEquals(List.of("CREATE_EVENT"), strings("SELECT action FROM organizer_event_audit_record"));
    }

    @Test
    void capacityAndCancellation_flowFromAttendeeToOrganizerRoster() throws Exception {
        OrganizerIdentity organizer = organizerWithClub("org_amy", "Volleyball");
        Actor admin = account("venue_admin_it", Role.VENUE_ADMINISTRATOR);
        account("attendee_ben", Role.ATTENDEE);
        account("attendee_chloe", Role.ATTENDEE);
        String ben = login("attendee_ben");
        String chloe = login("attendee_chloe");
        Event event = publishedEvent(organizer, admin, "Small workshop", 1);

        Registration benSeat = commandsAt(BEFORE_EVENT).register(ben, event.id(), -1);
        assertEquals("EVENT_FULL", code(() -> commandsAt(BEFORE_EVENT).register(chloe, event.id(), -1)));
        assertEquals(List.of("attendee_ben"), rosterNames(organizer, event.id()));

        commandsAt(BEFORE_EVENT).cancel(ben, event.id(), benSeat.version());
        assertTrue(rosterNames(organizer, event.id()).isEmpty());
        commandsAt(BEFORE_EVENT).register(chloe, event.id(), -1);
        assertEquals(List.of("attendee_chloe"), rosterNames(organizer, event.id()));
        assertThrows(ApplicationException.class,
                () -> commandsAt(DURING_EVENT).register(ben, event.id(), benSeat.version() + 1),
                "registration closes once the event starts");

        deliverInbox();
        assertTrue(inbox.list(ben).messages().stream()
                .anyMatch(message -> message.body().startsWith("Registration cancelled: Small workshop")));
    }

    @Test
    void venueDeactivatedAfterPublish_eventStaysListedButRegistrationIsRefused() throws Exception {
        OrganizerIdentity organizer = organizerWithClub("org_amy", "Volleyball");
        Actor admin = account("venue_admin_it", Role.VENUE_ADMINISTRATOR);
        account("attendee_ben", Role.ATTENDEE);
        String ben = login("attendee_ben");
        Event event = publishedEvent(organizer, admin, "Spike training", 80);

        sql("UPDATE venues SET status='INACTIVE'");

        assertEquals(List.of(event.id()), visibleEventIds());
        assertEquals("VENUE_NOT_CONFIRMED", code(() -> commandsAt(BEFORE_EVENT).register(ben, event.id(), -1)));
        assertThrows(ValidationException.class, () -> organizerVenues.releaseApprovedVenue(organizer, event.id()),
                "published events keep their booking");
    }

    @Test
    void secondRequestWhileBooked_refusedUntilReleased_soAdminNeverHitsDuplicateBooking() throws Exception {
        OrganizerIdentity organizer = organizerWithClub("org_amy", "Volleyball");
        Actor admin = account("venue_admin_it", Role.VENUE_ADMINISTRATOR);
        Event draft = events.createEvent(organizer, club(organizer), details("Welcome tea", 80));
        venueAdmin.approve(admin, organizerVenues.submit(organizer, draft.id(), activeVenue("LT1", 120)).requestId());
        UUID otherVenue = activeVenue("LT2", 120);

        assertThrows(ValidationException.class, () -> organizerVenues.submit(organizer, draft.id(), otherVenue));
        assertEquals(List.of("APPROVED"), strings("SELECT status FROM venue_requests"));

        organizerVenues.releaseApprovedVenue(organizer, draft.id());
        venueAdmin.approve(admin, organizerVenues.submit(organizer, draft.id(), otherVenue).requestId());
        assertEquals(List.of(otherVenue.toString()),
                strings("SELECT venue_id FROM venue_bookings WHERE status='CONFIRMED'"));
        assertEquals(EventStatus.PUBLISHED, events.publishEvent(organizer, draft.id(), 0).status());
    }

    @Test
    void deleteDraftWithApprovedVenue_freesSlotForAnotherClubAndHidesEvent() throws Exception {
        OrganizerIdentity amy = organizerWithClub("org_amy", "Volleyball");
        OrganizerIdentity cal = organizerWithClub("org_cal", "Chess Club");
        Actor admin = account("venue_admin_it", Role.VENUE_ADMINISTRATOR);
        UUID venue = activeVenue("LT1", 120);
        Event amyEvent = events.createEvent(amy, club(amy), details("Spike training", 80));
        venueAdmin.approve(admin, organizerVenues.submit(amy, amyEvent.id(), venue).requestId());
        Event calEvent = events.createEvent(cal, club(cal), details("Chess night", 40));
        VenueRequest calRequest = organizerVenues.submit(cal, calEvent.id(), venue);
        assertEquals("BOOKING_CONFLICT", code(() -> venueAdmin.approve(admin, calRequest.requestId())));

        assertThrows(AccessDeniedException.class, () -> events.deleteEvent(cal, amyEvent.id(), 0));
        events.deleteEvent(amy, amyEvent.id(), 0);

        assertTrue(events.listEvents(amy).isEmpty());
        assertThrows(EntityNotFoundException.class, () -> organizerVenues.latestRequest(amy, amyEvent.id()));
        assertThrows(EntityNotFoundException.class, () -> announcements.post(amy, amyEvent.id(), "Hello"));
        venueAdmin.approve(admin, calRequest.requestId());
        assertEquals(EventStatus.PUBLISHED, events.publishEvent(cal, calEvent.id(), 0).status());
        assertEquals(List.of(calEvent.id()), visibleEventIds());
        assertTrue(strings("SELECT action FROM organizer_event_audit_record").contains("DELETE_EVENT"));
        assertTrue(strings("SELECT action FROM audit_logs").contains("VENUE_BOOKING_RELEASED"));
    }

    @Test
    void deleteDraftWithPendingRequest_adminCanNoLongerApproveIt() throws Exception {
        OrganizerIdentity organizer = organizerWithClub("org_amy", "Volleyball");
        Actor admin = account("venue_admin_it", Role.VENUE_ADMINISTRATOR);
        Event draft = events.createEvent(organizer, club(organizer), details("Welcome tea", 80));
        VenueRequest pending = organizerVenues.submit(organizer, draft.id(), activeVenue("LT1", 120));

        events.deleteEvent(organizer, draft.id(), 0);

        assertEquals("INVALID_STATE", code(() -> venueAdmin.approve(admin, pending.requestId())));
        assertTrue(strings("SELECT booking_id FROM venue_bookings").isEmpty());
        assertEquals(List.of("WITHDRAWN"), strings("SELECT status FROM venue_requests"));
    }

    @Test
    void deletePublishedEvent_refusedAndAttendeesCanStillRegister() throws Exception {
        OrganizerIdentity organizer = organizerWithClub("org_amy", "Volleyball");
        Actor admin = account("venue_admin_it", Role.VENUE_ADMINISTRATOR);
        account("attendee_ben", Role.ATTENDEE);
        Event published = publishedEvent(organizer, admin, "Spike training", 80);

        assertThrows(ValidationException.class, () -> events.deleteEvent(organizer, published.id(), 1));

        assertEquals(List.of(published.id()), visibleEventIds());
        assertEquals(Registration.Status.CONFIRMED,
                commandsAt(BEFORE_EVENT).register(login("attendee_ben"), published.id(), -1).status());
        assertEquals(List.of("CONFIRMED"), strings("SELECT status FROM venue_bookings"));
    }

    private Event publishedEvent(OrganizerIdentity organizer, Actor admin, String title, int capacity)
            throws Exception {
        UUID venue = activeVenue("Hall " + title, 200);
        Event draft = events.createEvent(organizer, club(organizer), details(title, capacity));
        venueAdmin.approve(admin, organizerVenues.submit(organizer, draft.id(), venue).requestId());
        return events.publishEvent(organizer, draft.id(), draft.version());
    }

    private OrganizerIdentity organizerWithClub(String username, String clubName) {
        account(username, Role.CLUB_ORGANIZER);
        Actor actor = sessions.login(username, PASSWORD).actor();
        clubs.createClub(actor, clubName);
        return clubs.identityFor(actor);
    }

    private static String club(OrganizerIdentity organizer) {
        return organizer.ownedClubIds().iterator().next();
    }

    private static EventDetails details(String title, int capacity) {
        return new EventDetails(title, "", STARTS, ENDS, capacity);
    }

    private Actor account(String username, Role role) {
        return new Actor(UUID.fromString(sessions.createUser(username, PASSWORD, role)), role);
    }

    private String login(String username) {
        return sessions.login(username, PASSWORD).token();
    }

    private UUID activeVenue(String name, int capacity) {
        UUID id = UUID.randomUUID();
        new JdbcVenueRepository(database).save(new Venue(id, name, "Level 1", capacity, "", VenueStatus.ACTIVE));
        return id;
    }

    private RegistrationService commandsAt(Instant now) {
        return RegistrationServiceFactory.create(configuration, Clock.fixed(now, ZoneOffset.UTC));
    }

    private List<UUID> visibleEventIds() {
        return catalogue.search(CatalogueQuery.all()).stream().map(CatalogueEvent::id).toList();
    }

    private List<String> rosterNames(OrganizerIdentity organizer, UUID eventId) {
        return overview.overview(organizer, eventId).attendees().stream().map(a -> a.displayName()).toList();
    }

    private List<UUID> venueRequestIdsWithStatus(String status) throws Exception {
        return strings("SELECT request_id FROM venue_requests WHERE status='" + status + "'").stream()
                .map(UUID::fromString).toList();
    }

    private void deliverInbox() {
        int guard = 0;
        while (inboxWorker.processOnce()) {
            assertTrue(++guard < 50, "outbox did not drain");
        }
    }

    private static String code(Executable action) {
        return assertThrows(ApplicationException.class, action).code();
    }

    private List<String> strings(String query) throws Exception {
        try (var connection = DriverManager.getConnection(configuration.url(), user, password);
                var statement = connection.createStatement();
                var rows = statement.executeQuery(query)) {
            List<String> values = new ArrayList<>();
            while (rows.next()) {
                values.add(rows.getString(1));
            }
            return values;
        }
    }

    private void sql(String statementText) throws Exception {
        try (var connection = DriverManager.getConnection(configuration.url(), user, password);
                var statement = connection.createStatement()) {
            statement.execute(statementText);
        }
    }
}
