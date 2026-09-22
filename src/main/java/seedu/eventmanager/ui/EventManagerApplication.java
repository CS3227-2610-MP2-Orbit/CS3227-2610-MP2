package seedu.eventmanager.ui;

import java.time.Clock;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import seedu.eventmanager.event.EventService;
import seedu.eventmanager.event.JdbcEventRepository;
import seedu.eventmanager.event.OrganizerIdentity;
import seedu.eventmanager.storage.DatabaseConfig;
import seedu.eventmanager.storage.DatabaseMigration;
import seedu.eventmanager.storage.DriverManagerDataSource;

/** Desktop application shell for the Club Organizer event workflow. */
public final class EventManagerApplication extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Event Venue Manager");
        try {
            Map<String, String> environment = System.getenv();
            DatabaseConfig databaseConfig = DatabaseConfig.fromEnvironment(environment);
            DriverManagerDataSource dataSource = new DriverManagerDataSource(databaseConfig);
            new DatabaseMigration(dataSource).migrate();

            OrganizerIdentity organizer = organizerFrom(environment);
            EventService service = new EventService(
                    new JdbcEventRepository(dataSource), UUID::randomUUID, Clock.systemUTC());
            stage.setScene(new Scene(
                    new OrganizerEventView(service, organizer), 1_000, 680));
        } catch (RuntimeException | java.sql.SQLException exception) {
            stage.setScene(new Scene(databaseErrorView(), 700, 260));
        }
        stage.show();
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
