package seedu.eventmanager.ui;

import java.util.List;
import java.util.Objects;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import seedu.eventmanager.venue.VenueRequestStatus;
import seedu.eventmanager.service.VenueRequestDisplay;

/** Venue request review screen for the Venue Administrator. */
public final class VenueRequestManagementView {
    private final BorderPane root = new BorderPane();
    private final TableView<VenueRequestDisplay> table = new TableView<>();
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
        List<VenueRequestDisplay> submitted = state.data().pendingRequestDisplays().stream()
                .filter(request -> request.request().status() == VenueRequestStatus.SUBMITTED)
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
        table.getColumns().addAll(column("Request", r -> shortId(r.requestId())),
                column("Venue", r -> r.venueName() + " — " + r.venueLocation()),
                column("Event", VenueRequestDisplay::eventTitle),
                column("Organizer", VenueRequestDisplay::organizerName),
                column("Starts", r -> r.request().startsAt().toString()),
                column("Attendance", r -> String.valueOf(r.request().expectedAttendance())));

        Button approve = new Button("Approve");
        approve.setOnAction(event -> decideApprove());
        Button reject = new Button("Reject");
        reject.setOnAction(event -> decideReject());
        HBox actions = new HBox(10, approve, reject);
        VBox content = new VBox(16, back, heading, table, actions, status);
        content.setPadding(new Insets(28));
        root.setCenter(content);
    }

    private TableColumn<VenueRequestDisplay, String> column(String title,
            java.util.function.Function<VenueRequestDisplay, String> value) {
        TableColumn<VenueRequestDisplay, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        column.setPrefWidth(160);
        return column;
    }

    private static String shortId(java.util.UUID id) {
        String value = id.toString().replace("-", "").toUpperCase();
        return "REQ-" + value.substring(0, 6);
    }

    private void decideApprove() {
        VenueRequestDisplay selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a venue request first.");
            return;
        }
        controller.approve(selected.requestId());
    }

    private void decideReject() {
        VenueRequestDisplay selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a venue request first.");
            return;
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Venue already booked",
                List.of("Venue already booked", "Requested capacity exceeds venue capacity"));
        dialog.setTitle("Reject venue request");
        dialog.setHeaderText("Select a reason for rejection");
        dialog.setContentText("Reason:");
        dialog.showAndWait().ifPresent(reason -> controller.reject(selected.requestId(), reason));
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.WARNING, message).showAndWait();
    }
}
