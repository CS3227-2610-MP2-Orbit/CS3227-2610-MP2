package seedu.eventmanager.ui;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
import seedu.eventmanager.service.VenueAvailabilityRepository;
import seedu.eventmanager.venue.VenueAvailability;
import seedu.eventmanager.venue.VenueAvailabilityType;

/** Availability and blocked-period screen for venue administrators. */
public final class VenueAvailabilityView {
    private final BorderPane root = new BorderPane();
    private final TableView<VenueAvailability> table = new TableView<>();
    private final VenueAvailabilityRepository availability;

    public VenueAvailabilityView(VenueAvailabilityRepository availability, Runnable showDashboard) {
        this.availability = Objects.requireNonNull(availability);
        Button back = new Button("← Dashboard");
        back.setOnAction(event -> showDashboard.run());
        Label heading = new Label("Availability and Schedules");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        table.getColumns().addAll(column("Venue", value -> value.venueId().toString()),
                column("Type", value -> value.type().name()),
                column("Starts", value -> value.startsAt().toString()),
                column("Ends", value -> value.endsAt().toString()),
                column("Reason", VenueAvailability::reason));
        Button block = new Button("Block time");
        block.setOnAction(event -> blockTime());
        Button edit = new Button("Edit selected");
        edit.setOnAction(event -> editSelected());
        Button delete = new Button("Delete selected");
        delete.setOnAction(event -> deleteSelected());
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> reload());
        HBox actions = new HBox(10, block, edit, delete, refresh);
        VBox content = new VBox(16, back, heading, actions, table);
        content.setPadding(new Insets(28));
        root.setCenter(content);
        reload();
    }

    public BorderPane root() { return root; }

    private void reload() {
        table.setItems(FXCollections.observableArrayList(availability.findAll()));
    }

    private TableColumn<VenueAvailability, String> column(String title,
            java.util.function.Function<VenueAvailability, String> value) {
        TableColumn<VenueAvailability, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                value.apply(cell.getValue())));
        column.setPrefWidth(190);
        return column;
    }

    private void blockTime() {
        Optional<String> venue = prompt("Block venue time", "Venue ID", "");
        Optional<String> starts = prompt("Block venue time", "Start (ISO-8601)", "2030-01-01T10:00:00Z");
        Optional<String> ends = prompt("Block venue time", "End (ISO-8601)", "2030-01-01T12:00:00Z");
        Optional<String> reason = prompt("Block venue time", "Reason", "Maintenance");
        if (venue.isEmpty() || starts.isEmpty() || ends.isEmpty() || reason.isEmpty()) return;
        try {
            OffsetDateTime start = OffsetDateTime.parse(starts.get().trim());
            OffsetDateTime end = OffsetDateTime.parse(ends.get().trim());
            saveValidated(UUID.randomUUID(), UUID.fromString(venue.get().trim()), start, end, reason.get());
            reload();
        } catch (RuntimeException exception) {
            new Alert(Alert.AlertType.WARNING, "Invalid availability: " + exception.getMessage()).showAndWait();
        }
    }

    private void editSelected() {
        VenueAvailability selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select an availability record to edit.");
            return;
        }
        Optional<String> starts = prompt("Edit availability", "Start (ISO-8601)", selected.startsAt().toString());
        Optional<String> ends = prompt("Edit availability", "End (ISO-8601)", selected.endsAt().toString());
        Optional<String> reason = prompt("Edit availability", "Reason", selected.reason());
        if (starts.isEmpty() || ends.isEmpty() || reason.isEmpty()) return;
        try {
            saveValidated(selected.availabilityId(), selected.venueId(),
                    OffsetDateTime.parse(starts.get().trim()), OffsetDateTime.parse(ends.get().trim()), reason.get());
            reload();
        } catch (RuntimeException exception) {
            showWarning("Invalid availability: " + exception.getMessage());
        }
    }

    private void deleteSelected() {
        VenueAvailability selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select an availability record to delete.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete the selected availability record?", javafx.scene.control.ButtonType.OK,
                javafx.scene.control.ButtonType.CANCEL);
        confirmation.showAndWait().filter(javafx.scene.control.ButtonType.OK::equals).ifPresent(button -> {
            availability.delete(selected.availabilityId());
            reload();
        });
    }

    private void saveValidated(UUID availabilityId, UUID venueId, OffsetDateTime start,
            OffsetDateTime end, String reason) {
        if (!end.isAfter(start) || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("End must be after start and a reason is required.");
        }
        availability.save(new VenueAvailability(availabilityId, venueId, VenueAvailabilityType.BLOCKED,
                start, end, reason.trim()));
    }

    private void showWarning(String message) {
        new Alert(Alert.AlertType.WARNING, message).showAndWait();
    }

    private Optional<String> prompt(String title, String label, String value) {
        TextInputDialog dialog = new TextInputDialog(value);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(label + ":");
        return dialog.showAndWait();
    }
}
