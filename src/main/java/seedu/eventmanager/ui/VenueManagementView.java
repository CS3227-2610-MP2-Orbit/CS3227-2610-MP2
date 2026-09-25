package seedu.eventmanager.ui;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javafx.scene.control.Alert;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import seedu.eventmanager.service.VenueRepository;
import seedu.eventmanager.venue.Venue;
import seedu.eventmanager.venue.VenueStatus;

/** Initial venue management view for the Venue Administrator. */
public final class VenueManagementView {
    private final BorderPane root = new BorderPane();
    private final TableView<Venue> table = new TableView<>();
    private final VenueRepository venues;
    private final java.util.function.Consumer<UUID> onVenueCreated;

    public VenueManagementView(VenueRepository venues, Runnable showDashboard) {
        this(venues, showDashboard, venueId -> { });
    }

    public VenueManagementView(
            VenueRepository venues,
            Runnable showDashboard,
            java.util.function.Consumer<UUID> onVenueCreated) {
        this.venues = Objects.requireNonNull(venues);
        this.onVenueCreated = Objects.requireNonNull(onVenueCreated);
        Objects.requireNonNull(showDashboard);
        Button back = new Button("← Dashboard");
        back.setOnAction(event -> showDashboard.run());
        Label heading = new Label("Venues");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        table.getColumns().addAll(column("Name", venue -> venue.name()),
                column("Location", venue -> venue.location()),
                column("Capacity", venue -> String.valueOf(venue.capacity())),
                column("Status", venue -> venue.status().name()));
        Button create = new Button("Create venue");
        create.setOnAction(event -> createVenue());
        Button edit = new Button("Edit selected");
        edit.setOnAction(event -> editVenue());
        Button toggleAvailability = new Button("Toggle availability");
        toggleAvailability.setOnAction(event -> toggleAvailability());
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> reload());
        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10, create, edit, toggleAvailability, refresh);
        VBox content = new VBox(16, back, heading, actions, table);
        content.setPadding(new Insets(28));
        root.setCenter(content);
        reload();
    }

    public BorderPane root() { return root; }

    public void reload() {
        table.setItems(FXCollections.observableArrayList(venues.findAll()));
    }

    private TableColumn<Venue, String> column(String title,
            java.util.function.Function<Venue, String> value) {
        TableColumn<Venue, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                value.apply(cell.getValue())));
        column.setPrefWidth(180);
        return column;
    }

    private void createVenue() {
        Optional<String> name = prompt("Create venue", "Venue name", "");
        Optional<String> location = prompt("Create venue", "Location", "");
        Optional<String> capacity = prompt("Create venue", "Capacity", "100");
        if (name.isEmpty() || location.isEmpty() || capacity.isEmpty()) return;
        try {
            int parsedCapacity = Integer.parseInt(capacity.get().trim());
            if (name.get().isBlank() || location.get().isBlank() || parsedCapacity <= 0) {
                throw new IllegalArgumentException("Name, location, and positive capacity are required.");
            }
            UUID venueId = UUID.randomUUID();
            venues.save(new Venue(venueId, name.get().trim(), location.get().trim(),
                    parsedCapacity, null, VenueStatus.ACTIVE));
            onVenueCreated.accept(venueId);
            reload();
        } catch (RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    private void editVenue() {
        Venue selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a venue first.");
            return;
        }
        Optional<String> name = prompt("Edit venue", "Venue name", selected.name());
        Optional<String> location = prompt("Edit venue", "Location", selected.location());
        Optional<String> capacity = prompt("Edit venue", "Capacity", String.valueOf(selected.capacity()));
        if (name.isEmpty() || location.isEmpty() || capacity.isEmpty()) return;
        try {
            int parsedCapacity = Integer.parseInt(capacity.get().trim());
            if (name.get().isBlank() || location.get().isBlank() || parsedCapacity <= 0) {
                throw new IllegalArgumentException("Name, location, and positive capacity are required.");
            }
            venues.save(new Venue(selected.venueId(), name.get().trim(), location.get().trim(),
                    parsedCapacity, selected.description(), selected.status()));
            reload();
        } catch (RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    private void toggleAvailability() {
        Venue selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a venue first.");
            return;
        }
        VenueStatus nextStatus = selected.status() == VenueStatus.ACTIVE
                ? VenueStatus.INACTIVE : VenueStatus.ACTIVE;
        String action = nextStatus == VenueStatus.ACTIVE ? "activate" : "deactivate";
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Do you want to " + action + " " + selected.name() + "?");
        confirmation.showAndWait().ifPresent(button -> {
            if (button == javafx.scene.control.ButtonType.OK) {
                venues.save(new Venue(selected.venueId(), selected.name(), selected.location(),
                        selected.capacity(), selected.description(), nextStatus));
                reload();
            }
        });
    }

    private void claimAccess() {
        Venue selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a venue first.");
            return;
        }
        try {
            onVenueCreated.accept(selected.venueId());
            new Alert(Alert.AlertType.INFORMATION,
                    "Access granted for " + selected.name() + ".").showAndWait();
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
        new Alert(Alert.AlertType.WARNING, message == null ? "Invalid venue details." : message).showAndWait();
    }
}
