package seedu.eventmanager.ui;

import java.util.UUID;
import java.util.function.UnaryOperator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import seedu.eventmanager.event.Event;
import seedu.eventmanager.event.EventDetails;
import seedu.eventmanager.event.EventService;
import seedu.eventmanager.event.OrganizerIdentity;

/** JavaFX screen for an organizer to create and edit draft events. */
public final class OrganizerEventView extends BorderPane {
    private final EventService service;
    private final OrganizerIdentity actor;
    private final ListView<Event> events = new ListView<>();
    private final ComboBox<String> club = new ComboBox<>();
    private final TextField title = new TextField();
    private final TextArea description = new TextArea();
    private final DatePicker startDate = new DatePicker();
    private final TextField startTime = new TextField();
    private final DatePicker endDate = new DatePicker();
    private final TextField endTime = new TextField();
    private final TextField capacity = new TextField();
    private final Label feedback = new Label();
    private UUID editingEventId;
    private long editingVersion;

    public OrganizerEventView(EventService service, OrganizerIdentity actor) {
        this.service = service;
        this.actor = actor;
        club.getItems().setAll(actor.ownedClubIds().stream().sorted().toList());
        club.getSelectionModel().selectFirst();
        configureLayout();
        refreshEvents(null);
    }

    private void configureLayout() {
        setPadding(new Insets(20));

        Label heading = new Label("Club Organizer — Events");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Label identity = new Label("Organizer: " + actor.userId());
        VBox header = new VBox(4, heading, identity);
        header.setPadding(new Insets(0, 0, 16, 0));
        setTop(header);

        events.setPrefWidth(320);
        events.setPlaceholder(new Label("No events yet"));
        events.setCellFactory(ignored -> new ListCell<>() {
            @Override
            protected void updateItem(Event event, boolean empty) {
                super.updateItem(event, empty);
                setText(empty || event == null
                        ? null
                        : event.title() + "\n" + SingaporeDateTimes.display(event.startsAt()));
            }
        });
        events.getSelectionModel().selectedItemProperty()
                .addListener((ignored, previous, selected) -> loadEvent(selected));

        Button newEvent = new Button("New event");
        newEvent.setMaxWidth(Double.MAX_VALUE);
        newEvent.setOnAction(ignored -> clearForm());
        VBox left = new VBox(10, new Label("Your events"), events, newEvent);
        VBox.setVgrow(events, Priority.ALWAYS);
        left.setPadding(new Insets(0, 20, 0, 0));
        setLeft(left);

        title.setPromptText("Event title");
        description.setPromptText("Optional event description");
        description.setPrefRowCount(5);
        startDate.setPromptText("Start date");
        startTime.setPromptText("18:00");
        endDate.setPromptText("End date");
        endTime.setPromptText("20:00");
        startTime.setTextFormatter(new TextFormatter<>(timeInputFilter()));
        endTime.setTextFormatter(new TextFormatter<>(timeInputFilter()));
        capacity.setPromptText("80");

        HBox startFields = new HBox(8, startDate, startTime);
        HBox endFields = new HBox(8, endDate, endTime);
        HBox.setHgrow(startDate, Priority.ALWAYS);
        HBox.setHgrow(endDate, Priority.ALWAYS);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.addRow(0, new Label("Club"), club);
        form.addRow(1, new Label("Title"), title);
        form.addRow(2, new Label("Description"), description);
        form.addRow(3, new Label("Starts (Singapore time)"), startFields);
        form.addRow(4, new Label("Ends (Singapore time)"), endFields);
        form.addRow(5, new Label("Capacity"), capacity);
        GridPane.setHgrow(club, Priority.ALWAYS);
        GridPane.setHgrow(title, Priority.ALWAYS);
        GridPane.setHgrow(description, Priority.ALWAYS);
        GridPane.setHgrow(startFields, Priority.ALWAYS);
        GridPane.setHgrow(endFields, Priority.ALWAYS);
        GridPane.setHgrow(capacity, Priority.ALWAYS);

        Button save = new Button("Save event");
        save.setDefaultButton(true);
        save.setOnAction(ignored -> saveEvent());
        Button reset = new Button("Reset");
        reset.setOnAction(ignored -> reloadSelected());
        HBox actions = new HBox(10, save, reset);
        actions.setAlignment(Pos.CENTER_RIGHT);

        feedback.setWrapText(true);
        VBox editor = new VBox(14, new Label("Event details"), form, feedback, actions);
        VBox.setVgrow(form, Priority.ALWAYS);
        setCenter(editor);
    }

    private void saveEvent() {
        try {
            EventDetails details = EventFormParser.parse(
                    title.getText(),
                    description.getText(),
                    startDate.getValue(),
                    startTime.getText(),
                    endDate.getValue(),
                    endTime.getText(),
                    capacity.getText());
            Event saved;
            if (editingEventId == null) {
                saved = service.createEvent(actor, club.getValue(), details);
                setFeedback("Event created as a draft.", false);
            } else {
                saved = service.editEvent(actor, editingEventId, editingVersion, details);
                setFeedback("Draft event updated.", false);
            }
            refreshEvents(saved.id());
        } catch (RuntimeException exception) {
            setFeedback(exception.getMessage(), true);
        }
    }

    private void refreshEvents(UUID selectedId) {
        events.getItems().setAll(service.listEvents(actor));
        if (selectedId != null) {
            events.getItems().stream()
                    .filter(event -> event.id().equals(selectedId))
                    .findFirst()
                    .ifPresent(event -> events.getSelectionModel().select(event));
        } else if (!events.getItems().isEmpty()) {
            events.getSelectionModel().selectFirst();
        } else {
            clearForm();
        }
    }

    private void loadEvent(Event event) {
        if (event == null) {
            return;
        }
        editingEventId = event.id();
        editingVersion = event.version();
        club.setValue(event.clubId());
        club.setDisable(true);
        title.setText(event.title());
        description.setText(event.description());
        startDate.setValue(SingaporeDateTimes.dateOf(event.startsAt()));
        startTime.setText(SingaporeDateTimes.timeOf(event.startsAt()));
        endDate.setValue(SingaporeDateTimes.dateOf(event.endsAt()));
        endTime.setText(SingaporeDateTimes.timeOf(event.endsAt()));
        capacity.setText(Integer.toString(event.capacity()));
        feedback.setText("");
    }

    private void reloadSelected() {
        if (editingEventId == null) {
            clearForm();
            return;
        }
        loadEvent(service.getEvent(actor, editingEventId));
    }

    private void clearForm() {
        events.getSelectionModel().clearSelection();
        editingEventId = null;
        editingVersion = 0;
        club.setDisable(false);
        if (club.getValue() == null) {
            club.getSelectionModel().selectFirst();
        }
        title.clear();
        description.clear();
        startDate.setValue(null);
        startTime.clear();
        endDate.setValue(null);
        endTime.clear();
        capacity.clear();
        feedback.setText("");
        title.requestFocus();
    }

    private void setFeedback(String message, boolean error) {
        feedback.setText(message == null ? "Operation failed" : message);
        feedback.setStyle(error ? "-fx-text-fill: #b00020;" : "-fx-text-fill: #1b5e20;");
    }

    private static UnaryOperator<TextFormatter.Change> timeInputFilter() {
        return change -> change.getControlNewText().matches("[0-9]{0,2}:?[0-9]{0,2}")
                ? change
                : null;
    }
}
