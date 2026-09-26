package seedu.eventmanager.ui;

import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.util.List;
import java.util.Optional;
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
        Button register = new Button("Create normal user account");
        register.setOnAction(event -> register());
        message.setStyle("-fx-text-fill: #b42318;");
        message.setWrapText(true);
        message.setMaxWidth(900);
        root.getChildren().addAll(heading, username, password, login, register, message);
        root.setPrefWidth(900);
        root.setMaxWidth(1000);
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

    private void register() {
        TextField registrationUsername = new TextField();
        PasswordField registrationPassword = new PasswordField();
        ChoiceDialog<Role> roleDialog = new ChoiceDialog<>(Role.ATTENDEE,
                List.of(Role.ATTENDEE, Role.CLUB_ORGANIZER));
        roleDialog.setTitle("Create normal user account");
        roleDialog.setHeaderText("Choose a normal user role");
        roleDialog.setContentText("Role:");
        Optional<Role> role = roleDialog.showAndWait();
        if (role.isEmpty()) return;

        javafx.scene.control.Dialog<ButtonType> details = new javafx.scene.control.Dialog<>();
        details.setTitle("Create normal user account");
        details.setHeaderText("Enter your account details");
        ButtonType create = new ButtonType("Create", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        details.getDialogPane().getButtonTypes().addAll(create, ButtonType.CANCEL);
        VBox fields = new VBox(8, new Label("Username"), registrationUsername,
                new Label("Password (at least 8 characters)"), registrationPassword);
        fields.setPadding(new Insets(12));
        details.getDialogPane().setContent(fields);
        details.setResultConverter(button -> button == create ? create : null);
        if (details.showAndWait().isEmpty()) return;
        try {
            sessions.registerNormalUser(registrationUsername.getText(), registrationPassword.getText(), role.get());
            message.setStyle("-fx-text-fill: #067647;");
            message.setText("Account created. You can now log in.");
        } catch (RuntimeException exception) {
            message.setStyle("-fx-text-fill: #b42318;");
            message.setText(exception.getMessage() == null ? "Registration failed." : exception.getMessage());
        }
    }
}
