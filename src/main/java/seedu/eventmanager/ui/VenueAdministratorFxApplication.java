package seedu.eventmanager.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Initial JavaFX shell for the Venue Administrator frontend. */
public final class VenueAdministratorFxApplication extends Application {
    private static final int WIDTH = 1100;
    private static final int HEIGHT = 700;

    @Override
    public void start(Stage stage) {
        Label heading = new Label("Venue Administrator Dashboard");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label placeholder = new Label("Frontend foundation ready. Login and dashboard modules will be added next.");
        placeholder.setStyle("-fx-font-size: 14px;");

        VBox content = new VBox(12, heading, placeholder);
        content.setPadding(new Insets(32));

        BorderPane root = new BorderPane(content);
        root.setStyle("-fx-background-color: #f7f9fc;");

        stage.setTitle("Event Venue Manager");
        stage.setScene(new Scene(root, WIDTH, HEIGHT));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
