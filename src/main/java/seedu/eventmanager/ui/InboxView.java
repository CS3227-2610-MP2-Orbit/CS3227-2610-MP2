package seedu.eventmanager.ui;

import java.util.function.LongConsumer;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import seedu.eventmanager.attendee.InboxMessage;
import seedu.eventmanager.attendee.InboxSnapshot;

/** Persistent owner inbox within the existing Attendee workspace sidebar. */
final class InboxView extends VBox implements InboxController.View, AutoCloseable {
    private enum Filter {
        ALL("All"), UNREAD("Unread"), READ("Read");
        private final String label;
        Filter(String label) { this.label = label; }
        boolean includes(InboxMessage message) {
            return switch (this) {
                case ALL -> true;
                case UNREAD -> message.unread();
                case READ -> !message.unread();
            };
        }
        @Override public String toString() { return label; }
    }
    private final InboxController controller;
    private final ComboBox<Filter> filter = new ComboBox<>();
    private InboxSnapshot snapshot;
    private final ListView<InboxMessage> messages = new ListView<>();
    private final Label summary = label("");
    private final Label feedback = label("");
    private final Button one = new Button("Mark selected as read");
    private final Button all = new Button("Mark all as read");
    private final LongConsumer unreadBadge;

    InboxView(InboxActions actions, LongConsumer unreadBadge) {
        super(14);
        this.unreadBadge = unreadBadge;
        controller = new InboxController(actions, this);
        setPadding(new Insets(24));
        Label heading = label("Notifications");
        heading.setStyle("-fx-text-fill: #172033; -fx-font-size: 26px;");
        Button refresh = new Button("Refresh notifications");
        refresh.setId("attendee-inbox-refresh");
        refresh.setOnAction(ignored -> refresh());
        one.setId("attendee-inbox-mark-read");
        all.setId("attendee-inbox-mark-all");
        one.setDisable(true); all.setDisable(true);
        one.setOnAction(ignored -> {
            InboxMessage selected = messages.getSelectionModel().getSelectedItem();
            if (selected != null) controller.markRead(selected.id());
        });
        all.setOnAction(ignored -> controller.markAllRead());
        filter.setId("attendee-inbox-filter");
        filter.setAccessibleText("Filter notifications by read status");
        filter.getItems().setAll(Filter.values());
        filter.setValue(Filter.ALL);
        filter.setOnAction(ignored -> applyFilter());
        messages.setId("attendee-inbox-messages");
        messages.getSelectionModel().selectedItemProperty().addListener((ignored, previous, selected) ->
                one.setDisable(selected == null || !selected.unread()));
        messages.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(InboxMessage item, boolean empty) {
                super.updateItem(item, empty); setText(null);
                if (empty || item == null) { setGraphic(null); return; }
                Label title = label((item.unread() ? "Unread · " : "Read · ") + item.title());
                title.setStyle("-fx-text-fill: #172033; -fx-font-size: 15px;"
                        + (item.unread() ? " -fx-font-weight: bold;" : ""));
                Label body = label(item.body());
                Label time = label(SingaporeDateTimes.display(item.createdAt()));
                title.maxWidthProperty().bind(messages.widthProperty().subtract(56));
                body.maxWidthProperty().bind(messages.widthProperty().subtract(56));
                time.maxWidthProperty().bind(messages.widthProperty().subtract(56));
                VBox card = new VBox(6, title, body, time);
                card.setPadding(new Insets(10)); setGraphic(card);
            }
        });
        summary.setId("attendee-inbox-summary"); feedback.setId("attendee-inbox-feedback");
        VBox.setVgrow(messages, Priority.ALWAYS);
        getChildren().setAll(heading, label("Registration updates and event announcements. Newest first."),
                new FlowPane(12, 12, label("Show"), filter, refresh, one, all), summary, feedback, messages);
        disableProperty().bind(controller.busyProperty());
    }

    ReadOnlyBooleanProperty busyProperty() { return controller.busyProperty(); }
    void refresh() { controller.refresh(); }
    @Override public void loading() {
        snapshot = null; filter.setDisable(true);
        messages.getItems().clear(); one.setDisable(true); all.setDisable(true);
        summary.setText("Loading notifications…");
        messages.setPlaceholder(label("Loading…"));
        unreadBadge.accept(-1);
    }
    @Override public void loaded(InboxSnapshot snapshot) {
        this.snapshot = snapshot;
        filter.setDisable(false);
        applyFilter();
        all.setDisable(snapshot.unreadCount() == 0);
        unreadBadge.accept(snapshot.unreadCount());
    }
    private void applyFilter() {
        if (snapshot == null) return;
        messages.getSelectionModel().clearSelection();
        messages.getItems().setAll(snapshot.messages().stream().filter(filter.getValue()::includes).toList());
        one.setDisable(true);
        summary.setText(messages.getItems().size() + " notification(s) shown of " + snapshot.messages().size()
                + " · " + snapshot.unreadCount() + " unread in your inbox");
        messages.setPlaceholder(label(snapshot.messages().isEmpty()
                ? "No notifications yet. Use Refresh to check for updates."
                : "No notifications in this filter. Choose All to see your other messages."));
    }
    @Override public void failed(String message) {
        snapshot = null; filter.setDisable(true);
        messages.getItems().clear(); one.setDisable(true); all.setDisable(true);
        summary.setText(message); unreadBadge.accept(-1);
        messages.setPlaceholder(label("Notifications unavailable. Try Refresh."));
    }
    @Override public void feedback(String message) { feedback.setText(message); }
    @Override public void close() {
        controller.close(); snapshot = null; filter.setDisable(true); messages.getItems().clear();
    }
    private static Label label(String text) {
        Label label = new Label(text); label.setWrapText(true);
        label.setStyle("-fx-text-fill: #61708a; -fx-font-size: 13px;");
        return label;
    }
}
