package seedu.eventmanager.ui;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.UUID;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import seedu.eventmanager.attendee.CatalogueEvent;
import seedu.eventmanager.attendee.AttendeeEventDetails;
import seedu.eventmanager.attendee.CatalogueQuery;
import seedu.eventmanager.attendee.EventCatalogueService;
import seedu.eventmanager.common.EntityNotFoundException;
import seedu.eventmanager.common.JavaUtilStructuredLogger;
import seedu.eventmanager.common.StructuredLogger;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.common.ApplicationException;

/** Read-only Attendee workspace. All JDBC work runs outside the JavaFX thread. */
public final class AttendeeBrowseView extends BorderPane implements AutoCloseable, AttendeeBrowseController.View {
    private static final String CARD = "-fx-background-color: white; -fx-background-radius: 8;"
            + " -fx-border-color: #e2e8f0; -fx-border-radius: 8;";
    private final AttendeeBrowseController controller;
    private final StructuredLogger logger = new JavaUtilStructuredLogger(AttendeeBrowseView.class);
    private final TextField search = new TextField();
    private final TextField club = new TextField();
    private final DatePicker from = new DatePicker();
    private final DatePicker to = new DatePicker();
    private final ListView<CatalogueEvent> events = new ListView<>();
    private final VBox details = new VBox(12);
    private final Label feedback = text("", "#61708a", 13);

    public AttendeeBrowseView(Supplier<EventCatalogueService> services,
            Function<UUID, AttendeeEventDetails> detailReader, Runnable onHome) {
        Objects.requireNonNull(services);
        controller = new AttendeeBrowseController(query -> services.get().search(query), detailReader, this);
        Objects.requireNonNull(onHome);
        setStyle("-fx-background-color: #f7f9fc;");
        setLeft(sidebar(() -> { close(); onHome.run(); }));
        setCenter(content());
        refresh();
    }

    private VBox sidebar(Runnable onHome) {
        Label brand = text("EVENT VENUE\nMANAGER", "white", 16);
        Label role = text("Attendee", "#93a4bd", 13);
        Button browse = new Button("Browse events");
        browse.setMaxWidth(Double.MAX_VALUE);
        browse.setAlignment(Pos.CENTER_LEFT);
        browse.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-padding: 12;");
        browse.setOnAction(ignored -> refresh());
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label mode = text("Event catalogue\nRead-only access", "#93a4bd", 12);
        Button home = new Button("← Home");
        home.setId("attendee-home");
        home.setMaxWidth(Double.MAX_VALUE);
        home.setOnAction(ignored -> onHome.run());
        VBox sidebar = new VBox(16, brand, role, browse, spacer, mode, home);
        sidebar.setPadding(new Insets(24, 16, 24, 16));
        sidebar.setMinWidth(200);
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #172033;");
        return sidebar;
    }

    private VBox content() {
        search.setPromptText("Search title or description");
        search.setId("attendee-search-text");
        search.setOnAction(ignored -> refresh());
        club.setPromptText("Exact club ID, or leave blank");
        club.setId("attendee-club");
        club.setOnAction(ignored -> refresh());
        from.setEditable(false);
        to.setEditable(false);
        from.setId("attendee-from");
        to.setId("attendee-to");
        Button submit = new Button("Search / Refresh");
        submit.setId("attendee-search");
        submit.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-padding: 9 14;");
        submit.setOnAction(ignored -> refresh());
        Button clear = new Button("Clear filters");
        clear.setId("attendee-clear");
        clear.setOnAction(ignored -> {
            search.clear();
            club.clear();
            from.setValue(null);
            to.setValue(null);
            refresh();
        });
        Button refreshDetails = new Button("Refresh details");
        refreshDetails.setId("attendee-refresh-details");
        refreshDetails.disableProperty().bind(events.getSelectionModel().selectedItemProperty().isNull());
        refreshDetails.setOnAction(ignored -> controller.loadDetails(events.getSelectionModel().getSelectedItem().id()));
        FlowPane filters = new FlowPane(12, 12,
                field("Search", search), field("Club ID", club),
                field("From date (SGT)", from), field("To date (SGT)", to), submit, clear, refreshDetails);
        filters.setAlignment(Pos.BOTTOM_LEFT);
        feedback.setId("attendee-feedback");
        events.setId("attendee-events");
        events.setMinWidth(220);
        events.setCellFactory(ignored -> new ListCell<>() {
            @Override
            protected void updateItem(CatalogueEvent event, boolean empty) {
                super.updateItem(event, empty);
                setText(null);
                if (empty || event == null) {
                    setGraphic(null);
                } else {
                    Label title = text(event.title(), "#172033", 15);
                    title.maxWidthProperty().bind(events.widthProperty().subtract(48));
                    Label time = text(SingaporeDateTimes.display(event.startsAt()), "#61708a", 12);
                    time.maxWidthProperty().bind(events.widthProperty().subtract(48));
                    VBox row = new VBox(6, title, time);
                    row.setPadding(new Insets(8));
                    setGraphic(row);
                }
            }
        });
        events.getSelectionModel().selectedItemProperty().addListener((ignored, previous, selected) -> {
            if (selected != null) {
                controller.loadDetails(selected.id());
            } else {
                controller.clearDetails();
            }
        });
        details.setId("attendee-details");
        details.setPadding(new Insets(20));
        details.setStyle(CARD);
        ScrollPane detailScroll = new ScrollPane(details);
        detailScroll.setFitToWidth(true);
        detailScroll.setMinWidth(260);
        SplitPane body = new SplitPane(events, detailScroll);
        body.setDividerPositions(0.42);
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox content = new VBox(14, text("Attendee · Browse events", "#61708a", 13),
                text("Upcoming events", "#172033", 26),
                text("Published events only. Dates and times are shown in Singapore Time.", "#61708a", 13),
                filters, feedback, body);
        content.setPadding(new Insets(24));
        return content;
    }

    private void refresh() {
        controller.search(new CatalogueQuery(search.getText(), club.getText(), from.getValue(), to.getValue()));
    }

    @Override public void searching() {
        events.getItems().clear();
        events.setPlaceholder(text("Loading events…", "#61708a", 13));
        feedback.setText("Loading events…");
    }

    @Override public void searched(List<CatalogueEvent> results) {
        events.getItems().setAll(results);
        events.setPlaceholder(text("No matching upcoming published events.", "#61708a", 13));
        feedback.setText(results.isEmpty() ? "No matches. Try clearing filters. Draft events are not shown."
                : results.size() + " upcoming event(s). Select one for details.");
    }

    @Override public void searchFailed(Throwable failure) {
        feedback.setText(safeFailure(failure));
        events.setPlaceholder(text("Events could not be loaded. Use Search / Refresh to retry.", "#b42318", 13));
    }

    @Override public void clearedDetails() {
        showDetailMessage("Select an event to view its details.");
    }

    @Override public void loadingDetails() {
        showDetailMessage("Loading current event details…");
    }

    @Override public void detailFailed(Throwable failure) {
        showDetailMessage(safeFailure(failure));
    }

    @Override public void loadedDetails(AttendeeEventDetails value) {
        CatalogueEvent event = value.event();
        String ownStatus = value.ownStatus().map(status -> switch (status) {
            case CONFIRMED -> "Registered";
            case CANCELLED -> "Cancelled";
            case CHECKED_IN -> "Checked in";
        }).orElse("Not registered");
        String availability = switch (value.eligibility()) {
            case AVAILABLE -> "Open for registration";
            case EVENT_FULL -> "Event is full";
            case VENUE_INACTIVE -> "Registration unavailable: venue is not active";
            case VENUE_NOT_CONFIRMED -> "Registration unavailable: no confirmed booking matching this event's schedule";
            case EVENT_NOT_REGISTERABLE -> "Registration closed";
        };
        details.getChildren().setAll(
                text(event.title(), "#172033", 22), text("Club: " + event.clubId(), "#61708a", 13),
                text("Starts: " + SingaporeDateTimes.display(event.startsAt()), "#172033", 14),
                text("Ends: " + SingaporeDateTimes.display(event.endsAt()), "#172033", 14),
                text(event.description().isBlank() ? "No description provided." : event.description(), "#172033", 14),
                text(value.venue().map(v -> "Venue: " + v.name() + " · " + v.location())
                        .orElse("Venue: no current booking"), "#172033", 14),
                text(value.venue().map(v -> "Booking: " + v.bookingStatus() + " · Venue status: " + v.venueStatus())
                        .orElse("Booking: not confirmed"), "#61708a", 13),
                text("Remaining seats: " + value.remainingSeats() + " / " + event.capacity(), "#172033", 14),
                text("Your registration: " + ownStatus, "#172033", 14),
                text(availability, "#172033", 14),
                text("Availability is a snapshot, not a reserved seat. Use Refresh details for the latest information.", "#61708a", 13),
                text("Register/cancel controls will be added in the next feature.", "#61708a", 13));
    }

    private void showDetailMessage(String message) {
        details.getChildren().setAll(text(message, "#61708a", 14));
    }

    private String safeFailure(Throwable failure) {
        if (failure instanceof ApplicationException application
                && ("UNAUTHENTICATED".equals(application.code()) || "FORBIDDEN".equals(application.code()))) {
            return "Your attendee session is no longer valid. Return Home and log in again.";
        }
        if (failure instanceof ValidationException) {
            return "Check the date range: From must be on or before To.";
        }
        if (failure instanceof EntityNotFoundException) {
            return "This event is no longer available. Refresh the catalogue.";
        }
        logger.warn("attendee_catalogue_load_failed", Map.of(
                "failureType", failure == null ? "unknown" : failure.getClass().getSimpleName()));
        return "Unable to load events. Try Refresh details or Search / Refresh. If this continues, check database setup.";
    }

    private static VBox field(String label, javafx.scene.control.Control control) {
        control.setMaxWidth(Double.MAX_VALUE);
        Label caption = text(label, "#61708a", 12);
        caption.setLabelFor(control);
        VBox field = new VBox(5, caption, control);
        field.setPrefWidth(180);
        return field;
    }

    private static Label text(String value, String color, int size) {
        Label label = new Label(value);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-size: " + size + "px;");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    @Override
    public void close() {
        controller.close();
    }
}
