package seedu.eventmanager.ui;

import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import seedu.eventmanager.event.Event;
import seedu.eventmanager.event.EventDetails;
import seedu.eventmanager.event.EventService;
import seedu.eventmanager.event.OrganizerIdentity;
import seedu.eventmanager.event.OrganizerVenueRequestService;
import seedu.eventmanager.service.VenueRepository;
import seedu.eventmanager.venue.Venue;
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;
import seedu.eventmanager.venue.VenueStatus;

/**
 * JavaFX screen for an organizer to create/edit draft events and submit venue requests.
 * Visual layout follows the Venue Administrator shell (dark sidebar + card content).
 */
public final class OrganizerEventView extends BorderPane {
    private static final String SIDEBAR_STYLE = "-fx-background-color: #172033;";
    private static final String NAV_BUTTON_STYLE = "-fx-background-color: transparent; -fx-text-fill: #dce4f2;"
            + " -fx-font-size: 13px; -fx-padding: 10px 12px;";
    private static final String NAV_BUTTON_ACTIVE_STYLE = NAV_BUTTON_STYLE
            + " -fx-background-color: #243044; -fx-background-radius: 6px;";
    private static final String CARD_STYLE = "-fx-background-color: white; -fx-background-radius: 8px;"
            + " -fx-border-color: #e2e8f0; -fx-border-radius: 8px;";
    private static final String PRIMARY_BUTTON_STYLE =
            "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 16px;";

    private enum Screen { EVENTS, REQUEST_VENUE }

    private final EventService service;
    private final OrganizerVenueRequestService venueRequestService;
    private final VenueRepository venueRepository;
    private final OrganizerIdentity actor;
    private final Runnable onHome;

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
    private final Label editorHeading = new Label("Create draft event");
    private final Button reset = new Button("Reset");
    private final Button eventsNav = navButton("Events");
    private final Button newEventNav = navButton("New event");
    private final Button requestVenueNav = navButton("Request venue");

    private final ListView<Event> requestEvents = new ListView<>();
    private final ComboBox<Venue> venuePicker = new ComboBox<>();
    private final Label requestFeedback = new Label();
    private final Label requestSummary = new Label();
    private final Label requestStatus = new Label();
    private final Button submitRequest = new Button("Submit request");

    private final VBox eventsContent;
    private final VBox requestContent;
    private final StackPane workspace = new StackPane();

    private UUID editingEventId;
    private long editingVersion;
    private Screen screen = Screen.EVENTS;

    public OrganizerEventView(
            EventService service,
            OrganizerVenueRequestService venueRequestService,
            VenueRepository venueRepository,
            OrganizerIdentity actor,
            Runnable onHome) {
        this.service = Objects.requireNonNull(service, "service");
        this.venueRequestService = Objects.requireNonNull(venueRequestService, "venueRequestService");
        this.venueRepository = Objects.requireNonNull(venueRepository, "venueRepository");
        this.actor = Objects.requireNonNull(actor, "actor");
        this.onHome = Objects.requireNonNull(onHome, "onHome");
        club.getItems().setAll(actor.ownedClubIds().stream().sorted().toList());
        club.getSelectionModel().selectFirst();
        setStyle("-fx-background-color: #f7f9fc;");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        eventsContent = buildEventsContent();
        requestContent = buildRequestContent();
        workspace.getChildren().setAll(eventsContent);
        configureLayout();
        refreshEvents(null);
        enterCreateMode(false);
    }

    private void configureLayout() {
        setLeft(buildSidebar());
        setCenter(workspace);
        BorderPane.setAlignment(workspace, Pos.TOP_LEFT);
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(24, 16, 24, 16));
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(200);
        sidebar.setMaxWidth(240);
        sidebar.setStyle(SIDEBAR_STYLE);
        VBox.setVgrow(sidebar, Priority.ALWAYS);

        Label brand = new Label("EVENT VENUE\nMANAGER");
        brand.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label role = new Label("Club Organizer");
        role.setStyle("-fx-text-fill: #93a4bd; -fx-font-size: 12px;");

        eventsNav.setOnAction(ignored -> {
            showEventsScreen();
            showEventsList();
        });
        newEventNav.setOnAction(ignored -> {
            showEventsScreen();
            enterCreateMode(true);
        });
        requestVenueNav.setOnAction(ignored -> showRequestVenueScreen());
        highlightNav(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label clubsHint = new Label("Clubs: " + String.join(", ", actor.ownedClubIds()));
        clubsHint.setStyle("-fx-text-fill: #93a4bd; -fx-font-size: 11px;");
        clubsHint.setWrapText(true);

        Label identity = new Label("Signed in as\n" + actor.userId());
        identity.setStyle("-fx-text-fill: #93a4bd; -fx-font-size: 11px;");
        identity.setWrapText(true);

        Button home = navButton("← Home");
        home.setOnAction(ignored -> onHome.run());

        sidebar.getChildren().addAll(
                brand, role, eventsNav, newEventNav, requestVenueNav, spacer, clubsHint, identity, home);
        return sidebar;
    }

    private VBox buildEventsContent() {
        Label pageTitle = new Label("Events");
        pageTitle.setStyle("-fx-text-fill: #61708a; -fx-font-size: 13px;");
        Label contentTitle = new Label("Club Organizer — Events");
        contentTitle.setStyle("-fx-text-fill: #172033; -fx-font-size: 24px; -fx-font-weight: bold;");

        events.setMinWidth(260);
        events.setPrefWidth(320);
        events.setMaxWidth(360);
        events.setPlaceholder(new Label("No events yet"));
        events.setStyle(CARD_STYLE);
        events.setCellFactory(ignored -> eventCell());
        events.getSelectionModel().selectedItemProperty()
                .addListener((ignored, previous, selected) -> {
                    if (selected != null && screen == Screen.EVENTS) {
                        loadEvent(selected);
                    }
                });

        VBox listCard = new VBox(12, sectionLabel("Your events"), events);
        listCard.setPadding(new Insets(18));
        listCard.setStyle(CARD_STYLE);
        listCard.setMinWidth(280);
        listCard.setPrefWidth(340);
        listCard.setMaxWidth(380);
        VBox.setVgrow(events, Priority.ALWAYS);
        HBox.setHgrow(listCard, Priority.NEVER);

        title.setPromptText("Event title");
        title.setMaxWidth(Double.MAX_VALUE);
        description.setPromptText("Optional event description");
        description.setPrefRowCount(5);
        description.setMaxWidth(Double.MAX_VALUE);
        description.setWrapText(true);
        startDate.setPromptText("Start date");
        startTime.setPromptText("18:00");
        endDate.setPromptText("End date");
        endTime.setPromptText("20:00");
        startTime.setTextFormatter(new TextFormatter<>(timeInputFilter()));
        endTime.setTextFormatter(new TextFormatter<>(timeInputFilter()));
        capacity.setPromptText("80");
        capacity.setMaxWidth(Double.MAX_VALUE);
        club.setMaxWidth(Double.MAX_VALUE);

        HBox startFields = new HBox(8, startDate, startTime);
        HBox endFields = new HBox(8, endDate, endTime);
        HBox.setHgrow(startDate, Priority.ALWAYS);
        HBox.setHgrow(endDate, Priority.ALWAYS);
        startDate.setMaxWidth(Double.MAX_VALUE);
        endDate.setMaxWidth(Double.MAX_VALUE);

        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(14);
        form.setMaxWidth(Double.MAX_VALUE);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(170);
        labels.setPrefWidth(190);
        labels.setMaxWidth(220);
        labels.setHalignment(HPos.LEFT);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        fields.setMinWidth(280);
        fields.setFillWidth(true);
        form.getColumnConstraints().addAll(labels, fields);

        form.add(fieldLabel("Club"), 0, 0);
        form.add(club, 1, 0);
        form.add(fieldLabel("Title"), 0, 1);
        form.add(title, 1, 1);
        Label descriptionLabel = fieldLabel("Description");
        form.add(descriptionLabel, 0, 2);
        form.add(description, 1, 2);
        GridPane.setValignment(descriptionLabel, VPos.TOP);
        form.add(fieldLabel("Starts (Singapore time)"), 0, 3);
        form.add(startFields, 1, 3);
        form.add(fieldLabel("Ends (Singapore time)"), 0, 4);
        form.add(endFields, 1, 4);
        form.add(fieldLabel("Capacity"), 0, 5);
        form.add(capacity, 1, 5);

        Button save = new Button("Save event");
        save.setDefaultButton(true);
        save.setStyle(PRIMARY_BUTTON_STYLE);
        save.setOnAction(ignored -> saveEvent());
        reset.setOnAction(ignored -> resetForm());
        HBox actions = new HBox(10, save, reset);
        actions.setAlignment(Pos.CENTER_RIGHT);

        feedback.setWrapText(true);
        editorHeading.setStyle("-fx-text-fill: #172033; -fx-font-size: 16px; -fx-font-weight: bold;");

        VBox editorCard = new VBox(16, editorHeading, form, feedback, actions);
        editorCard.setPadding(new Insets(22));
        editorCard.setStyle(CARD_STYLE);
        editorCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editorCard, Priority.ALWAYS);
        VBox.setVgrow(form, Priority.ALWAYS);

        HBox body = new HBox(20, listCard, editorCard);
        body.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editorCard, Priority.ALWAYS);
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox.setVgrow(listCard, Priority.ALWAYS);
        VBox.setVgrow(editorCard, Priority.ALWAYS);

        VBox content = new VBox(18, pageTitle, contentTitle, body);
        content.setPadding(new Insets(28, 32, 28, 32));
        content.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(body, Priority.ALWAYS);
        return content;
    }

    private VBox buildRequestContent() {
        Label pageTitle = new Label("Request venue");
        pageTitle.setStyle("-fx-text-fill: #61708a; -fx-font-size: 13px;");
        Label contentTitle = new Label("Club Organizer — Request venue");
        contentTitle.setStyle("-fx-text-fill: #172033; -fx-font-size: 24px; -fx-font-weight: bold;");

        requestEvents.setMinWidth(260);
        requestEvents.setPrefWidth(320);
        requestEvents.setMaxWidth(360);
        requestEvents.setPlaceholder(new Label("No events yet"));
        requestEvents.setStyle(CARD_STYLE);
        requestEvents.setCellFactory(ignored -> requestEventCell());
        requestEvents.getSelectionModel().selectedItemProperty()
                .addListener((ignored, previous, selected) -> updateRequestSummary(selected));

        VBox listCard = new VBox(12, sectionLabel("Your events"), requestEvents);
        listCard.setPadding(new Insets(18));
        listCard.setStyle(CARD_STYLE);
        listCard.setMinWidth(280);
        listCard.setPrefWidth(340);
        listCard.setMaxWidth(380);
        VBox.setVgrow(requestEvents, Priority.ALWAYS);

        venuePicker.setMaxWidth(Double.MAX_VALUE);
        venuePicker.setPromptText("Select an ACTIVE venue");
        venuePicker.setCellFactory(ignored -> venueCell());
        venuePicker.setButtonCell(venueCell());

        requestSummary.setWrapText(true);
        requestSummary.setStyle("-fx-text-fill: #526075;");
        requestStatus.setWrapText(true);
        requestStatus.setStyle("-fx-text-fill: #172033; -fx-font-weight: bold;");
        requestFeedback.setWrapText(true);

        submitRequest.setStyle(PRIMARY_BUTTON_STYLE);
        submitRequest.setOnAction(ignored -> submitVenueRequest());
        HBox actions = new HBox(submitRequest);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Label heading = new Label("Submit venue booking request");
        heading.setStyle("-fx-text-fill: #172033; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label help = new Label(
                "Uses the event's schedule and capacity. Status updates after Venue Admin decides "
                        + "(pending / approved / rejected). This screen does not auto-publish events.");
        help.setWrapText(true);
        help.setStyle("-fx-text-fill: #61708a;");

        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(14);
        form.setMaxWidth(Double.MAX_VALUE);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(170);
        labels.setPrefWidth(190);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        fields.setMinWidth(280);
        fields.setFillWidth(true);
        form.getColumnConstraints().addAll(labels, fields);
        form.add(fieldLabel("Venue"), 0, 0);
        form.add(venuePicker, 1, 0);
        form.add(fieldLabel("From event"), 0, 1);
        form.add(requestSummary, 1, 1);
        form.add(fieldLabel("Request status"), 0, 2);
        form.add(requestStatus, 1, 2);

        VBox formCard = new VBox(16, heading, help, form, requestFeedback, actions);
        formCard.setPadding(new Insets(22));
        formCard.setStyle(CARD_STYLE);
        formCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(formCard, Priority.ALWAYS);

        HBox body = new HBox(20, listCard, formCard);
        body.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox.setVgrow(listCard, Priority.ALWAYS);
        VBox.setVgrow(formCard, Priority.ALWAYS);

        VBox content = new VBox(18, pageTitle, contentTitle, body);
        content.setPadding(new Insets(28, 32, 28, 32));
        content.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(body, Priority.ALWAYS);
        return content;
    }

    private static ListCell<Event> eventCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Event event, boolean empty) {
                super.updateItem(event, empty);
                setText(empty || event == null
                        ? null
                        : event.title() + "\n" + SingaporeDateTimes.display(event.startsAt()));
            }
        };
    }

    private ListCell<Event> requestEventCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Event event, boolean empty) {
                super.updateItem(event, empty);
                if (empty || event == null) {
                    setText(null);
                    return;
                }
                String status = venueRequestService.latestRequest(actor, event.id())
                        .map(request -> request.status().name())
                        .orElse("NONE");
                setText(event.title()
                        + "\n"
                        + SingaporeDateTimes.display(event.startsAt())
                        + "\nVenue request: "
                        + status);
            }
        };
    }

    private static ListCell<Venue> venueCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Venue venue, boolean empty) {
                super.updateItem(venue, empty);
                setText(empty || venue == null
                        ? null
                        : venue.name() + " — " + venue.location() + " (cap " + venue.capacity() + ")");
            }
        };
    }

    private static Button navButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setStyle(NAV_BUTTON_STYLE);
        return button;
    }

    private static Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #61708a; -fx-font-size: 13px; -fx-font-weight: bold;");
        return label;
    }

    private static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #172033;");
        label.setWrapText(true);
        label.setMinWidth(160);
        return label;
    }

    private void showEventsScreen() {
        screen = Screen.EVENTS;
        workspace.getChildren().setAll(eventsContent);
        highlightNavForScreen();
    }

    private void showRequestVenueScreen() {
        screen = Screen.REQUEST_VENUE;
        workspace.getChildren().setAll(requestContent);
        refreshEvents(null);
        refreshVenues();
        highlightNavForScreen();
        requestFeedback.setText("");
        if (requestEvents.getSelectionModel().getSelectedItem() == null
                && !requestEvents.getItems().isEmpty()) {
            requestEvents.getSelectionModel().selectFirst();
        } else {
            updateRequestSummary(requestEvents.getSelectionModel().getSelectedItem());
        }
    }

    private void refreshVenues() {
        venuePicker.getItems().setAll(
                venueRepository.findAll().stream()
                        .filter(venue -> venue.status() == VenueStatus.ACTIVE)
                        .toList());
        if (!venuePicker.getItems().isEmpty()
                && venuePicker.getSelectionModel().getSelectedItem() == null) {
            venuePicker.getSelectionModel().selectFirst();
        }
    }

    private void updateRequestSummary(Event event) {
        if (event == null) {
            requestSummary.setText("Select an event from the list.");
            requestStatus.setText("—");
            submitRequest.setDisable(true);
            return;
        }
        requestSummary.setText(
                event.title()
                        + "\n"
                        + SingaporeDateTimes.display(event.startsAt())
                        + " → "
                        + SingaporeDateTimes.display(event.endsAt())
                        + "\nCapacity / expected attendance: "
                        + event.capacity());
        try {
            var latest = venueRequestService.latestRequest(actor, event.id());
            if (latest.isEmpty()) {
                requestStatus.setText("NONE — no venue request yet.");
                submitRequest.setDisable(false);
                return;
            }
            VenueRequest request = latest.get();
            requestStatus.setText(
                    request.status().name()
                            + "\nRequest id: "
                            + request.requestId()
                            + "\nVenue id: "
                            + request.venueId());
            // Block another submit while pending or already approved; allow after reject/withdraw.
            boolean blockSubmit = request.status() == VenueRequestStatus.SUBMITTED
                    || request.status() == VenueRequestStatus.DRAFT
                    || request.status() == VenueRequestStatus.APPROVED;
            submitRequest.setDisable(blockSubmit);
        } catch (RuntimeException exception) {
            requestStatus.setText("Could not load status: " + exception.getMessage());
            submitRequest.setDisable(true);
        }
    }

    private void submitVenueRequest() {
        Event event = requestEvents.getSelectionModel().getSelectedItem();
        Venue venue = venuePicker.getSelectionModel().getSelectedItem();
        if (event == null) {
            setRequestFeedback("Select an event first.", true);
            return;
        }
        if (venue == null) {
            setRequestFeedback("Select an ACTIVE venue.", true);
            return;
        }
        try {
            VenueRequest request = venueRequestService.submit(actor, event.id(), venue.venueId());
            setRequestFeedback(
                    "Request submitted (" + request.requestId() + "). Status is SUBMITTED (pending Admin).",
                    false);
            requestEvents.refresh();
            updateRequestSummary(event);
        } catch (RuntimeException exception) {
            setRequestFeedback(exception.getMessage(), true);
            updateRequestSummary(event);
        }
    }

    private void setRequestFeedback(String message, boolean error) {
        requestFeedback.setText(message == null ? "Operation failed" : message);
        requestFeedback.setStyle(error ? "-fx-text-fill: #b42318;" : "-fx-text-fill: #1b5e20;");
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
        java.util.List<Event> listed = service.listEvents(actor);
        events.getItems().setAll(listed);
        requestEvents.getItems().setAll(listed);
        if (selectedId != null) {
            listed.stream()
                    .filter(event -> event.id().equals(selectedId))
                    .findFirst()
                    .ifPresent(event -> {
                        events.getSelectionModel().select(event);
                        requestEvents.getSelectionModel().select(event);
                    });
        }
    }

    private void loadEvent(Event event) {
        editingEventId = event.id();
        editingVersion = event.version();
        club.setValue(event.clubId());
        club.setDisable(true);
        applyDetails(
                event.title(),
                event.description(),
                SingaporeDateTimes.dateOf(event.startsAt()),
                SingaporeDateTimes.timeOf(event.startsAt()),
                SingaporeDateTimes.dateOf(event.endsAt()),
                SingaporeDateTimes.timeOf(event.endsAt()),
                Integer.toString(event.capacity()));
        editorHeading.setText("Edit draft event");
        reset.setText("Revert changes");
        feedback.setText("");
        highlightNav(false);
    }

    private void resetForm() {
        if (editingEventId == null) {
            enterCreateMode(true);
            return;
        }
        try {
            Event saved = service.getEvent(actor, editingEventId);
            applyDetails("", "", null, "", null, "", "");
            loadEvent(saved);
            setFeedback("Reverted to the last saved draft.", false);
        } catch (RuntimeException exception) {
            setFeedback(exception.getMessage(), true);
        }
    }

    private void showEventsList() {
        if (events.getItems().isEmpty()) {
            enterCreateMode(false);
            highlightNav(false);
            setFeedback("No events yet. Use New event to create a draft.", false);
            return;
        }
        highlightNav(false);
        if (events.getSelectionModel().getSelectedItem() == null) {
            events.getSelectionModel().selectFirst();
        } else {
            loadEvent(events.getSelectionModel().getSelectedItem());
        }
    }

    private void enterCreateMode(boolean announce) {
        events.getSelectionModel().clearSelection();
        editingEventId = null;
        editingVersion = 0;
        club.setDisable(false);
        if (!club.getItems().isEmpty() && club.getSelectionModel().getSelectedItem() == null) {
            club.getSelectionModel().selectFirst();
        }
        applyDetails("", "", null, "18:00", null, "20:00", "80");
        editorHeading.setText("Create draft event");
        reset.setText("Reset");
        highlightNav(true);
        if (announce) {
            setFeedback("Ready to create a new draft.", false);
        } else {
            feedback.setText("");
        }
        title.requestFocus();
    }

    /** {@code creating == true} highlights New event; otherwise Events. */
    private void highlightNav(boolean creating) {
        if (screen != Screen.EVENTS) {
            return;
        }
        eventsNav.setStyle(creating ? NAV_BUTTON_STYLE : NAV_BUTTON_ACTIVE_STYLE);
        newEventNav.setStyle(creating ? NAV_BUTTON_ACTIVE_STYLE : NAV_BUTTON_STYLE);
        requestVenueNav.setStyle(NAV_BUTTON_STYLE);
    }

    private void highlightNavForScreen() {
        if (screen == Screen.REQUEST_VENUE) {
            eventsNav.setStyle(NAV_BUTTON_STYLE);
            newEventNav.setStyle(NAV_BUTTON_STYLE);
            requestVenueNav.setStyle(NAV_BUTTON_ACTIVE_STYLE);
            return;
        }
        boolean creating = editingEventId == null;
        highlightNav(creating);
    }

    private void applyDetails(
            String titleValue,
            String descriptionValue,
            java.time.LocalDate startDateValue,
            String startTimeValue,
            java.time.LocalDate endDateValue,
            String endTimeValue,
            String capacityValue) {
        title.setText(titleValue);
        description.setText(descriptionValue);
        startDate.setValue(startDateValue);
        startTime.setText(startTimeValue);
        endDate.setValue(endDateValue);
        endTime.setText(endTimeValue);
        capacity.setText(capacityValue);
    }

    private void setFeedback(String message, boolean error) {
        feedback.setText(message == null ? "Operation failed" : message);
        feedback.setStyle(error ? "-fx-text-fill: #b42318;" : "-fx-text-fill: #1b5e20;");
    }

    private static UnaryOperator<TextFormatter.Change> timeInputFilter() {
        return change -> change.getControlNewText().matches("[0-9]{0,2}:?[0-9]{0,2}")
                ? change
                : null;
    }
}
