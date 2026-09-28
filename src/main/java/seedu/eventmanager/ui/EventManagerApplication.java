package seedu.eventmanager.ui;

import java.time.Clock;
import java.util.UUID;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import seedu.eventmanager.announcement.AnnouncementService;
import seedu.eventmanager.announcement.JdbcAnnouncementRepository;
import seedu.eventmanager.club.ClubService;
import seedu.eventmanager.club.JdbcClubRepository;
import seedu.eventmanager.event.EventService;
import seedu.eventmanager.attendee.EventCatalogueService;
import seedu.eventmanager.attendee.AttendeeEventDetailsService;
import seedu.eventmanager.attendee.MyRegistrationsService;
import seedu.eventmanager.attendee.InboxDispatcher;
import seedu.eventmanager.attendee.InboxNotificationDelivery;
import seedu.eventmanager.attendee.InboxService;
import seedu.eventmanager.attendee.AttendanceHistoryService;
import seedu.eventmanager.storage.JdbcAttendanceHistoryRepository;
import seedu.eventmanager.notification.NotificationOutboxWorker;
import seedu.eventmanager.event.JdbcEventRepository;
import seedu.eventmanager.event.OrganizerVenueRequestService;
import seedu.eventmanager.event.RegistrationOverviewService;
import seedu.eventmanager.registration.EventRegistrations;
import seedu.eventmanager.registration.RegistrationService;
import seedu.eventmanager.registration.RegistrationServiceFactory;
import seedu.eventmanager.storage.DatabaseBootstrap;
import seedu.eventmanager.storage.DatabaseConfig;
import seedu.eventmanager.storage.DatabaseConfiguration;
import seedu.eventmanager.storage.DatabaseMigration;
import seedu.eventmanager.storage.DriverManagerDataSource;
import seedu.eventmanager.storage.JdbcDatabase;
import seedu.eventmanager.storage.JdbcEventCatalogueRepository;
import seedu.eventmanager.storage.JdbcAttendeeEventDetailsRepository;
import seedu.eventmanager.storage.JdbcRegistrationEventInfoRepository;
import seedu.eventmanager.storage.JdbcEventRegistrations;
import seedu.eventmanager.storage.JdbcNotificationService;
import seedu.eventmanager.storage.InboxDatabaseMigration;
import seedu.eventmanager.storage.JdbcInboxRepository;
import seedu.eventmanager.storage.JdbcNotificationOutboxRepository;
import seedu.eventmanager.storage.JdbcTransactionManager;
import seedu.eventmanager.storage.RegistrationDatabaseMigration;
import seedu.eventmanager.storage.JdbcVenueRepository;
import seedu.eventmanager.storage.JdbcVenueRequestRepository;
import seedu.eventmanager.storage.JdbcLocalSessionService;
import seedu.eventmanager.storage.PasswordHasher;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.volunteer.JdbcVolunteerRepository;
import seedu.eventmanager.volunteer.VolunteerService;

/** Desktop application shell that routes users to the available role workspaces. */
public final class EventManagerApplication extends Application {
    private AttendeeBrowseView attendeeView;
    private final InboxDispatcher inboxDispatcher = new InboxDispatcher();

    @Override
    public void start(Stage stage) {
        stage.setTitle("Event Venue Manager");
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f7f9fc;");
        showHome(root);
        stage.setScene(new Scene(root, 1_280, 800));
        stage.setMinWidth(1_000);
        stage.setMinHeight(640);
        stage.show();
    }

    private void showHome(BorderPane root) {
        closeAttendee();
        try {
            DatabaseConfiguration configuration = DatabaseBootstrap.configuration();
            DatabaseBootstrap.migrate(configuration);
            JdbcLocalSessionService sessions = new JdbcLocalSessionService(
                    new JdbcDatabase(configuration), new PasswordHasher());
            root.setPadding(new Insets(24));
            root.setTop(null);
            root.setCenter(new HomeAuthenticationView(sessions,
                    session -> routeAuthenticatedUser(root, configuration, session)));
        } catch (RuntimeException exception) {
            root.setPadding(new Insets(24));
            root.setCenter(databaseErrorView(exception));
        }
    }

    private void routeAuthenticatedUser(BorderPane root, DatabaseConfiguration configuration,
            JdbcLocalSessionService.Session session) {
        if (session.actor().role() == Role.VENUE_ADMINISTRATOR) {
            showVenueAdministrator(root, session);
        } else if (session.actor().role() == Role.CLUB_ORGANIZER) {
            showOrganizer(root, session.actor());
        } else {
            showAttendee(root, session);
        }
    }

    private void showOrganizer(BorderPane root, Actor actor) {
        try {
            DatabaseConfiguration configuration = DatabaseBootstrap.configuration();
            DatabaseBootstrap.migrate(configuration);
            DatabaseConfig databaseConfig = new DatabaseConfig(
                    configuration.url(), configuration.username(), configuration.password());
            DriverManagerDataSource dataSource = new DriverManagerDataSource(databaseConfig);
            new DatabaseMigration(dataSource).migrate();
            RegistrationDatabaseMigration.migrate(configuration);

            ClubService clubService = new ClubService(
                    new JdbcClubRepository(dataSource), UUID::randomUUID, Clock.systemUTC());
            JdbcDatabase jdbcDatabase = new JdbcDatabase(configuration);
            JdbcVenueRepository venueRepository = new JdbcVenueRepository(jdbcDatabase);
            JdbcVenueRequestRepository venueRequestRepository = new JdbcVenueRequestRepository(jdbcDatabase);
            EventService eventService = new EventService(
                    new JdbcEventRepository(dataSource),
                    UUID::randomUUID,
                    Clock.systemUTC(),
                    venueRequestRepository);
            OrganizerVenueRequestService venueRequestService = new OrganizerVenueRequestService(
                    eventService,
                    venueRepository,
                    venueRequestRepository,
                    UUID::randomUUID);
            EventRegistrations registrations = new JdbcEventRegistrations(jdbcDatabase);
            VolunteerService volunteerService = new VolunteerService(
                    eventService,
                    registrations,
                    new JdbcVolunteerRepository(dataSource),
                    Clock.systemUTC());
            RegistrationOverviewService registrationService =
                    new RegistrationOverviewService(eventService, registrations);
            AnnouncementService announcementService = new AnnouncementService(
                    eventService,
                    registrations,
                    new JdbcAnnouncementRepository(dataSource),
                    new JdbcNotificationService(jdbcDatabase),
                    UUID::randomUUID,
                    Clock.systemUTC());
            // Edge-to-edge role shell: Home lives in the Organizer sidebar (no dual chrome).
            root.setPadding(Insets.EMPTY);
            root.setTop(null);
            root.setCenter(new OrganizerEventView(
                    eventService,
                    venueRequestService,
                    volunteerService,
                    registrationService,
                    announcementService,
                    clubService,
                    venueRepository,
                    actor,
                    () -> showHome(root)));
        } catch (RuntimeException | java.sql.SQLException exception) {
            root.setPadding(new Insets(24));
            showWorkspace(root, "Club Organizer", databaseErrorView(exception));
        }
    }

    private void showVenueAdministrator(BorderPane root, JdbcLocalSessionService.Session session) {
        root.setPadding(Insets.EMPTY);
        showWorkspace(root, "Venue Administrator",
                new VenueAdministratorFxApplication().createRoot(session));
    }

    private void showAttendee(BorderPane root, JdbcLocalSessionService.Session session) {
        closeAttendee();
        root.setPadding(Insets.EMPTY);
        root.setTop(null);
        record AttendeeServices(EventCatalogueService catalogue, AttendeeEventDetailsService details,
                RegistrationService commands, MyRegistrationsService registrations,
                InboxService inbox, AttendanceHistoryService history) { }
        // Lazy bootstrap runs only on background tasks; a failed initialization can be retried.
        var services = new java.util.function.Supplier<AttendeeServices>() {
            private AttendeeServices value;

            @Override public synchronized AttendeeServices get() {
                if (value == null) {
                    DatabaseConfiguration configuration = DatabaseBootstrap.configuration();
                    InboxDatabaseMigration.migrate(configuration);
                    var dataSource = new DriverManagerDataSource(new DatabaseConfig(
                            configuration.url(), configuration.username(), configuration.password()));
                    var database = new JdbcDatabase(configuration);
                    var sessions = new JdbcLocalSessionService(database, new PasswordHasher());
                    var commands = RegistrationServiceFactory.create(configuration, Clock.systemUTC());
                    var inbox = new JdbcInboxRepository(database);
                    value = new AttendeeServices(
                            new EventCatalogueService(new JdbcEventCatalogueRepository(dataSource), Clock.systemUTC()),
                            new AttendeeEventDetailsService(new JdbcAttendeeEventDetailsRepository(database),
                                    sessions::resolve, Clock.systemUTC()), commands,
                            new MyRegistrationsService(commands::myRegistrations,
                                    new JdbcRegistrationEventInfoRepository(database),
                                    sessions::resolve, Clock.systemUTC()),
                            new InboxService(inbox, sessions::resolve,
                                    new JdbcTransactionManager(database), Clock.systemUTC()),
                            new AttendanceHistoryService(new JdbcAttendanceHistoryRepository(database), sessions::resolve));
                    inboxDispatcher.start(new NotificationOutboxWorker(
                            new JdbcNotificationOutboxRepository(database, InboxNotificationDelivery.EVENT_TYPES),
                            new InboxNotificationDelivery(inbox::deliver)));
                }
                return value;
            }
        };
        attendeeView = new AttendeeBrowseView(() -> services.get().catalogue(),
                id -> services.get().details().getEvent(session.token(), id),
                new AttendeeRegistrationActions(
                        (id, version) -> services.get().commands().register(session.token(), id, version),
                        (id, version) -> services.get().commands().cancel(session.token(), id, version),
                        (id, version) -> services.get().commands().checkIn(session.token(), id, version),
                        () -> services.get().registrations().list(session.token())),
                new InboxActions(() -> services.get().inbox().list(session.token()),
                        id -> services.get().inbox().markRead(session.token(), id),
                        () -> services.get().inbox().markAllRead(session.token())),
                () -> services.get().history().list(session.token()), () -> showHome(root));
        root.setCenter(attendeeView);
    }

    private void closeAttendee() {
        if (attendeeView != null) {
            attendeeView.close();
            attendeeView = null;
        }
    }

    @Override
    public void stop() {
        inboxDispatcher.close();
        closeAttendee();
    }

    private void showWorkspace(BorderPane root, String title, Node workspace) {
        root.setPadding(new Insets(16, 16, 16, 16));
        Button home = new Button("← Home");
        home.setOnAction(ignored -> showHome(root));
        Label workspaceTitle = new Label(title);
        workspaceTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        HBox header = new HBox(12, home, workspaceTitle);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 12, 0));
        root.setTop(header);
        root.setCenter(workspace);
    }

    private static Button roleButton(String title, String detail) {
        Button button = new Button(title + "\n" + detail);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(74);
        button.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 14px;");
        return button;
    }

    private static VBox databaseErrorView(Exception exception) {
        Label heading = new Label("Unable to connect to the event database");
        heading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        Label help = new Label("""
                Start Postgres.app (or another local PostgreSQL), then create a project-root .env
                with DATABASE_URL / DATABASE_USER (or EVENT_MANAGER_DB_*). Restart the app after
                changing .env. Passwords and full connection strings are not shown here.""");
        help.setWrapText(true);
        Label detail = new Label(exception.getClass().getSimpleName()
                + ": "
                + (exception.getMessage() == null ? "no message" : exception.getMessage()));
        detail.setWrapText(true);
        detail.setStyle("-fx-text-fill: #6b7280;");
        VBox view = new VBox(12, heading, help, detail);
        view.setPadding(new Insets(24));
        return view;
    }
}
