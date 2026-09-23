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
import seedu.eventmanager.event.JdbcEventRepository;
import seedu.eventmanager.event.OrganizerIdentity;
import seedu.eventmanager.storage.DatabaseBootstrap;
import seedu.eventmanager.storage.DatabaseConfig;
import seedu.eventmanager.storage.DatabaseConfiguration;
import seedu.eventmanager.storage.DatabaseMigration;
import seedu.eventmanager.storage.DriverManagerDataSource;

/** Desktop application shell that routes users to the available role workspaces. */
public final class EventManagerApplication extends Application {
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
        root.setPadding(new Insets(24));
        Label heading = new Label("Event Venue Manager");
        heading.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        Label message = new Label("Choose a workspace to continue.");
        message.setStyle("-fx-text-fill: #526075; -fx-font-size: 14px;");

        Button organizer = roleButton("Club Organizer", "Create and edit your club events.");
        organizer.setOnAction(ignored -> showOrganizer(root));
        Button venueAdministrator = roleButton("Venue Administrator",
                "Review venue requests and manage venues.");
        venueAdministrator.setOnAction(ignored -> showVenueAdministrator(root));

        VBox choices = new VBox(14, organizer, venueAdministrator);
        choices.setMaxWidth(420);
        VBox content = new VBox(18, heading, message, choices);
        content.setAlignment(Pos.CENTER_LEFT);
        root.setTop(null);
        root.setCenter(content);
    }

    private void showOrganizer(BorderPane root) {
        try {
            DatabaseConfiguration configuration = DatabaseBootstrap.configuration();
            DatabaseConfig databaseConfig = new DatabaseConfig(
                    configuration.url(), configuration.username(), configuration.password());
            DriverManagerDataSource dataSource = new DriverManagerDataSource(databaseConfig);
            new DatabaseMigration(dataSource).migrate();

            OrganizerIdentity organizer = organizerFrom(localSettings());
            EventService service = new EventService(
                    new JdbcEventRepository(dataSource), UUID::randomUUID, Clock.systemUTC());
            // Edge-to-edge role shell: Home lives in the Organizer sidebar (no dual chrome).
            root.setPadding(Insets.EMPTY);
            root.setTop(null);
            root.setCenter(new OrganizerEventView(service, organizer, () -> showHome(root)));
        } catch (RuntimeException | java.sql.SQLException exception) {
            root.setPadding(new Insets(24));
            showWorkspace(root, "Club Organizer", databaseErrorView(exception));
        }
    }

    private void showVenueAdministrator(BorderPane root) {
        root.setPadding(Insets.EMPTY);
        showWorkspace(root, "Venue Administrator", new VenueAdministratorFxApplication().createRoot());
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
