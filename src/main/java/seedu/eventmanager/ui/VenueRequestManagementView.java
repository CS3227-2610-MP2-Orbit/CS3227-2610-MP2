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
import seedu.eventmanager.service.ApprovedBookingDisplay;

/** Venue request review screen for the Venue Administrator. */
public final class VenueRequestManagementView {
    private final BorderPane root = new BorderPane();
    private final TableView<VenueRequestDisplay> table = new TableView<>();
    private final TableView<ApprovedBookingDisplay> approvedTable = new TableView<>();
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
        approvedTable.setItems(FXCollections.observableArrayList(state.data().approvedBookingDisplays()));
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
        approvedTable.setPlaceholder(new Label("No approved venue bookings."));
        table.getColumns().addAll(column("Request", r -> shortId(r.requestId())),
                column("Venue", r -> r.venueName() + " — " + r.venueLocation()),
                column("Event", VenueRequestDisplay::eventTitle),
                column("Organizer", VenueRequestDisplay::organizerName),
                column("Starts", r -> r.request().startsAt().toString()),
                column("Attendance", r -> String.valueOf(r.request().expectedAttendance())));
        approvedTable.getColumns().addAll(
                bookingColumn("Booking", r -> shortId(r.bookingId())),
                bookingColumn("Venue", ApprovedBookingDisplay::venueLabel),
                bookingColumn("Event", r -> text(r.eventTitle(), "Unknown event")),
                bookingColumn("Organizer", r -> text(r.organizerName(), "Unknown organizer")),
                bookingColumn("Starts", r -> r.startsAt().toString()),
                bookingColumn("Ends", r -> r.endsAt().toString()),
                bookingColumn("Attendance", r -> String.valueOf(r.expectedAttendance())),
                bookingColumn("Status", ApprovedBookingDisplay::status));

        Button approve = new Button("Approve");
        approve.setOnAction(event -> decideApprove());
        Button reject = new Button("Reject");
        reject.setOnAction(event -> decideReject());
        HBox actions = new HBox(10, approve, reject);
        Label approvedHeading = new Label("Approved bookings");
        approvedHeading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        VBox content = new VBox(16, back, heading, table, actions, status,
                approvedHeading, approvedTable);
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

    private TableColumn<ApprovedBookingDisplay, String> bookingColumn(String title,
            java.util.function.Function<ApprovedBookingDisplay, String> value) {
        TableColumn<ApprovedBookingDisplay, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        column.setPrefWidth(160);
        return column;
    }

    private static String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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
