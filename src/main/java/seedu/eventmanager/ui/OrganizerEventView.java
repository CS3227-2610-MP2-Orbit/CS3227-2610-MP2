package seedu.eventmanager.ui;

import java.util.HashMap;
import java.util.Map;
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
import javafx.util.StringConverter;
import seedu.eventmanager.club.Club;
import seedu.eventmanager.club.ClubService;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.event.CapacityUpdateResult;
import seedu.eventmanager.announcement.Announcement;
import seedu.eventmanager.announcement.AnnouncementResult;
import seedu.eventmanager.announcement.AnnouncementService;
import seedu.eventmanager.event.Event;
import seedu.eventmanager.event.EventDetails;
import seedu.eventmanager.event.EventService;
import seedu.eventmanager.event.OrganizerIdentity;
import seedu.eventmanager.event.OrganizerVenueRequestService;
import seedu.eventmanager.event.RegistrationOverview;
import seedu.eventmanager.event.RegistrationOverviewService;
import seedu.eventmanager.registration.RegisteredAttendee;
import seedu.eventmanager.service.VenueRepository;
import seedu.eventmanager.venue.Venue;
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;
import seedu.eventmanager.venue.VenueStatus;
import seedu.eventmanager.volunteer.AssignedVolunteer;
import seedu.eventmanager.volunteer.VolunteerService;

/**
 * JavaFX screen for an organizer to create/edit draft events, submit venue requests,
 * assign volunteers, view registrations, post announcements, and create clubs. Visual layout follows the Venue Administrator shell
 * (dark sidebar + card content).
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

    private static final String SECONDARY_BUTTON_STYLE =
            "-fx-background-color: white; -fx-text-fill: #b42318; -fx-border-color: #e2e8f0;"
                    + " -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 8px 16px;";

    private enum Screen { EVENTS, REQUEST_VENUE, VOLUNTEERS, REGISTRATIONS, ANNOUNCEMENTS, CLUBS }

    private final EventService service;
    private final OrganizerVenueRequestService venueRequestService;
    private final VolunteerService volunteerService;
    private final RegistrationOverviewService registrationService;
    private final AnnouncementService announcementService;
    private final VenueRepository venueRepository;
    private final ClubService clubService;
    private final Actor account;
    private OrganizerIdentity actor;
    private final Map<String, String> clubNames = new HashMap<>();
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
    private final Button newEvent = new Button("+ New event");
    private final Button requestVenueNav = navButton("Request venue");
    private final Button volunteersNav = navButton("Volunteers");
    private final Button registrationsNav = navButton("Registrations");
    private final Button announcementsNav = navButton("Announcements");
    private final Button clubsNav = navButton("Clubs");
    private final Label clubsHint = new Label();

    private final ListView<Club> myClubs = new ListView<>();
    private final TextField clubName = new TextField();
    private final Button createClub = new Button("Create club");
    private final Label clubFeedback = new Label();
    private final Label noClubNote = new Label();

    private final ListView<Event> requestEvents = new ListView<>();
    private final ComboBox<Venue> venuePicker = new ComboBox<>();
    private final Label requestFeedback = new Label();
    private final Label requestSummary = new Label();
    private final Label requestStatus = new Label();
    private final Button submitRequest = new Button("Submit request");

    private final ListView<Event> volunteerEvents = new ListView<>();
    private final Label volunteerHeading = new Label("Volunteers");
    private final ListView<AssignedVolunteer> assignedVolunteers = new ListView<>();
    private final ComboBox<RegisteredAttendee> attendeePicker = new ComboBox<>();
    private final TextField volunteerRole = new TextField();
    private final Label availabilityNote = new Label();
    private final Label volunteerFeedback = new Label();
    private final Button assignVolunteer = new Button("Assign volunteer");
    private final Button removeVolunteer = new Button("Remove selected");

    private final ListView<Event> registrationEvents = new ListView<>();
    private final Label registrationHeading = new Label("Registrations");
    private final Label registrationCount = new Label();
    private final ListView<RegisteredAttendee> registrants = new ListView<>();
    private final Label registrationFeedback = new Label();

    private final ListView<Event> announcementEvents = new ListView<>();
    private final Label announcementHeading = new Label("Announcements");
    private final ListView<Announcement> announcementHistory = new ListView<>();
    private final TextArea announcementMessage = new TextArea();
    private final Label announcementCounter = new Label();
    private final Label announcementFeedback = new Label();
    private final Button sendAnnouncement = new Button("Send announcement");

    private final VBox eventsContent;
    private final VBox requestContent;
    private final VBox volunteerContent;
    private final VBox registrationContent;
    private final VBox announcementContent;
    private final VBox clubContent;
    private final StackPane workspace = new StackPane();

    private UUID editingEventId;
    private long editingVersion;
    private Screen screen = Screen.EVENTS;

    public OrganizerEventView(
            EventService service,
            OrganizerVenueRequestService venueRequestService,
            VolunteerService volunteerService,
            RegistrationOverviewService registrationService,
            AnnouncementService announcementService,
            ClubService clubService,
            VenueRepository venueRepository,
            Actor account,
            Runnable onHome) {
        this.service = Objects.requireNonNull(service, "service");
        this.venueRequestService = Objects.requireNonNull(venueRequestService, "venueRequestService");
        this.volunteerService = Objects.requireNonNull(volunteerService, "volunteerService");
        this.registrationService = Objects.requireNonNull(registrationService, "registrationService");
        this.announcementService = Objects.requireNonNull(announcementService, "announcementService");
        this.clubService = Objects.requireNonNull(clubService, "clubService");
        this.venueRepository = Objects.requireNonNull(venueRepository, "venueRepository");
        this.account = Objects.requireNonNull(account, "account");
        this.onHome = Objects.requireNonNull(onHome, "onHome");
        club.setConverter(new StringConverter<>() {
            @Override
            public String toString(String clubId) {
                return clubId == null ? "" : clubNames.getOrDefault(clubId, clubId);
            }

            @Override
            public String fromString(String text) {
                return text;
            }
        });
        reloadClubs();
        setStyle("-fx-background-color: #f7f9fc;");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        eventsContent = buildEventsContent();
        requestContent = buildRequestContent();
        volunteerContent = buildVolunteerContent();
        registrationContent = buildRegistrationContent();
        announcementContent = buildAnnouncementContent();
        clubContent = buildClubContent();
        workspace.getChildren().setAll(eventsContent);
        configureLayout();
        refreshEvents(null);
        enterCreateMode(false);
    }

    /** Re-reads this account's clubs so ownership always comes from the database, not local settings. */
    private void reloadClubs() {
        actor = clubService.identityFor(account);
        java.util.List<Club> owned = clubService.myClubs(account);
        clubNames.clear();
        owned.forEach(ownedClub -> clubNames.put(ownedClub.id().toString(), ownedClub.name()));
        String selected = club.getValue();
        club.getItems().setAll(owned.stream().map(ownedClub -> ownedClub.id().toString()).toList());
        if (selected != null && club.getItems().contains(selected)) {
            club.setValue(selected);
        } else {
            club.getSelectionModel().selectFirst();
        }
        myClubs.getItems().setAll(owned);
        clubsHint.setText(owned.isEmpty()
                ? "No clubs yet"
                : "Clubs: " + String.join(", ", owned.stream().map(Club::name).toList()));
        noClubNote.setText(owned.isEmpty()
                ? "You don't own a club yet. Create one under Clubs before creating events."
                : "");
        noClubNote.setVisible(owned.isEmpty());
        noClubNote.setManaged(owned.isEmpty());
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
        requestVenueNav.setOnAction(ignored -> showRequestVenueScreen());
        volunteersNav.setOnAction(ignored -> showVolunteersScreen());
        registrationsNav.setOnAction(ignored -> showRegistrationsScreen());
        announcementsNav.setOnAction(ignored -> showAnnouncementsScreen());
        clubsNav.setOnAction(ignored -> showClubsScreen());
        highlightNavForScreen();

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        clubsHint.setStyle("-fx-text-fill: #93a4bd; -fx-font-size: 11px;");
        clubsHint.setWrapText(true);

        Button home = navButton("← Home");
        home.setOnAction(ignored -> onHome.run());

        sidebar.getChildren().addAll(
                brand, role, eventsNav, requestVenueNav, volunteersNav, registrationsNav, announcementsNav,
                clubsNav, spacer, clubsHint, home);
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

        newEvent.setStyle(PRIMARY_BUTTON_STYLE);
        newEvent.setOnAction(ignored -> enterCreateMode(true));
        Region listHeaderSpacer = new Region();
        HBox.setHgrow(listHeaderSpacer, Priority.ALWAYS);
        HBox listHeader = new HBox(8, sectionLabel("Your events"), listHeaderSpacer, newEvent);
        listHeader.setAlignment(Pos.CENTER_LEFT);

        VBox listCard = new VBox(12, listHeader, events);
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

        noClubNote.setWrapText(true);
        noClubNote.setStyle("-fx-text-fill: #b42318;");
        VBox editorCard = new VBox(16, editorHeading, noClubNote, form, feedback, actions);
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

    private VBox buildVolunteerContent() {
        Label pageTitle = new Label("Volunteers");
        pageTitle.setStyle("-fx-text-fill: #61708a; -fx-font-size: 13px;");
        Label contentTitle = new Label("Club Organizer — Volunteers");
        contentTitle.setStyle("-fx-text-fill: #172033; -fx-font-size: 24px; -fx-font-weight: bold;");

        volunteerEvents.setMinWidth(260);
        volunteerEvents.setPrefWidth(320);
        volunteerEvents.setMaxWidth(360);
        volunteerEvents.setPlaceholder(new Label("No events yet"));
        volunteerEvents.setStyle(CARD_STYLE);
        volunteerEvents.setCellFactory(ignored -> eventCell());
        volunteerEvents.getSelectionModel().selectedItemProperty()
                .addListener((ignored, previous, selected) -> {
                    volunteerFeedback.setText("");
                    loadVolunteers(selected);
                });

        VBox listCard = new VBox(12, sectionLabel("Your events"), volunteerEvents);
        listCard.setPadding(new Insets(18));
        listCard.setStyle(CARD_STYLE);
        listCard.setMinWidth(280);
        listCard.setPrefWidth(340);
        listCard.setMaxWidth(380);
        VBox.setVgrow(volunteerEvents, Priority.ALWAYS);

        volunteerHeading.setStyle("-fx-text-fill: #172033; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label help = new Label(
                "Only attendees registered for this event can be assigned. "
                        + "A role is optional (for example Usher or Registration desk).");
        help.setWrapText(true);
        help.setStyle("-fx-text-fill: #61708a;");

        assignedVolunteers.setPlaceholder(new Label("No volunteers assigned yet"));
        assignedVolunteers.setStyle(CARD_STYLE);
        assignedVolunteers.setPrefHeight(200);
        assignedVolunteers.setCellFactory(ignored -> volunteerCell());
        assignedVolunteers.getSelectionModel().selectedItemProperty()
                .addListener((ignored, previous, selected) -> removeVolunteer.setDisable(selected == null));
        removeVolunteer.setStyle(SECONDARY_BUTTON_STYLE);
        removeVolunteer.setDisable(true);
        removeVolunteer.setOnAction(ignored -> removeSelectedVolunteer());
        HBox removeActions = new HBox(removeVolunteer);
        removeActions.setAlignment(Pos.CENTER_RIGHT);

        attendeePicker.setMaxWidth(Double.MAX_VALUE);
        attendeePicker.setPromptText("Select a registered attendee");
        attendeePicker.setCellFactory(ignored -> attendeeCell());
        attendeePicker.setButtonCell(attendeeCell());
        volunteerRole.setPromptText("Optional, e.g. Usher");
        volunteerRole.setMaxWidth(Double.MAX_VALUE);
        volunteerRole.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().length() <= VolunteerService.MAX_ROLE_LENGTH ? change : null));
        availabilityNote.setWrapText(true);
        availabilityNote.setStyle("-fx-text-fill: #61708a;");
        volunteerFeedback.setWrapText(true);

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
        form.add(fieldLabel("Attendee"), 0, 0);
        form.add(attendeePicker, 1, 0);
        form.add(fieldLabel("Role"), 0, 1);
        form.add(volunteerRole, 1, 1);
        form.add(availabilityNote, 1, 2);

        assignVolunteer.setStyle(PRIMARY_BUTTON_STYLE);
        assignVolunteer.setOnAction(ignored -> assignSelectedAttendee());
        HBox assignActions = new HBox(assignVolunteer);
        assignActions.setAlignment(Pos.CENTER_RIGHT);

        VBox formCard = new VBox(16,
                volunteerHeading,
                help,
                sectionLabel("Assigned volunteers"),
                assignedVolunteers,
                removeActions,
                sectionLabel("Assign a volunteer"),
                form,
                volunteerFeedback,
                assignActions);
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

    private VBox buildRegistrationContent() {
        Label pageTitle = new Label("Registrations");
        pageTitle.setStyle("-fx-text-fill: #61708a; -fx-font-size: 13px;");
        Label contentTitle = new Label("Club Organizer — Registrations");
        contentTitle.setStyle("-fx-text-fill: #172033; -fx-font-size: 24px; -fx-font-weight: bold;");

        registrationEvents.setMinWidth(260);
        registrationEvents.setPrefWidth(320);
        registrationEvents.setMaxWidth(360);
        registrationEvents.setPlaceholder(new Label("No events yet"));
        registrationEvents.setStyle(CARD_STYLE);
        registrationEvents.setCellFactory(ignored -> eventCell());
        registrationEvents.getSelectionModel().selectedItemProperty()
                .addListener((ignored, previous, selected) -> loadRegistrations(selected));

        VBox listCard = new VBox(12, sectionLabel("Your events"), registrationEvents);
        listCard.setPadding(new Insets(18));
        listCard.setStyle(CARD_STYLE);
        listCard.setMinWidth(280);
        listCard.setPrefWidth(340);
        listCard.setMaxWidth(380);
        VBox.setVgrow(registrationEvents, Priority.ALWAYS);

        registrationHeading.setStyle("-fx-text-fill: #172033; -fx-font-size: 16px; -fx-font-weight: bold;");
        registrationCount.setStyle("-fx-text-fill: #172033; -fx-font-size: 14px;");
        registrationFeedback.setWrapText(true);
        registrationFeedback.setStyle("-fx-text-fill: #b42318;");

        registrants.setPlaceholder(new Label("No attendees have registered for this event yet."));
        registrants.setStyle(CARD_STYLE);
        registrants.setCellFactory(ignored -> attendeeCell());
        VBox.setVgrow(registrants, Priority.ALWAYS);

        VBox detailCard = new VBox(16,
                registrationHeading,
                registrationCount,
                sectionLabel("Registered attendees"),
                registrants,
                registrationFeedback);
        detailCard.setPadding(new Insets(22));
        detailCard.setStyle(CARD_STYLE);
        detailCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(detailCard, Priority.ALWAYS);

        HBox body = new HBox(20, listCard, detailCard);
        body.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(listCard, Priority.ALWAYS);
        VBox.setVgrow(detailCard, Priority.ALWAYS);

        VBox content = new VBox(18, pageTitle, contentTitle, body);
        content.setPadding(new Insets(28, 32, 28, 32));
        content.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(body, Priority.ALWAYS);
        return content;
    }

    private VBox buildAnnouncementContent() {
        Label pageTitle = new Label("Announcements");
        pageTitle.setStyle("-fx-text-fill: #61708a; -fx-font-size: 13px;");
        Label contentTitle = new Label("Club Organizer — Announcements");
        contentTitle.setStyle("-fx-text-fill: #172033; -fx-font-size: 24px; -fx-font-weight: bold;");

        announcementEvents.setMinWidth(260);
        announcementEvents.setPrefWidth(320);
        announcementEvents.setMaxWidth(360);
        announcementEvents.setPlaceholder(new Label("No events yet"));
        announcementEvents.setStyle(CARD_STYLE);
        announcementEvents.setCellFactory(ignored -> eventCell());
        announcementEvents.getSelectionModel().selectedItemProperty()
                .addListener((ignored, previous, selected) -> {
                    announcementFeedback.setText("");
                    loadAnnouncements(selected);
                });

        VBox listCard = new VBox(12, sectionLabel("Your events"), announcementEvents);
        listCard.setPadding(new Insets(18));
        listCard.setStyle(CARD_STYLE);
        listCard.setMinWidth(280);
        listCard.setPrefWidth(340);
        listCard.setMaxWidth(380);
        VBox.setVgrow(announcementEvents, Priority.ALWAYS);

        announcementHeading.setStyle("-fx-text-fill: #172033; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label help = new Label(
                "Announcements are saved permanently and queued as notifications for every attendee "
                        + "registered for the event at the time you send.");
        help.setWrapText(true);
        help.setStyle("-fx-text-fill: #61708a;");

        announcementMessage.setPromptText("Write an announcement, e.g. Doors open at 6pm.");
        announcementMessage.setWrapText(true);
        announcementMessage.setPrefRowCount(4);
        announcementMessage.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().length() <= AnnouncementService.MAX_MESSAGE_LENGTH ? change : null));
        announcementMessage.textProperty().addListener((ignored, previous, text) -> updateAnnouncementControls());
        announcementCounter.setStyle("-fx-text-fill: #61708a; -fx-font-size: 11px;");
        announcementFeedback.setWrapText(true);

        sendAnnouncement.setStyle(PRIMARY_BUTTON_STYLE);
        sendAnnouncement.setOnAction(ignored -> sendSelectedAnnouncement());
        HBox sendActions = new HBox(12, announcementCounter, spacer(), sendAnnouncement);
        sendActions.setAlignment(Pos.CENTER_LEFT);

        announcementHistory.setPlaceholder(new Label("No announcements posted yet"));
        announcementHistory.setStyle(CARD_STYLE);
        announcementHistory.setCellFactory(ignored -> announcementCell());
        VBox.setVgrow(announcementHistory, Priority.ALWAYS);

        VBox detailCard = new VBox(16,
                announcementHeading,
                help,
                sectionLabel("New announcement"),
                announcementMessage,
                sendActions,
                announcementFeedback,
                sectionLabel("Posted announcements"),
                announcementHistory);
        detailCard.setPadding(new Insets(22));
        detailCard.setStyle(CARD_STYLE);
        detailCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(detailCard, Priority.ALWAYS);

        HBox body = new HBox(20, listCard, detailCard);
        body.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(listCard, Priority.ALWAYS);
        VBox.setVgrow(detailCard, Priority.ALWAYS);

        VBox content = new VBox(18, pageTitle, contentTitle, body);
        content.setPadding(new Insets(28, 32, 28, 32));
        content.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(body, Priority.ALWAYS);
        updateAnnouncementControls();
        return content;
    }

    private static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private static ListCell<Announcement> announcementCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Announcement announcement, boolean empty) {
                super.updateItem(announcement, empty);
                if (empty || announcement == null) {
                    setText(null);
                    return;
                }
                setWrapText(true);
                setPrefWidth(0);
                setText(SingaporeDateTimes.display(announcement.createdAt()) + "\n" + announcement.message());
            }
        };
    }

    private VBox buildClubContent() {
        Label pageTitle = new Label("Clubs");
        pageTitle.setStyle("-fx-text-fill: #61708a; -fx-font-size: 13px;");
        Label contentTitle = new Label("Club Organizer — Clubs");
        contentTitle.setStyle("-fx-text-fill: #172033; -fx-font-size: 24px; -fx-font-weight: bold;");

        myClubs.setPlaceholder(new Label("You don't own any clubs yet"));
        myClubs.setStyle(CARD_STYLE);
        myClubs.setCellFactory(ignored -> new ListCell<>() {
            @Override
            protected void updateItem(Club ownedClub, boolean empty) {
                super.updateItem(ownedClub, empty);
                setText(empty || ownedClub == null
                        ? null
                        : ownedClub.name() + "\nCreated " + SingaporeDateTimes.display(ownedClub.createdAt()));
            }
        });
        VBox listCard = new VBox(12, sectionLabel("Your clubs"), myClubs);
        listCard.setPadding(new Insets(18));
        listCard.setStyle(CARD_STYLE);
        listCard.setMinWidth(280);
        listCard.setPrefWidth(340);
        listCard.setMaxWidth(380);
        VBox.setVgrow(myClubs, Priority.ALWAYS);

        Label heading = new Label("Create a club");
        heading.setStyle("-fx-text-fill: #172033; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label help = new Label(
                "You become the only organizer who can manage this club's events. "
                        + "Club names must be unique across the system.");
        help.setWrapText(true);
        help.setStyle("-fx-text-fill: #61708a;");

        clubName.setPromptText("e.g. Chess Club");
        clubName.setMaxWidth(Double.MAX_VALUE);
        clubName.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().length() <= ClubService.MAX_NAME_LENGTH ? change : null));
        clubName.textProperty().addListener((ignored, previous, text) -> createClub.setDisable(text.isBlank()));
        clubName.setOnAction(ignored -> createSelectedClub());
        createClub.setStyle(PRIMARY_BUTTON_STYLE);
        createClub.setDisable(true);
        createClub.setOnAction(ignored -> createSelectedClub());
        clubFeedback.setWrapText(true);

        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(14);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(120);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        fields.setFillWidth(true);
        form.getColumnConstraints().addAll(labels, fields);
        form.add(fieldLabel("Club name"), 0, 0);
        form.add(clubName, 1, 0);

        HBox actions = new HBox(createClub);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox formCard = new VBox(16, heading, help, form, clubFeedback, actions);
        formCard.setPadding(new Insets(22));
        formCard.setStyle(CARD_STYLE);
        formCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(formCard, Priority.ALWAYS);

        HBox body = new HBox(20, listCard, formCard);
        body.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(listCard, Priority.ALWAYS);

        VBox content = new VBox(18, pageTitle, contentTitle, body);
        content.setPadding(new Insets(28, 32, 28, 32));
        content.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(body, Priority.ALWAYS);
        return content;
    }

    private void showClubsScreen() {
        screen = Screen.CLUBS;
        workspace.getChildren().setAll(clubContent);
        highlightNavForScreen();
        clubFeedback.setText("");
        try {
            reloadClubs();
        } catch (RuntimeException exception) {
            setClubFeedback("Could not load clubs: " + exception.getMessage(), true);
        }
        clubName.requestFocus();
    }

    private void createSelectedClub() {
        try {
            Club created = clubService.createClub(account, clubName.getText());
            clubName.clear();
            reloadClubs();
            myClubs.getSelectionModel().select(created);
            setClubFeedback("Club \"" + created.name() + "\" created. You can now create events for it.", false);
        } catch (RuntimeException exception) {
            setClubFeedback(exception.getMessage(), true);
        }
    }

    private void setClubFeedback(String message, boolean error) {
        clubFeedback.setText(message == null ? "Operation failed" : message);
        clubFeedback.setStyle(error ? "-fx-text-fill: #b42318;" : "-fx-text-fill: #1b5e20;");
    }

    private static ListCell<AssignedVolunteer> volunteerCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(AssignedVolunteer volunteer, boolean empty) {
                super.updateItem(volunteer, empty);
                if (empty || volunteer == null) {
                    setText(null);
                    return;
                }
                String name = volunteer.displayName().orElse("Attendee no longer registered");
                String role = volunteer.role().isEmpty() ? "No role" : volunteer.role();
                setText(name + " — " + role);
            }
        };
    }

    private static ListCell<RegisteredAttendee> attendeeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(RegisteredAttendee attendee, boolean empty) {
                super.updateItem(attendee, empty);
                setText(empty || attendee == null ? null : attendee.displayName());
            }
        };
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

    private void showVolunteersScreen() {
        screen = Screen.VOLUNTEERS;
        workspace.getChildren().setAll(volunteerContent);
        refreshEvents(null);
        highlightNavForScreen();
        volunteerFeedback.setText("");
        if (volunteerEvents.getSelectionModel().getSelectedItem() == null
                && !volunteerEvents.getItems().isEmpty()) {
            volunteerEvents.getSelectionModel().selectFirst();
        } else {
            loadVolunteers(volunteerEvents.getSelectionModel().getSelectedItem());
        }
    }

    private void showAnnouncementsScreen() {
        screen = Screen.ANNOUNCEMENTS;
        workspace.getChildren().setAll(announcementContent);
        refreshEvents(null);
        highlightNavForScreen();
        announcementFeedback.setText("");
        if (announcementEvents.getSelectionModel().getSelectedItem() == null
                && !announcementEvents.getItems().isEmpty()) {
            announcementEvents.getSelectionModel().selectFirst();
        } else {
            loadAnnouncements(announcementEvents.getSelectionModel().getSelectedItem());
        }
    }

    private void loadAnnouncements(Event event) {
        if (event == null) {
            announcementHeading.setText("Announcements");
            announcementHistory.getItems().clear();
            updateAnnouncementControls();
            return;
        }
        announcementHeading.setText("Announcements — " + event.title());
        try {
            announcementHistory.getItems().setAll(announcementService.list(actor, event.id()));
        } catch (RuntimeException exception) {
            announcementHistory.getItems().clear();
            setAnnouncementFeedback("Could not load announcements: " + exception.getMessage(), true);
        }
        updateAnnouncementControls();
    }

    private void updateAnnouncementControls() {
        String text = announcementMessage.getText() == null ? "" : announcementMessage.getText();
        announcementCounter.setText(text.length() + " / " + AnnouncementService.MAX_MESSAGE_LENGTH);
        boolean eventSelected = announcementEvents.getSelectionModel().getSelectedItem() != null;
        announcementMessage.setDisable(!eventSelected);
        sendAnnouncement.setDisable(!eventSelected || text.isBlank());
    }

    private void sendSelectedAnnouncement() {
        Event event = announcementEvents.getSelectionModel().getSelectedItem();
        if (event == null) {
            setAnnouncementFeedback("Select an event first.", true);
            return;
        }
        try {
            AnnouncementResult result = announcementService.post(actor, event.id(), announcementMessage.getText());
            announcementMessage.clear();
            loadAnnouncements(event);
            setAnnouncementFeedback(announcementOutcome(result), result.notificationFailures() > 0);
        } catch (RuntimeException exception) {
            setAnnouncementFeedback(exception.getMessage(), true);
        }
    }

    private static String announcementOutcome(AnnouncementResult result) {
        String outcome = result.notificationsQueued() == 0 && result.notificationFailures() == 0
                ? "Announcement saved. No registered attendees to notify yet."
                : "Announcement saved. Notification queued for " + result.notificationsQueued()
                        + " registered attendee" + (result.notificationsQueued() == 1 ? "" : "s") + ".";
        if (result.notificationFailures() > 0) {
            outcome += " " + result.notificationFailures() + " notification"
                    + (result.notificationFailures() == 1 ? "" : "s") + " could not be queued.";
        }
        return outcome;
    }

    private void setAnnouncementFeedback(String message, boolean error) {
        announcementFeedback.setText(message == null ? "Operation failed" : message);
        announcementFeedback.setStyle(error ? "-fx-text-fill: #b42318;" : "-fx-text-fill: #1b5e20;");
    }

    private void showRegistrationsScreen() {
        screen = Screen.REGISTRATIONS;
        workspace.getChildren().setAll(registrationContent);
        refreshEvents(null);
        highlightNavForScreen();
        if (registrationEvents.getSelectionModel().getSelectedItem() == null
                && !registrationEvents.getItems().isEmpty()) {
            registrationEvents.getSelectionModel().selectFirst();
        } else {
            loadRegistrations(registrationEvents.getSelectionModel().getSelectedItem());
        }
    }

    private void loadRegistrations(Event event) {
        registrationFeedback.setText("");
        if (event == null) {
            registrationHeading.setText("Registrations");
            registrationCount.setText("Select an event from the list.");
            registrants.getItems().clear();
            return;
        }
        registrationHeading.setText("Registrations — " + event.title());
        try {
            RegistrationOverview overview = registrationService.overview(actor, event.id());
            registrationCount.setText(overview.registeredCount() + " / " + overview.capacity() + " registered");
            registrants.getItems().setAll(overview.attendees());
        } catch (RuntimeException exception) {
            registrationCount.setText("");
            registrants.getItems().clear();
            registrationFeedback.setText("Could not load registrations: " + exception.getMessage());
        }
    }

    private void loadVolunteers(Event event) {
        if (event == null) {
            volunteerHeading.setText("Volunteers");
            assignedVolunteers.getItems().clear();
            attendeePicker.getItems().clear();
            availabilityNote.setText("Select an event from the list.");
            attendeePicker.setDisable(true);
            volunteerRole.setDisable(true);
            assignVolunteer.setDisable(true);
            return;
        }
        volunteerHeading.setText("Volunteers — " + event.title());
        try {
            assignedVolunteers.getItems().setAll(volunteerService.listVolunteers(actor, event.id()));
            attendeePicker.getItems().setAll(volunteerService.availableAttendees(actor, event.id()));
            boolean noneAvailable = attendeePicker.getItems().isEmpty();
            availabilityNote.setText(noneAvailable
                    ? "No registered attendees available to assign. Attendees must register for "
                            + "this event before they can volunteer."
                    : "");
            attendeePicker.setDisable(noneAvailable);
            volunteerRole.setDisable(noneAvailable);
            assignVolunteer.setDisable(noneAvailable);
            if (!noneAvailable) {
                attendeePicker.getSelectionModel().selectFirst();
            }
        } catch (RuntimeException exception) {
            assignedVolunteers.getItems().clear();
            attendeePicker.getItems().clear();
            attendeePicker.setDisable(true);
            volunteerRole.setDisable(true);
            assignVolunteer.setDisable(true);
            setVolunteerFeedback("Could not load volunteers: " + exception.getMessage(), true);
        }
    }

    private void assignSelectedAttendee() {
        Event event = volunteerEvents.getSelectionModel().getSelectedItem();
        RegisteredAttendee attendee = attendeePicker.getSelectionModel().getSelectedItem();
        if (event == null) {
            setVolunteerFeedback("Select an event first.", true);
            return;
        }
        if (attendee == null) {
            setVolunteerFeedback("Select a registered attendee.", true);
            return;
        }
        try {
            volunteerService.assign(actor, event.id(), attendee.attendeeId(), volunteerRole.getText());
            volunteerRole.clear();
            loadVolunteers(event);
            setVolunteerFeedback(attendee.displayName() + " assigned as a volunteer.", false);
        } catch (RuntimeException exception) {
            setVolunteerFeedback(exception.getMessage(), true);
        }
    }

    private void removeSelectedVolunteer() {
        Event event = volunteerEvents.getSelectionModel().getSelectedItem();
        AssignedVolunteer volunteer = assignedVolunteers.getSelectionModel().getSelectedItem();
        if (event == null || volunteer == null) {
            setVolunteerFeedback("Select a volunteer to remove.", true);
            return;
        }
        try {
            volunteerService.remove(actor, event.id(), volunteer.attendeeId());
            loadVolunteers(event);
            setVolunteerFeedback(
                    volunteer.displayName().orElse("Volunteer") + " removed.", false);
        } catch (RuntimeException exception) {
            setVolunteerFeedback(exception.getMessage(), true);
        }
    }

    private void setVolunteerFeedback(String message, boolean error) {
        volunteerFeedback.setText(message == null ? "Operation failed" : message);
        volunteerFeedback.setStyle(error ? "-fx-text-fill: #b42318;" : "-fx-text-fill: #1b5e20;");
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
                CapacityUpdateResult result =
                        service.editEvent(actor, editingEventId, editingVersion, details);
                saved = result.event();
                String message = switch (result.syncStatus()) {
                    case PENDING_REQUEST_SYNCED ->
                            "Draft event updated. Pending venue request attendance synced.";
                    case DECIDED_REQUEST_UNCHANGED ->
                            "Draft event updated. Venue request already decided — attendance left unchanged.";
                    case NO_OPEN_REQUEST -> "Draft event updated.";
                };
                setFeedback(message, false);
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
        volunteerEvents.getItems().setAll(listed);
        registrationEvents.getItems().setAll(listed);
        announcementEvents.getItems().setAll(listed);
        if (selectedId != null) {
            listed.stream()
                    .filter(event -> event.id().equals(selectedId))
                    .findFirst()
                    .ifPresent(event -> {
                        events.getSelectionModel().select(event);
                        requestEvents.getSelectionModel().select(event);
                        volunteerEvents.getSelectionModel().select(event);
                        registrationEvents.getSelectionModel().select(event);
                        announcementEvents.getSelectionModel().select(event);
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
        editorHeading.setText("Edit draft event — " + event.title());
        reset.setText("Revert changes");
        feedback.setText("");
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
            setFeedback("No events yet. Fill in the form to create your first draft.", false);
            return;
        }
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
        if (announce) {
            setFeedback("Ready to create a new draft.", false);
        } else {
            feedback.setText("");
        }
        title.requestFocus();
    }

    private void highlightNavForScreen() {
        eventsNav.setStyle(screen == Screen.EVENTS ? NAV_BUTTON_ACTIVE_STYLE : NAV_BUTTON_STYLE);
        requestVenueNav.setStyle(screen == Screen.REQUEST_VENUE ? NAV_BUTTON_ACTIVE_STYLE : NAV_BUTTON_STYLE);
        volunteersNav.setStyle(screen == Screen.VOLUNTEERS ? NAV_BUTTON_ACTIVE_STYLE : NAV_BUTTON_STYLE);
        registrationsNav.setStyle(screen == Screen.REGISTRATIONS ? NAV_BUTTON_ACTIVE_STYLE : NAV_BUTTON_STYLE);
        announcementsNav.setStyle(screen == Screen.ANNOUNCEMENTS ? NAV_BUTTON_ACTIVE_STYLE : NAV_BUTTON_STYLE);
        clubsNav.setStyle(screen == Screen.CLUBS ? NAV_BUTTON_ACTIVE_STYLE : NAV_BUTTON_STYLE);
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
