package seedu.eventmanager.ui;

import java.util.Objects;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import seedu.eventmanager.service.VenueRepository;
import seedu.eventmanager.venue.Venue;

/** Initial venue management view for the Venue Administrator. */
public final class VenueManagementView {
    private final BorderPane root = new BorderPane();
    private final TableView<Venue> table = new TableView<>();
    private final VenueRepository venues;

    public VenueManagementView(VenueRepository venues, Runnable showDashboard) {
        this.venues = Objects.requireNonNull(venues);
        Objects.requireNonNull(showDashboard);
        Button back = new Button("← Dashboard");
        back.setOnAction(event -> showDashboard.run());
        Label heading = new Label("Venues");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        table.getColumns().addAll(column("Name", "name"), column("Location", "location"),
                column("Capacity", "capacity"), column("Status", "status"));
        VBox content = new VBox(16, back, heading, table);
        content.setPadding(new Insets(28));
        root.setCenter(content);
        reload();
    }

    public BorderPane root() { return root; }

    public void reload() {
        table.setItems(FXCollections.observableArrayList(venues.findAll()));
    }

    private TableColumn<Venue, ?> column(String title, String property) {
        TableColumn<Venue, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(180);
        return column;
    }
}
