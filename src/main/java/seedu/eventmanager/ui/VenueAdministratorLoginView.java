package seedu.eventmanager.ui;

import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.storage.JdbcLocalSessionService;

/** Minimal login form for local Venue Administrator sessions. */
public final class VenueAdministratorLoginView {
    private final JdbcLocalSessionService sessions;
    private final Consumer<JdbcLocalSessionService.Session> onLogin;
    private final VBox root = new VBox(12);
    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();
    private final Label message = new Label();

    public VenueAdministratorLoginView(JdbcLocalSessionService sessions,
            Consumer<JdbcLocalSessionService.Session> onLogin) {
        this.sessions = Objects.requireNonNull(sessions);
        this.onLogin = Objects.requireNonNull(onLogin);
        build();
    }

    public VBox root() {
        return root;
    }

    private void build() {
        Label heading = new Label("Venue Administrator Login");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        username.setPromptText("Username");
        password.setPromptText("Password");
        Button login = new Button("Log in");
        login.setDefaultButton(true);
        login.setOnAction(event -> submit());
        message.setStyle("-fx-text-fill: #b42318;");
        root.getChildren().addAll(heading, username, password, login, message);
        root.setMaxWidth(360);
        root.setPadding(new Insets(32));
    }

    private void submit() {
        message.setText("");
        try {
            JdbcLocalSessionService.Session session = sessions.login(username.getText(), password.getText());
            if (session.actor().role() != Role.VENUE_ADMINISTRATOR) {
                throw new IllegalArgumentException("This login is not a Venue Administrator account.");
            }
            onLogin.accept(session);
        } catch (RuntimeException exception) {
            message.setText(exception.getMessage() == null
                    ? "Login failed." : exception.getMessage());
        }
    }
}
