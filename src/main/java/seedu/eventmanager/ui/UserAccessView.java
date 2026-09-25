package seedu.eventmanager.ui;

import java.util.Objects;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.service.UserAccessRepository;

/** Local user and venue-scope management screen. */
public final class UserAccessView {
    private final BorderPane root = new BorderPane();
    private final TableView<UserAccessRepository.UserSummary> table = new TableView<>();
    private final UserAccessRepository users;

    public UserAccessView(UserAccessRepository users, Runnable showDashboard) {
        this.users = Objects.requireNonNull(users);
        Button back = new Button("← Dashboard");
        back.setOnAction(event -> showDashboard.run());
        Label heading = new Label("Users and Access");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        table.getColumns().addAll(column("Username", user -> user.username()),
                column("Role", user -> user.role().name()),
                column("Active", user -> String.valueOf(user.active())),
                column("User ID", user -> user.userId().toString()));
        Button create = new Button("Create user");
        create.setOnAction(event -> createUser());
        Button edit = new Button("Edit account");
        edit.setOnAction(event -> editUser());
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> reload());
        HBox actions = new HBox(10, create, edit, refresh);
        VBox content = new VBox(16, back, heading, actions, table);
        content.setPadding(new Insets(28));
        root.setCenter(content);
        reload();
    }

    public BorderPane root() { return root; }

    private void reload() {
        table.setItems(FXCollections.observableArrayList(users.findAllUsers()));
    }

    private TableColumn<UserAccessRepository.UserSummary, String> column(String title,
            java.util.function.Function<UserAccessRepository.UserSummary, String> value) {
        TableColumn<UserAccessRepository.UserSummary, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                value.apply(cell.getValue())));
        column.setPrefWidth(220);
        return column;
    }

    private void createUser() {
        Optional<String> username = prompt("Create user", "Username", "");
        Optional<String> password = prompt("Create user", "Password", "");
        Optional<String> role = prompt("Create user", "Role", "VENUE_ADMINISTRATOR");
        if (username.isEmpty() || password.isEmpty() || role.isEmpty()) return;
        try {
            users.createUser(username.get().trim(), password.get(), Role.valueOf(role.get().trim()));
            reload();
        } catch (RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    private void editUser() {
        UserAccessRepository.UserSummary selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a user account first.");
            return;
        }
        TextInputDialog username = new TextInputDialog(selected.username());
        username.setTitle("Edit account");
        username.setContentText("Username:");
        Optional<String> updatedUsername = username.showAndWait();
        if (updatedUsername.isEmpty()) return;
        ChoiceDialog<Role> role = new ChoiceDialog<>(selected.role(),
                java.util.List.of(Role.ATTENDEE, Role.CLUB_ORGANIZER, Role.VENUE_ADMINISTRATOR));
        role.setTitle("Edit account");
        role.setContentText("Role:");
        Optional<Role> updatedRole = role.showAndWait();
        if (updatedRole.isEmpty()) return;
        CheckBox active = new CheckBox("Account active");
        active.setSelected(selected.active());
        Alert status = new Alert(Alert.AlertType.CONFIRMATION);
        status.setTitle("Edit account");
        status.setHeaderText("Update account status");
        status.getDialogPane().setContent(active);
        if (status.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            users.updateUser(selected.userId(), updatedUsername.get(), updatedRole.get(), active.isSelected());
            reload();
        } catch (RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    private Optional<String> prompt(String title, String label, String value) {
        TextInputDialog dialog = new TextInputDialog(value);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(label + ":");
        return dialog.showAndWait();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.WARNING, message == null ? "Operation failed." : message).showAndWait();
    }
}
