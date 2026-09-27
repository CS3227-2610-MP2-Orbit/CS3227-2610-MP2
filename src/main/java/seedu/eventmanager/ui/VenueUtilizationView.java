package seedu.eventmanager.ui;

import java.time.OffsetDateTime;
import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import seedu.eventmanager.service.VenueUtilizationRepository;
import seedu.eventmanager.venue.VenueUtilization;

/** Read-only utilization report for the previous 30 days. */
public final class VenueUtilizationView {
    private final BorderPane root = new BorderPane();
    private final TableView<VenueUtilization> table = new TableView<>();
    private final VenueUtilizationRepository utilization;

    public VenueUtilizationView(VenueUtilizationRepository utilization, Runnable showDashboard) {
        this.utilization = Objects.requireNonNull(utilization);
        Button back = new Button("← Dashboard");
        back.setOnAction(event -> showDashboard.run());
        Label heading = new Label("Venue Utilization (Last 30 Days)");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        table.getColumns().addAll(column("Venue", VenueUtilization::venueName),
                column("Bookings", value -> String.valueOf(value.bookingCount())),
                column("Booked hours", value -> String.format("%.1f", value.bookedHours())),
                column("Utilization", value -> String.format("%.1f%%", value.utilizationPercentage())));
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> reload());
        VBox content = new VBox(16, back, heading,
                new HBox(10, new Label("Confirmed and at-risk bookings"), refresh), table);
        content.setPadding(new Insets(28));
        root.setCenter(content);
        reload();
    }

    public BorderPane root() { return root; }

    private void reload() {
        OffsetDateTime endsAt = OffsetDateTime.now();
        table.setItems(FXCollections.observableArrayList(
                utilization.findForWindow(endsAt.minusDays(30), endsAt)));
    }

    private TableColumn<VenueUtilization, String> column(String title,
            java.util.function.Function<VenueUtilization, String> value) {
        TableColumn<VenueUtilization, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                value.apply(cell.getValue())));
        column.setPrefWidth(190);
        return column;
    }
}
