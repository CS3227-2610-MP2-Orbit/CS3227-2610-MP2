package seedu.eventmanager.ui;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.storage.JdbcLocalSessionService;

/** Shared authentication gate for the role workspaces. */
public final class HomeAuthenticationView extends VBox {
    private final JdbcLocalSessionService sessions;
    private final Consumer<JdbcLocalSessionService.Session> onAuthenticated;
    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();
    private final Label message = new Label();

    public HomeAuthenticationView(JdbcLocalSessionService sessions,
            Consumer<JdbcLocalSessionService.Session> onAuthenticated) {
        this.sessions = Objects.requireNonNull(sessions);
        this.onAuthenticated = Objects.requireNonNull(onAuthenticated);
        setSpacing(12);
        setPadding(new Insets(32));
        setMaxWidth(900);
        Label heading = new Label("Event Venue Manager");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        username.setPromptText("Username");
        password.setPromptText("Password");
        Button login = new Button("Log in");
        login.setDefaultButton(true);
        login.setOnAction(event -> login());
        Button register = new Button("Create normal user account");
        register.setOnAction(event -> register());
        message.setWrapText(true);
        message.setMaxWidth(900);
        getChildren().addAll(heading,
                new Label("Log in or create an account before entering a workspace."),
                username, password, login, register, message);
    }

    private void login() {
        try {
            onAuthenticated.accept(sessions.login(username.getText(), password.getText()));
        } catch (RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    private void register() {
        ChoiceDialog<Role> roleDialog = new ChoiceDialog<>(Role.ATTENDEE,
                List.of(Role.ATTENDEE, Role.CLUB_ORGANIZER));
        roleDialog.setTitle("Create normal user account");
        roleDialog.setHeaderText("Choose a normal user role");
        roleDialog.setContentText("Role:");
        Optional<Role> role = roleDialog.showAndWait();
        if (role.isEmpty()) return;

        Dialog<ButtonType> details = new Dialog<>();
        details.setTitle("Create normal user account");
        details.setHeaderText("Enter your account details");
        ButtonType create = new ButtonType("Create", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        details.getDialogPane().getButtonTypes().addAll(create, ButtonType.CANCEL);
        TextField newUsername = new TextField();
        PasswordField newPassword = new PasswordField();
        VBox fields = new VBox(8, new Label("Username"), newUsername,
                new Label("Password (at least 8 characters)"), newPassword);
        fields.setPadding(new Insets(12));
        details.getDialogPane().setContent(fields);
        details.setResultConverter(button -> button == create ? create : null);
        if (details.showAndWait().isEmpty()) return;
        try {
            sessions.registerNormalUser(newUsername.getText(), newPassword.getText(), role.get());
            username.setText(newUsername.getText().trim());
            password.setText("");
            message.setStyle("-fx-text-fill: #067647;");
            message.setText("Account created. Log in to continue.");
        } catch (RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    private void showError(String text) {
        message.setStyle("-fx-text-fill: #b42318;");
        message.setText(text == null ? "Authentication failed." : text);
    }
}
