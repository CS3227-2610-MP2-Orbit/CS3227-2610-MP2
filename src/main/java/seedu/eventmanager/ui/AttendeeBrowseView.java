package seedu.eventmanager.ui;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.UUID;
import java.time.Clock;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
import seedu.eventmanager.attendee.CatalogueClub;
import seedu.eventmanager.attendee.AttendeeEventDetails;
import seedu.eventmanager.attendee.CatalogueQuery;
import seedu.eventmanager.attendee.EventCatalogueService;
import seedu.eventmanager.common.EntityNotFoundException;
import seedu.eventmanager.common.JavaUtilStructuredLogger;
import seedu.eventmanager.common.StructuredLogger;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.attendee.MyRegistration;
import seedu.eventmanager.attendee.AttendanceRecord;
import seedu.eventmanager.registration.Registration;
import seedu.eventmanager.registration.RegistrationEligibilityPolicy;

/** Attendee workspace. All JDBC reads and commands run outside the JavaFX thread. */
public final class AttendeeBrowseView extends BorderPane implements AutoCloseable, AttendeeBrowseController.View {
    private static final String CARD = "-fx-background-color: white; -fx-background-radius: 8;"
            + " -fx-border-color: #e2e8f0; -fx-border-radius: 8;";
    private final AttendeeBrowseController controller;
    private final StructuredLogger logger = new JavaUtilStructuredLogger(AttendeeBrowseView.class);
    private final TextField search = new TextField();
    private static final CatalogueClub ALL_CLUBS = new CatalogueClub("", "All clubs");
    private final ComboBox<CatalogueClub> club = new ComboBox<>();
    private final Label clubFeedback = text("", "#61708a", 12);
    private final DatePicker from = new DatePicker();
    private final DatePicker to = new DatePicker();
    private final ListView<CatalogueEvent> events = new ListView<>();
    private final VBox details = new VBox(12);
    private final Label feedback = text("", "#61708a", 13);
    private final Label commandFeedback = text("", "#172033", 14);
    private final VBox browseContent;
    private final MyRegistrationsView registrations;
    private final Button browseNav = new Button("Browse events");
    private final Button registrationsNav = new Button("My Registrations");
    private final Button notificationsNav = new Button("Notifications");
    private final InboxView inbox;
    private final Button historyNav = new Button("Attendance history");
    private final AttendanceHistoryView history;
    private final Clock clock;

    public AttendeeBrowseView(Supplier<EventCatalogueService> services,
            Function<UUID, AttendeeEventDetails> detailReader, AttendeeRegistrationActions actions,
            InboxActions inboxActions, Runnable onHome) {
        this(services, detailReader, actions, inboxActions, onHome, Clock.systemUTC());
    }

    AttendeeBrowseView(Supplier<EventCatalogueService> services,
            Function<UUID, AttendeeEventDetails> detailReader, AttendeeRegistrationActions actions,
            InboxActions inboxActions, Runnable onHome, Clock clock) {
        this(services, detailReader, actions, inboxActions, List::of, onHome, clock);
    }

    public AttendeeBrowseView(Supplier<EventCatalogueService> services,
            Function<UUID, AttendeeEventDetails> detailReader, AttendeeRegistrationActions actions,
            InboxActions inboxActions, Supplier<List<AttendanceRecord>> historyReader, Runnable onHome) {
        this(services, detailReader, actions, inboxActions, historyReader, onHome, Clock.systemUTC());
    }

    private AttendeeBrowseView(Supplier<EventCatalogueService> services,
            Function<UUID, AttendeeEventDetails> detailReader, AttendeeRegistrationActions actions,
            InboxActions inboxActions, Supplier<List<AttendanceRecord>> historyReader, Runnable onHome, Clock clock) {
        this.clock = Objects.requireNonNull(clock);
        Objects.requireNonNull(services);
        controller = new AttendeeBrowseController(query -> services.get().search(query),
                () -> services.get().clubs(), detailReader, actions, this);
        browseContent = content();
        registrations = new MyRegistrationsView(controller::loadRegistrations,
                row -> controller.cancelRegistration(row.eventId(), row.version()),
                row -> controller.checkIn(row.eventId(), row.version()), clock);
        inbox = new InboxView(inboxActions, count -> notificationsNav.setText(
                count < 0 ? "Notifications (?)" : "Notifications (" + count + " unread)"));
        history = new AttendanceHistoryView(historyReader);
        history.disableProperty().bind(controller.busyProperty().or(inbox.busyProperty()));
        browseContent.disableProperty().bind(controller.busyProperty().or(inbox.busyProperty()));
        registrations.disableProperty().bind(controller.busyProperty().or(inbox.busyProperty()));
        Objects.requireNonNull(onHome);
        setStyle("-fx-background-color: #f7f9fc;");
        setLeft(sidebar(() -> { close(); onHome.run(); }));
        setCenter(browseContent);
        commandFeedback.setId("attendee-command-feedback");
        commandFeedback.setPadding(new Insets(12, 24, 12, 24));
        setBottom(commandFeedback);
        refresh();
        inbox.refresh();
    }

    private VBox sidebar(Runnable onHome) {
        Label brand = text("ORBIT", "white", 16);
        Label role = text("Attendee", "#93a4bd", 13);
        Button browse = browseNav;
        browse.setMaxWidth(Double.MAX_VALUE);
        browse.setAlignment(Pos.CENTER_LEFT);
        browse.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-padding: 12;");
        browse.setId("attendee-browse-nav");
        browse.setOnAction(ignored -> {
            selectNavigation(browse); commandFeedback.setText(""); setCenter(browseContent); refresh();
        });
        browse.disableProperty().bind(controller.busyProperty().or(inbox.busyProperty()));
        Button mine = registrationsNav;
        mine.setId("attendee-registrations-nav");
        mine.setMaxWidth(Double.MAX_VALUE);
        mine.setAlignment(Pos.CENTER_LEFT);
        mine.setStyle("-fx-background-color: #24334c; -fx-text-fill: #dce4f2; -fx-padding: 12;");
        mine.disableProperty().bind(controller.busyProperty().or(inbox.busyProperty()));
        mine.setOnAction(ignored -> {
            selectNavigation(mine); commandFeedback.setText(""); setCenter(registrations); controller.loadRegistrations();
        });
        notificationsNav.setId("attendee-notifications-nav");
        notificationsNav.setMaxWidth(Double.MAX_VALUE);
        notificationsNav.setAlignment(Pos.CENTER_LEFT);
        notificationsNav.setWrapText(true);
        notificationsNav.setStyle("-fx-background-color: #24334c; -fx-text-fill: #dce4f2; -fx-padding: 12;");
        notificationsNav.disableProperty().bind(controller.busyProperty().or(inbox.busyProperty()));
        notificationsNav.setOnAction(ignored -> {
            selectNavigation(notificationsNav); commandFeedback.setText(""); setCenter(inbox); inbox.refresh();
        });
        historyNav.setId("attendee-history-nav");
        historyNav.setMaxWidth(Double.MAX_VALUE);
        historyNav.setAlignment(Pos.CENTER_LEFT);
        historyNav.setWrapText(true);
        historyNav.setStyle("-fx-background-color: #24334c; -fx-text-fill: #dce4f2; -fx-padding: 12;");
        historyNav.disableProperty().bind(controller.busyProperty().or(inbox.busyProperty()));
        historyNav.setOnAction(ignored -> {
            selectNavigation(historyNav); commandFeedback.setText(""); setCenter(history); history.refresh();
        });
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label mode = text("Browse events\nManage your bookings", "#93a4bd", 12);
        Button home = new Button("← Home");
        home.setId("attendee-home");
        home.setMaxWidth(Double.MAX_VALUE);
        home.setOnAction(ignored -> onHome.run());
        home.disableProperty().bind(controller.busyProperty().or(inbox.busyProperty()));
        VBox sidebar = new VBox(16, brand, role, browse, mine, notificationsNav, historyNav, spacer, mode, home);
        sidebar.setPadding(new Insets(24, 16, 24, 16));
        sidebar.setMinWidth(200);
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #172033;");
        return sidebar;
    }

    private void selectNavigation(Button selected) {
        if (selected != historyNav) history.suspend();
        String active = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-padding: 12;";
        String inactive = "-fx-background-color: #24334c; -fx-text-fill: #dce4f2; -fx-padding: 12;";
        for (Button button : List.of(browseNav, registrationsNav, notificationsNav, historyNav)) {
            button.setStyle(button == selected ? active : inactive);
        }
    }

    private VBox content() {
        search.setPromptText("Search title or description");
        search.setId("attendee-search-text");
        search.setOnAction(ignored -> refresh());
        club.getItems().setAll(ALL_CLUBS);
        club.setValue(ALL_CLUBS);
        club.setAccessibleText("Filter events by club");
        club.setId("attendee-club");
        clubFeedback.setId("attendee-club-feedback");
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
            club.setValue(ALL_CLUBS);
            from.setValue(null);
            to.setValue(null);
            refresh();
        });
        Button refreshDetails = new Button("Refresh details");
        refreshDetails.setId("attendee-refresh-details");
        refreshDetails.disableProperty().bind(events.getSelectionModel().selectedItemProperty().isNull());
        refreshDetails.setOnAction(ignored -> controller.loadDetails(events.getSelectionModel().getSelectedItem().id()));
        FlowPane filters = new FlowPane(12, 12,
                field("Search", search), field("Club", club),
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
                    Label clubName = text(event.clubName(), "#61708a", 12);
                    clubName.maxWidthProperty().bind(events.widthProperty().subtract(48));
                    VBox row = new VBox(6, title, clubName, time);
                    row.setPadding(new Insets(8));
                    setGraphic(row);
                }
            }
        });
        events.getSelectionModel().selectedItemProperty().addListener((ignored, previous, selected) -> {
            commandFeedback.setText("");
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
                text("Upcoming and ongoing events", "#172033", 26),
                text("Published events only. Dates and times are shown in Singapore Time.", "#61708a", 13),
                filters, clubFeedback, feedback, body);
        content.setPadding(new Insets(24));
        return content;
    }

    private void refresh() {
        controller.loadClubs();
        var selected = club.getValue();
        controller.search(new CatalogueQuery(search.getText(), selected == null ? "" : selected.id(),
                from.getValue(), to.getValue()));
    }

    @Override public void loadingClubs() {
        club.setDisable(true);
        clubFeedback.setText("Loading clubs…");
    }

    @Override public void loadedClubs(List<CatalogueClub> clubs) {
        var selected = club.getValue();
        club.getItems().setAll(ALL_CLUBS);
        club.getItems().addAll(clubs);
        var restored = selected == null || selected.id().isEmpty() ? ALL_CLUBS
                : clubs.stream().filter(value -> value.id().equals(selected.id())).findFirst()
                        .orElse(new CatalogueClub(selected.id(), null));
        // Preserve an already-selected ID if it disappears: never silently broaden the query.
        if (!club.getItems().contains(restored)) {
            var choices = new java.util.ArrayList<>(clubs);
            choices.add(restored);
            choices.sort(CatalogueClub.BY_NAME);
            club.getItems().setAll(ALL_CLUBS);
            club.getItems().addAll(choices);
        }
        club.setValue(restored);
        club.setDisable(false);
        clubFeedback.setText("");
    }

    @Override public void clubsFailed(Throwable failure) {
        club.setDisable(false);
        clubFeedback.setText("Unable to refresh clubs. Existing choices remain available; use Search / Refresh to retry.");
        logger.warn("attendee_clubs_load_failed", Map.of(
                "failureType", failure == null ? "unknown" : failure.getClass().getSimpleName()));
    }

    @Override public void searching() {
        events.getItems().clear();
        events.setPlaceholder(text("Loading events…", "#61708a", 13));
        feedback.setText("Loading events…");
    }

    @Override public void searched(List<CatalogueEvent> results) {
        events.getItems().setAll(results);
        events.setPlaceholder(text("No matching upcoming or ongoing published events.", "#61708a", 13));
        feedback.setText(results.isEmpty() ? "No matches. Try clearing filters. Draft events are not shown."
                : results.size() + " upcoming/ongoing event(s). Select one for details.");
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
        Button register = new Button(value.ownStatus().orElse(null) == Registration.Status.CANCELLED ? "Re-register" : "Register");
        register.setId("attendee-register");
        register.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-padding: 9 14;");
        register.setDisable(value.eligibility() != RegistrationEligibilityPolicy.Result.AVAILABLE
                || value.ownStatus().filter(status -> status != Registration.Status.CANCELLED).isPresent());
        register.setOnAction(ignored -> controller.register(event.id(), value.ownRegistrationVersion()));
        Button cancel = new Button("Cancel registration");
        cancel.setId("attendee-cancel");
        var now = clock.instant();
        cancel.setDisable(value.ownStatus().orElse(null) != Registration.Status.CONFIRMED || !now.isBefore(event.startsAt()));
        cancel.setOnAction(ignored -> controller.cancelRegistration(event.id(), value.ownRegistrationVersion()));
        FlowPane actions = new FlowPane(12, 12);
        // Ongoing events are visible, but never offer Register/Re-register.
        if (value.eligibility() != RegistrationEligibilityPolicy.Result.EVENT_NOT_REGISTERABLE && now.isBefore(event.startsAt())) {
            actions.getChildren().add(register);
        }
        actions.getChildren().add(cancel);
        if (value.canCheckIn() && !now.isBefore(event.startsAt()) && now.isBefore(event.endsAt())) {
            Button checkIn = new Button("Check in");
            checkIn.setId("attendee-check-in");
            checkIn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-padding: 9 14;");
            checkIn.setOnAction(ignored -> controller.checkIn(event.id(), value.ownRegistrationVersion()));
            actions.getChildren().add(checkIn);
        }
        Label checkInExplanation = text(CheckInAvailabilityText.atDisplayTime(value.checkInAvailability(),
                event.startsAt(), event.endsAt(), now), "#172033", 14);
        checkInExplanation.setId("attendee-check-in-availability");
        details.getChildren().setAll(
                text(event.title(), "#172033", 22), text("Club: " + event.clubName(), "#61708a", 13),
                text("Starts: " + SingaporeDateTimes.display(event.startsAt()), "#172033", 14),
                text("Ends: " + SingaporeDateTimes.display(event.endsAt()), "#172033", 14),
                checkInExplanation,
                text(event.description().isBlank() ? "No description provided." : event.description(), "#172033", 14),
                text(value.venue().map(v -> "Venue: " + v.name() + " · " + v.location())
                        .orElse("Venue: no current booking"), "#172033", 14),
                text(value.venue().map(v -> "Booking: " + v.bookingStatus() + " · Venue status: " + v.venueStatus())
                        .orElse("Booking: not confirmed"), "#61708a", 13),
                text("Remaining seats: " + value.remainingSeats() + " / " + event.capacity(), "#172033", 14),
                text("Your registration: " + ownStatus, "#172033", 14),
                text(availability, "#172033", 14),
                text("Availability is a snapshot, not a reserved seat. Use Refresh details for the latest information.", "#61708a", 13),
                actions);
    }

    @Override public void loadingRegistrations() { registrations.loading(); }

    @Override public void loadedRegistrations(List<MyRegistration> values) { registrations.loaded(values); }

    @Override public void registrationsFailed(Throwable failure) {
        registrations.failed(RegistrationFeedback.failure(failure));
    }

    @Override public void commandFeedback(String message) { commandFeedback.setText(message); }

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
        inbox.close();
        history.close();
    }
}
