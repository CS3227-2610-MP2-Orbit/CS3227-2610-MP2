package seedu.eventmanager.ui;

import io.github.cdimascio.dotenv.Dotenv;
import java.time.Clock;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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
import seedu.eventmanager.event.EventService;
import seedu.eventmanager.attendee.EventCatalogueService;
import seedu.eventmanager.event.JdbcEventRepository;
import seedu.eventmanager.event.OrganizerIdentity;
import seedu.eventmanager.event.OrganizerVenueRequestService;
import seedu.eventmanager.storage.DatabaseBootstrap;
import seedu.eventmanager.storage.DatabaseConfig;
import seedu.eventmanager.storage.DatabaseConfiguration;
import seedu.eventmanager.storage.DatabaseMigration;
import seedu.eventmanager.storage.DriverManagerDataSource;
import seedu.eventmanager.storage.JdbcDatabase;
import seedu.eventmanager.storage.JdbcEventCatalogueRepository;
import seedu.eventmanager.storage.JdbcVenueRepository;
import seedu.eventmanager.storage.JdbcVenueRequestRepository;
import seedu.eventmanager.storage.JdbcLocalSessionService;
import seedu.eventmanager.storage.PasswordHasher;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.Role;

/** Desktop application shell that routes users to the available role workspaces. */
public final class EventManagerApplication extends Application {
    private AttendeeBrowseView attendeeView;

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
            showAttendee(root);
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

            OrganizerIdentity organizer = organizerFrom(localSettings());
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
            // Edge-to-edge role shell: Home lives in the Organizer sidebar (no dual chrome).
            root.setPadding(Insets.EMPTY);
            root.setTop(null);
            root.setCenter(new OrganizerEventView(
                    eventService,
                    venueRequestService,
                    venueRepository,
                    organizer,
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

    private void showAttendee(BorderPane root) {
        closeAttendee();
        root.setPadding(Insets.EMPTY);
        root.setTop(null);
        attendeeView = new AttendeeBrowseView(() -> {
            // Configuration and JDBC are resolved by the view's background task.
            DatabaseConfiguration configuration = DatabaseBootstrap.configuration();
            var dataSource = new DriverManagerDataSource(new DatabaseConfig(
                    configuration.url(), configuration.username(), configuration.password()));
            return new EventCatalogueService(new JdbcEventCatalogueRepository(dataSource), Clock.systemUTC());
        }, () -> showHome(root));
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

    private static OrganizerIdentity organizerFrom(Map<String, String> environment) {
        String organizerId = environment.getOrDefault(
                "EVENT_MANAGER_ORGANIZER_ID", "demo-organizer");
        String rawClubIds = environment.getOrDefault("EVENT_MANAGER_CLUB_IDS", "demo-club");
        Set<String> clubIds = new LinkedHashSet<>();
        Arrays.stream(rawClubIds.split(","))
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .forEach(clubIds::add);
        if (clubIds.isEmpty()) {
            throw new IllegalArgumentException("At least one organizer club ID is required");
        }
        return new OrganizerIdentity(organizerId, clubIds);
    }

    /** Process env vars override values from the project-root {@code .env} file. */
    private static Map<String, String> localSettings() {
        Map<String, String> values = new HashMap<>();
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();
        dotenv.entries().forEach(entry -> values.put(entry.getKey(), entry.getValue()));
        values.putAll(System.getenv());
        return values;
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
