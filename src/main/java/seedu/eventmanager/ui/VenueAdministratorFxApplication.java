package seedu.eventmanager.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.service.UserAccessRepository;
import seedu.eventmanager.service.VenueAdministratorService;
import seedu.eventmanager.service.VenueAdministratorServiceFactory;
import seedu.eventmanager.service.VenueAvailabilityRepository;
import seedu.eventmanager.service.VenueRepository;
import seedu.eventmanager.service.VenueRequestRepository;
import seedu.eventmanager.storage.DatabaseBootstrap;
import seedu.eventmanager.storage.DatabaseConfiguration;
import seedu.eventmanager.storage.JdbcAuthorizationService;
import seedu.eventmanager.storage.JdbcVenueRequestRepository;
import seedu.eventmanager.storage.JdbcVenueRepository;
import seedu.eventmanager.service.VenueRepository;
import seedu.eventmanager.service.UserAccessRepository;
import seedu.eventmanager.storage.JdbcUserAccessRepository;
import seedu.eventmanager.storage.JdbcVenueAvailabilityRepository;
import seedu.eventmanager.storage.JdbcVenueRepository;
import seedu.eventmanager.storage.JdbcVenueRequestRepository;
import seedu.eventmanager.storage.PasswordHasher;
import seedu.eventmanager.venue.VenueStatus;

/** Initial JavaFX shell for the Venue Administrator frontend. */
public final class VenueAdministratorFxApplication extends Application {
    private static final int WIDTH = 1100;
    private static final int HEIGHT = 700;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Event Venue Manager");
        stage.setScene(new Scene(createRoot(), WIDTH, HEIGHT));
        stage.show();
    }

    /** Creates an embeddable Venue Administrator workspace for the shared desktop shell. */
    public BorderPane createRoot() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f7f9fc;");
        showLogin(root);
        return root;
    }

    private void showLogin(BorderPane root) {
        try {
            DatabaseConfiguration configuration = DatabaseBootstrap.configuration();
            DatabaseBootstrap.migrate(configuration);
            JdbcLocalSessionService sessions = new JdbcLocalSessionService(
                    new JdbcDatabase(configuration), new PasswordHasher());
            VenueAdministratorLoginView login = new VenueAdministratorLoginView(sessions,
                    session -> showDashboardPlaceholder(root, configuration, session));
            root.setCenter(login.root());
        } catch (RuntimeException exception) {
            Label error = new Label("Unable to start database-backed login: " + exception.getMessage());
            error.setWrapText(true);
            error.setPadding(new Insets(32));
            root.setCenter(error);
        }
    }

    private void showDashboardPlaceholder(BorderPane root,
            DatabaseConfiguration configuration, JdbcLocalSessionService.Session session) {
        JdbcDatabase database = new JdbcDatabase(configuration);
        JdbcAuthorizationService authorization = new JdbcAuthorizationService(database);
        VenueAdministratorService workflow = VenueAdministratorServiceFactory.create(configuration, authorization);
        VenueRequestRepository requests = new JdbcVenueRequestRepository(database);
        JdbcVenueAdministratorApiClient client = new JdbcVenueAdministratorApiClient(
                workflow, requests, session.actor());
        VenueRequestManagementView[] requestView = new VenueRequestManagementView[1];
        VenueAdministratorDashboardView[] dashboardView = new VenueAdministratorDashboardView[1];
        VenueManagementView[] venueView = new VenueManagementView[1];
        UserAccessView[] usersView = new UserAccessView[1];
        VenueRepository venueRepository = new JdbcVenueRepository(database);

        VenueAdministratorDashboardController controller = new VenueAdministratorDashboardController(
                session.actor(), authorization, client, state -> {
                    if (requestView[0] != null) {
                        requestView[0].update(state);
                    }
        });
        requestView[0] = new VenueRequestManagementView(controller,
                () -> root.setCenter(dashboardView[0].root()));
        requestView[0].update(controller.state());
        VenueRepository venueRepository = new JdbcVenueRepository(database);
        venueView[0] = new VenueManagementView(venueRepository,
                () -> root.setCenter(dashboardView[0].root()));
        UserAccessRepository userRepository = new JdbcUserAccessRepository(database, new PasswordHasher());
        usersView[0] = new UserAccessView(userRepository, showDashboard);
        VenueAdministratorDashboardView dashboard = new VenueAdministratorDashboardView(
                session, () -> showLogin(stage, root), () -> root.setCenter(requestView[0].root()),
                () -> root.setCenter(venueView[0].root()),
                () -> root.setCenter(usersView[0].root()));
        dashboardView[0] = dashboard;
        controller.load();
        root.setCenter(dashboard.root());
    }

    private static int activeVenueCount(VenueRepository venueRepository) {
        return (int) venueRepository.findAll().stream()
                .filter(venue -> venue.status() == VenueStatus.ACTIVE)
                .count();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
