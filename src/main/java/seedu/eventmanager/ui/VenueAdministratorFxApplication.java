package seedu.eventmanager.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import seedu.eventmanager.storage.DatabaseBootstrap;
import seedu.eventmanager.storage.DatabaseConfiguration;
import seedu.eventmanager.storage.JdbcDatabase;
import seedu.eventmanager.storage.JdbcLocalSessionService;
import seedu.eventmanager.storage.PasswordHasher;

/** Initial JavaFX shell for the Venue Administrator frontend. */
public final class VenueAdministratorFxApplication extends Application {
    private static final int WIDTH = 1100;
    private static final int HEIGHT = 700;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f7f9fc;");

        stage.setTitle("Event Venue Manager");
        stage.setScene(new Scene(root, WIDTH, HEIGHT));
        showLogin(stage, root);
        stage.show();
    }

    private void showLogin(Stage stage, BorderPane root) {
        try {
            DatabaseConfiguration configuration = DatabaseBootstrap.configuration();
            DatabaseBootstrap.migrate(configuration);
            JdbcLocalSessionService sessions = new JdbcLocalSessionService(
                    new JdbcDatabase(configuration), new PasswordHasher());
            VenueAdministratorLoginView login = new VenueAdministratorLoginView(sessions,
                    session -> showDashboardPlaceholder(stage, root, sessions, session));
            root.setCenter(login.root());
        } catch (RuntimeException exception) {
            Label error = new Label("Unable to start database-backed login: " + exception.getMessage());
            error.setWrapText(true);
            error.setPadding(new Insets(32));
            root.setCenter(error);
        }
    }

    private void showDashboardPlaceholder(Stage stage, BorderPane root,
            JdbcLocalSessionService sessions, JdbcLocalSessionService.Session session) {
        Label heading = new Label("Venue Administrator Dashboard");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Label welcome = new Label("Signed in as " + session.actor().userId()
                + ". Dashboard modules will be added next.");
        Button logout = new Button("Log out");
        logout.setOnAction(event -> showLogin(stage, root));
        VBox content = new VBox(12, heading, welcome, logout);
        content.setPadding(new Insets(32));
        root.setCenter(content);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
