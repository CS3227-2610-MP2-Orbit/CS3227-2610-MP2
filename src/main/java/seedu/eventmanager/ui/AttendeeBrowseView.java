package seedu.eventmanager.ui;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import javafx.concurrent.Task;
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
import seedu.eventmanager.attendee.CatalogueQuery;
import seedu.eventmanager.attendee.EventCatalogueService;
import seedu.eventmanager.common.EntityNotFoundException;
import seedu.eventmanager.common.JavaUtilStructuredLogger;
import seedu.eventmanager.common.StructuredLogger;
import seedu.eventmanager.common.ValidationException;

/** Read-only Attendee workspace. All JDBC work runs outside the JavaFX thread. */
public final class AttendeeBrowseView extends BorderPane implements AutoCloseable {
    private static final String CARD = "-fx-background-color: white; -fx-background-radius: 8;"
            + " -fx-border-color: #e2e8f0; -fx-border-radius: 8;";
    private final Supplier<EventCatalogueService> services;
    private final StructuredLogger logger = new JavaUtilStructuredLogger(AttendeeBrowseView.class);
    private final TextField search = new TextField();
    private final TextField club = new TextField();
    private final DatePicker from = new DatePicker();
    private final DatePicker to = new DatePicker();
    private final ListView<CatalogueEvent> events = new ListView<>();
    private final VBox details = new VBox(12);
    private final Label feedback = text("", "#61708a", 13);
    private Task<List<CatalogueEvent>> searchTask;
    private Task<CatalogueEvent> detailTask;
    private boolean closed;

    public AttendeeBrowseView(Supplier<EventCatalogueService> services, Runnable onHome) {
        this.services = Objects.requireNonNull(services);
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
        Label mode = text("Public catalogue\nRead-only access", "#93a4bd", 12);
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
        FlowPane filters = new FlowPane(12, 12,
                field("Search", search), field("Club ID", club),
                field("From date (SGT)", from), field("To date (SGT)", to), submit, clear);
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
                loadDetails(selected);
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
        if (closed) {
            return;
        }
        cancel(searchTask);
        cancel(detailTask);
        detailTask = null;
        events.getItems().clear();
        showDetailMessage("Select an event to view its details.");
        events.setPlaceholder(text("Loading events…", "#61708a", 13));
        feedback.setText("Loading events…");
        CatalogueQuery query = new CatalogueQuery(search.getText(), club.getText(), from.getValue(), to.getValue());
        Task<List<CatalogueEvent>> task = new Task<>() {
            @Override
            protected List<CatalogueEvent> call() {
                return services.get().search(query);
            }
        };
        searchTask = task;
        task.setOnSucceeded(ignored -> {
            if (closed || searchTask != task) {
                return;
            }
            events.getItems().setAll(task.getValue());
            events.setPlaceholder(text("No matching upcoming published events.", "#61708a", 13));
            feedback.setText(task.getValue().isEmpty()
                    ? "No matches. Try clearing filters. Draft events are not shown."
                    : task.getValue().size() + " upcoming event(s). Select one for details.");
        });
        task.setOnFailed(ignored -> {
            if (!closed && searchTask == task) {
                feedback.setText(safeFailure(task.getException()));
                events.setPlaceholder(text("Events could not be loaded. Use Search / Refresh to retry.", "#b42318", 13));
            }
        });
        Thread.ofVirtual().name("attendee-catalogue-search").start(task);
    }

    private void loadDetails(CatalogueEvent selected) {
        cancel(detailTask);
        showDetailMessage("Loading current event details…");
        Task<CatalogueEvent> task = new Task<>() {
            @Override
            protected CatalogueEvent call() {
                return services.get().getEvent(selected.id());
            }
        };
        detailTask = task;
        task.setOnSucceeded(ignored -> {
            if (!closed && detailTask == task) {
                showDetails(task.getValue());
            }
        });
        task.setOnFailed(ignored -> {
            if (!closed && detailTask == task) {
                showDetailMessage(safeFailure(task.getException()));
            }
        });
        Thread.ofVirtual().name("attendee-catalogue-details").start(task);
    }

    private void showDetails(CatalogueEvent event) {
        details.getChildren().setAll(
                text(event.title(), "#172033", 22), text("Club: " + event.clubId(), "#61708a", 13),
                text("Starts: " + SingaporeDateTimes.display(event.startsAt()), "#172033", 14),
                text("Ends: " + SingaporeDateTimes.display(event.endsAt()), "#172033", 14),
                text(event.description().isBlank() ? "No description provided." : event.description(), "#172033", 14),
                text("Event capacity: " + event.capacity() + " (not remaining seats)", "#61708a", 13),
                text("Venue: booking details are not connected yet.", "#61708a", 13),
                text("Registration, personal notifications, check-in and attendance history are not available yet.",
                        "#61708a", 13));
    }

    private void showDetailMessage(String message) {
        details.getChildren().setAll(text(message, "#61708a", 14));
    }

    private String safeFailure(Throwable failure) {
        if (failure instanceof ValidationException) {
            return "Check the date range: From must be on or before To.";
        }
        if (failure instanceof EntityNotFoundException) {
            return "This event is no longer available. Refresh the catalogue.";
        }
        logger.warn("attendee_catalogue_load_failed", Map.of(
                "failureType", failure == null ? "unknown" : failure.getClass().getSimpleName()));
        return "Unable to load events. Check database setup and try Search / Refresh."
                + " The Organizer event schema must already be initialized.";
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

    private static void cancel(Task<?> task) {
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public void close() {
        closed = true;
        cancel(searchTask);
        cancel(detailTask);
    }
}
