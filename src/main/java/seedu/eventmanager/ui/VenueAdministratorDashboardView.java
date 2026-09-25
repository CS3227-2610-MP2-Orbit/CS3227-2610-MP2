package seedu.eventmanager.ui;

import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import seedu.eventmanager.storage.JdbcLocalSessionService;
import seedu.eventmanager.ui.VenueAdministratorDashboardState;

/** Dashboard shell for the Venue Administrator role. */
public final class VenueAdministratorDashboardView {
    private final BorderPane root = new BorderPane();
    private final Label pageTitle = new Label();
    private final Label contentTitle = new Label();
    private Label pendingRequestsValue;
    private Label availableVenuesValue;
    private final Runnable refreshDashboard;
    private final Runnable showRequests;
    private final Runnable showVenues;
    private final Runnable showUsers;

    public VenueAdministratorDashboardView(JdbcLocalSessionService.Session session,
            Runnable onLogout, Runnable refreshDashboard, Runnable showRequests,
            Runnable showVenues, Runnable showUsers) {
        Objects.requireNonNull(session);
        Objects.requireNonNull(onLogout);
        this.refreshDashboard = Objects.requireNonNull(refreshDashboard);
        this.showRequests = Objects.requireNonNull(showRequests);
        this.showVenues = Objects.requireNonNull(showVenues);
        this.showUsers = Objects.requireNonNull(showUsers);
        root.setStyle("-fx-background-color: #f7f9fc;");
        root.setLeft(sidebar(onLogout));
        showOverview(session);
    }

    public BorderPane root() {
        return root;
    }

    public void update(VenueAdministratorDashboardState state) {
        pendingRequestsValue.setText(String.valueOf(state.data().pendingRequests().size()));
    }

    public void update(VenueAdministratorDashboardState state, int availableVenues) {
        update(state);
        availableVenuesValue.setText(String.valueOf(availableVenues));
    }

    private VBox sidebar(Runnable onLogout) {
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(24, 16, 24, 16));
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #172033;");

        Label brand = new Label("EVENT VENUE\nMANAGER");
        brand.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        sidebar.getChildren().add(brand);

        addNavigation(sidebar, "Dashboard", () -> {
            showOverview(null);
            refreshDashboard.run();
        });
        addNavigation(sidebar, "Venue requests", showRequests);
        addNavigation(sidebar, "Venues", showVenues);
        addNavigation(sidebar, "Users and access", showUsers);

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        Button logout = navigationButton("Log out");
        logout.setOnAction(event -> onLogout.run());
        sidebar.getChildren().add(logout);
        return sidebar;
    }

    private void addNavigation(VBox sidebar, String text, Runnable action) {
        Button button = navigationButton(text);
        button.setOnAction(event -> action.run());
        sidebar.getChildren().add(button);
    }

    private Button navigationButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setStyle("-fx-background-color: transparent; -fx-text-fill: #dce4f2;"
                + " -fx-font-size: 13px; -fx-padding: 10px 12px;");
        return button;
    }

    private void showOverview(JdbcLocalSessionService.Session session) {
        pageTitle.setText("Overview");
        contentTitle.setText("Venue Administrator Dashboard");
        GridPane cards = new GridPane();
        cards.setHgap(16);
        cards.setVgap(16);
        pendingRequestsValue = new Label("0");
        cards.add(summaryCard("Pending requests", pendingRequestsValue, "Awaiting review"), 0, 0);
        cards.add(summaryCard("Upcoming bookings", "0", "Next 30 days"), 1, 0);
        availableVenuesValue = new Label("0");
        cards.add(summaryCard("Available venues", availableVenuesValue, "Ready to book"), 2, 0);
        cards.add(summaryCard("Warnings", "0", "Needs attention"), 0, 1);

        VBox content = content(pageTitle, contentTitle, cards,
                new Label("Live venue activity will appear here once the dashboard repository is connected."));
        root.setCenter(content);
    }

    private VBox content(Node... nodes) {
        VBox content = new VBox(18, nodes);
        content.setPadding(new Insets(28));
        return content;
    }

    private VBox summaryCard(String title, String value, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #61708a; -fx-font-size: 13px;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: #172033; -fx-font-size: 28px; -fx-font-weight: bold;");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-text-fill: #61708a; -fx-font-size: 12px;");
        VBox card = new VBox(8, titleLabel, valueLabel, subtitleLabel);
        card.setPadding(new Insets(18));
        card.setPrefWidth(210);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8px;"
                + " -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");
        return card;
    }

    private VBox summaryCard(String title, Label valueLabel, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #61708a; -fx-font-size: 13px;");
        valueLabel.setStyle("-fx-text-fill: #172033; -fx-font-size: 28px; -fx-font-weight: bold;");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-text-fill: #61708a; -fx-font-size: 12px;");
        VBox card = new VBox(8, titleLabel, valueLabel, subtitleLabel);
        card.setPadding(new Insets(18));
        card.setPrefWidth(210);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8px;"
                + " -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");
        return card;
    }
}
