package seedu.eventmanager.ui;

import java.util.List;
import java.time.Clock;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import seedu.eventmanager.attendee.MyRegistration;
import seedu.eventmanager.attendee.RegistrationListQuery;
import seedu.eventmanager.attendee.RegistrationListQuery.Filter;
import seedu.eventmanager.attendee.RegistrationListQuery.Order;

/** Owner's booking list; the existing Attendee workspace supplies the only navigation chrome. */
final class MyRegistrationsView extends VBox {
    private final ListView<MyRegistration> rows = new ListView<>();
    private final Label feedback = label("");
    private final Button cancel = new Button("Cancel selected registration");
    private final Button checkIn = new Button("Check in");
    private final ComboBox<Filter> filter = new ComboBox<>();
    private final ComboBox<Order> order = new ComboBox<>();
    private final VBox details = new VBox(14);
    private final Clock clock;
    private List<MyRegistration> allRows = List.of();

    MyRegistrationsView(Runnable refresh, Consumer<MyRegistration> cancelAction, Consumer<MyRegistration> checkInAction) {
        this(refresh, cancelAction, checkInAction, Clock.systemUTC());
    }

    MyRegistrationsView(Runnable refresh, Consumer<MyRegistration> cancelAction, Consumer<MyRegistration> checkInAction, Clock clock) {
        super(14);
        this.clock = Objects.requireNonNull(clock);
        setPadding(new Insets(24));
        Label heading = label("My Registrations");
        heading.setStyle("-fx-text-fill: #172033; -fx-font-size: 26px;");
        Button reload = new Button("Refresh registrations");
        reload.setId("attendee-refresh-registrations");
        reload.setOnAction(ignored -> refresh.run());
        cancel.setId("attendee-cancel-registration");
        cancel.setDisable(true);
        checkIn.setId("attendee-check-in-registration");
        checkIn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-padding: 9 14;");
        checkIn.setVisible(false);
        checkIn.managedProperty().bind(checkIn.visibleProperty());
        checkIn.setOnAction(ignored -> {
            var selected = rows.getSelectionModel().getSelectedItem();
            if (selected != null) checkInAction.accept(selected);
        });
        cancel.setOnAction(ignored -> {
            var selected = rows.getSelectionModel().getSelectedItem();
            if (selected != null) cancelAction.accept(selected);
        });
        rows.setId("attendee-registrations");
        rows.getSelectionModel().selectedItemProperty().addListener((ignored, previous, selected) -> {
            cancel.setDisable(selected == null || !selected.canCancel() || !clock.instant().isBefore(selected.startsAt()));
            checkIn.setVisible(selected != null && selected.canCheckIn()
                    && !clock.instant().isBefore(selected.startsAt()) && clock.instant().isBefore(selected.endsAt()));
            if (selected == null) clearDetails();
            else showDetails(selected);
        });
        rows.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(MyRegistration row, boolean empty) {
                super.updateItem(row, empty);
                setText(null);
                if (empty || row == null) { setGraphic(null); return; }
                Label title = label(row.title());
                title.setStyle("-fx-text-fill: #172033; -fx-font-size: 16px; -fx-font-weight: bold;");
                Label info = label(SingaporeDateTimes.display(row.startsAt()) + "\nVenue: "
                        + (row.venue().isBlank() ? "No booking recorded" : row.venue())
                        + "\nClub: " + row.clubName()
                        + "\nStatus: " + row.status().name().replace('_', ' '));
                title.maxWidthProperty().bind(rows.widthProperty().subtract(56));
                info.maxWidthProperty().bind(rows.widthProperty().subtract(56));
                VBox card = new VBox(6, title, info);
                card.setPadding(new Insets(10));
                setGraphic(card);
            }
        });
        feedback.setId("attendee-registrations-feedback");
        filter.setId("attendee-registration-filter");
        filter.getItems().setAll(Filter.values());
        filter.setValue(Filter.ALL);
        filter.setAccessibleText("Filter registrations");
        order.setId("attendee-registration-order");
        order.getItems().setAll(Order.values());
        order.setValue(Order.EARLIEST);
        order.setAccessibleText("Sort registrations by start time");
        filter.setOnAction(ignored -> applyFilters());
        order.setOnAction(ignored -> applyFilters());
        details.setId("attendee-registration-details");
        details.setPadding(new Insets(20));
        details.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-background-radius: 8;");
        ScrollPane scroll = new ScrollPane(details);
        scroll.setFitToWidth(true);
        scroll.setMinWidth(260);
        rows.setMinWidth(220);
        SplitPane body = new SplitPane(rows, scroll);
        body.setId("attendee-registration-split");
        body.setDividerPositions(0.42);
        VBox.setVgrow(body, Priority.ALWAYS);
        clearDetails();
        getChildren().setAll(heading, label("Your bookings: filter by event timing or cancellation. Times are in Singapore Time."),
                label("Only confirmed bookings before the event starts can be cancelled. To re-register, use Browse events."),
                new FlowPane(12, 12, label("Show"), filter, label("Sort by"), order, reload, cancel, checkIn), feedback, body);
    }

    void loading() {
        allRows = List.of();
        rows.getItems().clear();
        clearDetails();
        filter.setDisable(true);
        order.setDisable(true);
        feedback.setText("Loading your registrations…");
        rows.setPlaceholder(label("Loading…"));
    }

    void loaded(List<MyRegistration> values) {
        allRows = List.copyOf(values);
        filter.setDisable(false);
        order.setDisable(false);
        applyFilters();
    }

    void failed(String message) {
        allRows = List.of();
        rows.getItems().clear();
        clearDetails();
        filter.setDisable(true);
        order.setDisable(true);
        feedback.setText(message);
        rows.setPlaceholder(label("Registrations could not be loaded. Use Refresh registrations to retry."));
    }

    private void applyFilters() {
        if (filter.isDisabled()) return;
        rows.getSelectionModel().clearSelection();
        clearDetails();
        var visible = RegistrationListQuery.apply(allRows, filter.getValue(), order.getValue(), clock.instant());
        rows.getItems().setAll(visible);
        feedback.setText(visible.size() + " registration(s) shown of " + allRows.size()
                + ". Select a booking to view its details.");
        rows.setPlaceholder(label(allRows.isEmpty() ? "No registrations yet. Browse events to find one to join."
                : "No bookings in this filter. Choose All to see your other registrations."));
    }

    private void clearDetails() {
        details.getChildren().setAll(label("Select a booking to view its event details."));
        cancel.setDisable(true);
        checkIn.setVisible(false);
    }

    private void showDetails(MyRegistration row) {
        Label title = label(row.title());
        title.setStyle("-fx-text-fill: #172033; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label checkInExplanation = label(CheckInAvailabilityText.atDisplayTime(row.checkInAvailability(),
                row.startsAt(), row.endsAt(), clock.instant()));
        checkInExplanation.setId("attendee-registration-check-in-availability");
        details.getChildren().setAll(title, label("Club: " + row.clubName()),
                label("Starts: " + SingaporeDateTimes.display(row.startsAt())),
                label("Ends: " + SingaporeDateTimes.display(row.endsAt())),
                checkInExplanation,
                label("Venue: " + (row.venue().isBlank() ? "No booking recorded" : row.venue())),
                label("Event status: " + row.eventStatus()),
                label("Your registration: " + row.status().name().replace('_', ' ')),
                label(row.description().isBlank() ? "No description provided." : row.description()),
                label("Details reflect the latest loaded event information. Use Refresh registrations for updates."));
    }

    private static Label label(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #61708a; -fx-font-size: 13px;");
        return label;
    }
}
