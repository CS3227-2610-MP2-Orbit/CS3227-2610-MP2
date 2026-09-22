package seedu.eventmanager.ui;

import java.time.Clock;
import java.util.Arrays;
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
import seedu.eventmanager.storage.DatabaseConfig;
import seedu.eventmanager.storage.DatabaseMigration;
import seedu.eventmanager.storage.DriverManagerDataSource;

/** Desktop application shell that routes users to the available role workspaces. */
public final class EventManagerApplication extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Event Venue Manager");
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f7f9fc;");
        root.setPadding(new Insets(24));
        showHome(root);
        stage.setScene(new Scene(root, 1_100, 700));
        stage.show();
    }

    private void showHome(BorderPane root) {
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
            Map<String, String> environment = System.getenv();
            DatabaseConfig databaseConfig = DatabaseConfig.fromEnvironment(environment);
            DriverManagerDataSource dataSource = new DriverManagerDataSource(databaseConfig);
            new DatabaseMigration(dataSource).migrate();

            OrganizerIdentity organizer = organizerFrom(environment);
            EventService service = new EventService(
                    new JdbcEventRepository(dataSource), UUID::randomUUID, Clock.systemUTC());
            showWorkspace(root, "Club Organizer", new OrganizerEventView(service, organizer));
        } catch (RuntimeException | java.sql.SQLException exception) {
            showWorkspace(root, "Club Organizer", databaseErrorView());
        }
    }

    private void showVenueAdministrator(BorderPane root) {
        showWorkspace(root, "Venue Administrator", new VenueAdministratorFxApplication().createRoot());
    }

    private void showWorkspace(BorderPane root, String title, Node workspace) {
        Button home = new Button("← Home");
        home.setOnAction(ignored -> showHome(root));
        Label workspaceTitle = new Label(title);
        workspaceTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        HBox header = new HBox(12, home, workspaceTitle);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 16, 0));
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

    private static VBox databaseErrorView() {
        Label heading = new Label("Unable to connect to the event database");
        heading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        Label help = new Label("""
                Start PostgreSQL and configure EVENT_MANAGER_DB_URL, EVENT_MANAGER_DB_USER,
                and EVENT_MANAGER_DB_PASSWORD. Connection details and credentials are not displayed.""");
        help.setWrapText(true);
        VBox view = new VBox(12, heading, help);
        view.setPadding(new Insets(24));
        return view;
    }
}
