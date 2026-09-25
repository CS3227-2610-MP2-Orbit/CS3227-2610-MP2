package seedu.eventmanager.ui;

import java.util.List;
import java.util.Objects;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;

/** Venue request review screen for the Venue Administrator. */
public final class VenueRequestManagementView {
    private final BorderPane root = new BorderPane();
    private final TableView<VenueRequest> table = new TableView<>();
    private final Label status = new Label();
    private final VenueAdministratorDashboardController controller;
    private final Runnable showDashboard;

    public VenueRequestManagementView(VenueAdministratorDashboardController controller) {
        this(controller, () -> { });
    }

    public VenueRequestManagementView(VenueAdministratorDashboardController controller,
            Runnable showDashboard) {
        this.controller = Objects.requireNonNull(controller);
        this.showDashboard = Objects.requireNonNull(showDashboard);
        build();
        controller.load();
    }

    public BorderPane root() {
        return root;
    }

    public void update(VenueAdministratorDashboardState state) {
        if (state.status() == VenueAdministratorDashboardState.Status.ERROR) {
            status.setText(state.message());
            return;
        }
        List<VenueRequest> submitted = state.data().pendingRequests().stream()
                .filter(request -> request.status() == VenueRequestStatus.SUBMITTED)
                .toList();
        table.setItems(FXCollections.observableArrayList(submitted));
        table.getSelectionModel().clearSelection();
        status.setText(state.status() == VenueAdministratorDashboardState.Status.ERROR
                ? state.message() : submitted.size() + " submitted request(s)");
    }

    private void build() {
        Label heading = new Label("Venue Requests");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Button back = new Button("← Dashboard");
        back.setOnAction(event -> showDashboard.run());
        table.setPlaceholder(new Label("No submitted venue requests."));
        table.getColumns().addAll(column("Request", r -> r.requestId().toString()),
                column("Venue", r -> r.venueId().toString()),
                column("Event", r -> r.eventId().toString()),
                column("Organizer", r -> r.organizerId().toString()),
                column("Starts", r -> r.startsAt().toString()),
                column("Attendance", r -> String.valueOf(r.expectedAttendance())));

        Button approve = new Button("Approve");
        approve.setOnAction(event -> decideApprove());
        Button reject = new Button("Reject");
        reject.setOnAction(event -> decideReject());
        HBox actions = new HBox(10, approve, reject);
        VBox content = new VBox(16, back, heading, table, actions, status);
        content.setPadding(new Insets(28));
        root.setCenter(content);
    }

    private TableColumn<VenueRequest, String> column(String title,
            java.util.function.Function<VenueRequest, String> value) {
        TableColumn<VenueRequest, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        column.setPrefWidth(160);
        return column;
    }

    private void decideApprove() {
        VenueRequest selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a venue request first.");
            return;
        }
        controller.approve(selected.requestId());
    }

    private void decideReject() {
        VenueRequest selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a venue request first.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject venue request");
        dialog.setHeaderText("Provide a reason for rejection");
        dialog.setContentText("Reason:");
        dialog.showAndWait().ifPresent(reason -> controller.reject(selected.requestId(), reason));
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.WARNING, message).showAndWait();
    }
}
