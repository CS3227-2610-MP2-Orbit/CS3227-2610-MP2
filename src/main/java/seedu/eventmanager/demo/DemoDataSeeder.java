package seedu.eventmanager.demo;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import seedu.eventmanager.announcement.AnnouncementService;
import seedu.eventmanager.announcement.JdbcAnnouncementRepository;
import seedu.eventmanager.club.ClubService;
import seedu.eventmanager.club.JdbcClubRepository;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.event.Event;
import seedu.eventmanager.event.EventDetails;
import seedu.eventmanager.event.EventService;
import seedu.eventmanager.event.JdbcEventRepository;
import seedu.eventmanager.event.OrganizerIdentity;
import seedu.eventmanager.event.OrganizerVenueRequestService;
import seedu.eventmanager.registration.Registration;
import seedu.eventmanager.registration.RegistrationServiceFactory;
import seedu.eventmanager.service.VenueAdministratorService;
import seedu.eventmanager.service.VenueAdministratorServiceFactory;
import seedu.eventmanager.storage.DatabaseBootstrap;
import seedu.eventmanager.storage.DatabaseConfig;
import seedu.eventmanager.storage.DatabaseConfiguration;
import seedu.eventmanager.storage.DriverManagerDataSource;
import seedu.eventmanager.storage.InboxDatabaseMigration;
import seedu.eventmanager.storage.JdbcAuthorizationService;
import seedu.eventmanager.storage.JdbcDatabase;
import seedu.eventmanager.storage.JdbcDraftEventDeletion;
import seedu.eventmanager.storage.JdbcEventBookingCheck;
import seedu.eventmanager.storage.JdbcEventRegistrations;
import seedu.eventmanager.storage.JdbcLocalSessionService;
import seedu.eventmanager.storage.JdbcNotificationService;
import seedu.eventmanager.storage.JdbcVenueRelease;
import seedu.eventmanager.storage.JdbcVenueRepository;
import seedu.eventmanager.storage.JdbcVenueRequestRepository;
import seedu.eventmanager.storage.PasswordHasher;
import seedu.eventmanager.venue.Venue;
import seedu.eventmanager.venue.VenueStatus;
import seedu.eventmanager.volunteer.JdbcVolunteerRepository;
import seedu.eventmanager.volunteer.VolunteerService;

/**
 * Loads opt-in demo data for testers through the real services, so every row passes the same
 * business rules and audit paths as the app. Run with {@code ./gradlew seedDemo} or
 * {@code java -jar <jar> --seed-demo}. It never runs automatically and skips an already seeded database.
 */
public final class DemoDataSeeder {
    /** Shared password for every seeded demo account; documented in the User Guide. */
    public static final String DEMO_PASSWORD = "demo1234";
    static final List<String> USERNAMES = List.of("demo_admin", "demo_organizer", "demo_organizer2",
            "demo_attendee", "demo_attendee2", "demo_attendee3");
    private static final ZoneId SGT = ZoneId.of("Asia/Singapore");

    private final DatabaseConfiguration configuration;
    private final JdbcDatabase database;
    private final DataSource dataSource;
    private final JdbcLocalSessionService sessions;
    private final Instant now;
    private final LocalDate today;

    DemoDataSeeder(DatabaseConfiguration configuration, Clock clock) {
        this.configuration = Objects.requireNonNull(configuration);
        this.database = new JdbcDatabase(configuration);
        this.dataSource = new DriverManagerDataSource(
                new DatabaseConfig(configuration.url(), configuration.username(), configuration.password()));
        this.sessions = new JdbcLocalSessionService(database, new PasswordHasher());
        this.now = clock.instant().truncatedTo(ChronoUnit.MINUTES);
        this.today = LocalDate.ofInstant(now, SGT);
    }

    public static void main(String[] args) {
        DatabaseConfiguration configuration = DatabaseBootstrap.configuration();
        boolean seeded = new DemoDataSeeder(configuration, Clock.systemUTC()).seed();
        System.out.println(seeded
                ? "Demo data loaded. Log in as " + String.join(", ", USERNAMES)
                        + " with the demo password from the User Guide."
                : "Demo data is already present; nothing was changed.");
    }

    /** Returns false without writing anything when the demo accounts already exist. */
    boolean seed() {
        InboxDatabaseMigration.migrate(configuration);
        if (alreadySeeded()) {
            return false;
        }
        Actor admin = account("demo_admin", Role.VENUE_ADMINISTRATOR);
        account("demo_organizer", Role.CLUB_ORGANIZER);
        account("demo_organizer2", Role.CLUB_ORGANIZER);
        account("demo_attendee", Role.ATTENDEE);
        Actor volunteer = account("demo_attendee2", Role.ATTENDEE);
        account("demo_attendee3", Role.ATTENDEE);

        UUID seminarRoom = venue("Seminar Room 3", "COM1 Level 2", 40, VenueStatus.ACTIVE);
        UUID hall = venue("Multipurpose Hall", "University Town", 150, VenueStatus.ACTIVE);
        UUID theatre = venue("LT27", "Science Faculty", 300, VenueStatus.ACTIVE);
        venue("Old Dance Studio", "Yusof Ishak House", 25, VenueStatus.MAINTENANCE);

        OrganizerIdentity hackers = club("demo_organizer", "NUS Hackers");
        OrganizerIdentity photography = club("demo_organizer", "Photography Society");
        OrganizerIdentity dance = club("demo_organizer2", "Dance Club");

        Event git = published(hackers, admin, now, seminarRoom, new EventDetails("Intro to Git Workshop",
                "Hands-on basics: commits, branches and pull requests.", at(3, 18), at(3, 20), 40));
        Event hackNight = published(hackers, admin, now, hall, new EventDetails("Hack Night",
                "An evening of building side projects with friends.", at(10, 19), at(10, 23), 120));
        published(photography, admin, now, seminarRoom, new EventDetails("Photo Walk: Kent Ridge",
                "Golden-hour walk around campus. Bring any camera.", at(14, 9), at(14, 11), 25));
        Instant showStarts = now.truncatedTo(ChronoUnit.HOURS).minus(Duration.ofHours(1));
        Instant twoDaysAgo = now.minus(Duration.ofDays(2));
        Event showcase = published(dance, admin, twoDaysAgo, theatre, new EventDetails("Street Dance Showcase",
                "Happening now: check in from My Registrations.", showStarts, showStarts.plus(Duration.ofHours(6)),
                200));
        Instant tenDaysAgo = now.minus(Duration.ofDays(10));
        Event basics = published(photography, admin, tenDaysAgo, seminarRoom, new EventDetails(
                "Photography Basics", "Exposure, framing and light.", at(-7, 14), at(-7, 16), 30));

        Event robotics = events(now).createEvent(hackers, club(hackers), new EventDetails("Robotics Demo Day",
                "Waiting for venue approval.", at(21, 10), at(21, 17), 250));
        organizerVenues(now).submit(hackers, robotics.id(), theatre);
        events(now).createEvent(dance, club(dance), new EventDetails("Welcome Tea",
                "Draft with no venue yet.", at(5, 16), at(5, 18), 60));

        register("demo_attendee", git.id(), now);
        register("demo_attendee2", git.id(), now);
        register("demo_attendee2", hackNight.id(), now);
        Registration cancelled = register("demo_attendee3", hackNight.id(), now);
        registrations(now).cancel(login("demo_attendee3"), hackNight.id(), cancelled.version());
        register("demo_attendee", showcase.id(), twoDaysAgo);
        Registration attended = register("demo_attendee", basics.id(), tenDaysAgo);
        registrations(at(-7, 14).plus(Duration.ofMinutes(20)))
                .checkIn(login("demo_attendee"), basics.id(), attended.version());

        volunteers().assign(hackers, git.id(), volunteer.userId(), "Usher");
        announcements().post(hackers, git.id(), "Please bring a laptop with Git installed.");
        return true;
    }

    private boolean alreadySeeded() {
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
                statement.setString(1, "demo_organizer");
                try (var result = statement.executeQuery()) {
                    return result.next();
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not check for existing demo data.", exception);
            }
        });
    }

    private Actor account(String username, Role role) {
        return new Actor(UUID.fromString(sessions.createUser(username, DEMO_PASSWORD, role)), role);
    }

    private String login(String username) {
        return sessions.login(username, DEMO_PASSWORD).token();
    }

    private UUID venue(String name, String location, int capacity, VenueStatus status) {
        UUID id = UUID.randomUUID();
        new JdbcVenueRepository(database).save(new Venue(id, name, location, capacity, "Demo venue", status));
        return id;
    }

    private OrganizerIdentity club(String organizer, String name) {
        Actor actor = sessions.login(organizer, DEMO_PASSWORD).actor();
        ClubService clubs = new ClubService(new JdbcClubRepository(dataSource), UUID::randomUUID,
                Clock.fixed(now, ZoneOffset.UTC));
        UUID clubId = clubs.createClub(actor, name).id();
        return new OrganizerIdentity(actor.userId().toString(), Set.of(clubId.toString()));
    }

    private static String club(OrganizerIdentity organizer) {
        return organizer.ownedClubIds().iterator().next();
    }

    /** Creates, books and publishes an event as if the organizer and admin acted at {@code when}. */
    private Event published(OrganizerIdentity organizer, Actor admin, Instant when, UUID venue, EventDetails details) {
        Event draft = events(when).createEvent(organizer, club(organizer), details);
        venueAdmin().approve(admin, organizerVenues(when).submit(organizer, draft.id(), venue).requestId());
        return events(when).publishEvent(organizer, draft.id(), draft.version());
    }

    private Registration register(String attendee, UUID eventId, Instant when) {
        return registrations(when).register(login(attendee), eventId, -1);
    }

    private Instant at(int daysFromToday, int hourSgt) {
        return ZonedDateTime.of(today.plusDays(daysFromToday), LocalTime.of(hourSgt, 0), SGT).toInstant();
    }

    private EventService events(Instant when) {
        return new EventService(new JdbcEventRepository(dataSource), UUID::randomUUID,
                Clock.fixed(when, ZoneOffset.UTC), new JdbcVenueRequestRepository(database),
                new JdbcEventBookingCheck(dataSource), new JdbcDraftEventDeletion(dataSource));
    }

    private OrganizerVenueRequestService organizerVenues(Instant when) {
        return new OrganizerVenueRequestService(events(when), new JdbcVenueRepository(database),
                new JdbcVenueRequestRepository(database), UUID::randomUUID, new JdbcVenueRelease(dataSource));
    }

    private VenueAdministratorService venueAdmin() {
        return VenueAdministratorServiceFactory.create(configuration, new JdbcAuthorizationService(database));
    }

    private seedu.eventmanager.registration.RegistrationService registrations(Instant when) {
        return RegistrationServiceFactory.create(configuration, Clock.fixed(when, ZoneOffset.UTC));
    }

    private VolunteerService volunteers() {
        return new VolunteerService(events(now), new JdbcEventRegistrations(database),
                new JdbcVolunteerRepository(dataSource), Clock.fixed(now, ZoneOffset.UTC));
    }

    private AnnouncementService announcements() {
        return new AnnouncementService(events(now), new JdbcEventRegistrations(database),
                new JdbcAnnouncementRepository(dataSource), new JdbcNotificationService(database),
                UUID::randomUUID, Clock.fixed(now, ZoneOffset.UTC));
    }
}
