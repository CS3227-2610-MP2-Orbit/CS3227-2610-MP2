package seedu.eventmanager.ui;

import java.util.List;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import seedu.eventmanager.attendee.AttendanceRecord;

/** Read-only list and side-by-side details; no registration or check-in commands. */
final class AttendanceHistoryView extends VBox implements AttendanceHistoryController.View, AutoCloseable {
    private final ListView<AttendanceRecord> rows = new ListView<>();
    private final VBox details = new VBox(14);
    private final Label feedback = text("", 13);
    private final AttendanceHistoryController controller;

    AttendanceHistoryView(Supplier<List<AttendanceRecord>> reader) {
        super(14);
        controller = new AttendanceHistoryController(reader, this);
        setPadding(new Insets(24));
        Button refresh = new Button("Refresh");
        refresh.setId("attendee-history-refresh");
        refresh.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-padding: 9 14;");
        refresh.setOnAction(ignored -> refresh());
        rows.setId("attendee-history-list"); rows.setMinWidth(200);
        rows.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(AttendanceRecord row, boolean empty) {
                super.updateItem(row, empty); setText(null);
                if (empty || row == null) { setGraphic(null); return; }
                Label title = text(row.title(), 15);
                title.maxWidthProperty().bind(rows.widthProperty().subtract(48));
                Label time = text("Checked in: " + SingaporeDateTimes.display(row.checkedInAt()), 12);
                time.maxWidthProperty().bind(rows.widthProperty().subtract(48));
                VBox card = new VBox(6, title, time); card.setPadding(new Insets(8)); setGraphic(card);
            }
        });
        rows.getSelectionModel().selectedItemProperty().addListener((ignored, previous, selected) -> {
            if (selected == null) clearDetails(); else showDetails(selected);
        });
        details.setId("attendee-history-details"); details.setPadding(new Insets(20));
        details.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        ScrollPane scroll = new ScrollPane(details); scroll.setFitToWidth(true); scroll.setMinWidth(260);
        SplitPane body = new SplitPane(rows, scroll); body.setDividerPositions(0.42); VBox.setVgrow(body, Priority.ALWAYS);
        feedback.setId("attendee-history-feedback");
        getChildren().setAll(text("Attendee · Attendance history", 13), text("Attendance history", 26),
                text("Your checked-in events, newest check-in first. Times are in Singapore Time.", 13),
                refresh, feedback, body);
        clearDetails();
    }

    void refresh() { controller.refresh(); }
    void suspend() { controller.suspend(); rows.getItems().clear(); clearDetails(); }

    @Override public void loading() {
        rows.getItems().clear(); clearDetails();
        rows.setPlaceholder(text("Loading attendance history…", 13)); feedback.setText("Loading attendance history…");
    }

    @Override public void loaded(List<AttendanceRecord> records) {
        rows.getItems().setAll(records);
        rows.setPlaceholder(text("No attendance yet. Events appear here after you check in.", 13));
        feedback.setText(records.isEmpty() ? "No checked-in events yet." : records.size() + " attended event(s). Select one for details.");
    }

    @Override public void failed(String message) {
        rows.getItems().clear(); clearDetails(); feedback.setText(message);
        rows.setPlaceholder(text("Attendance history could not be loaded. Use Refresh to retry.", 13));
    }

    private void clearDetails() { details.getChildren().setAll(text("Select an attended event to view its details.", 14)); }

    private void showDetails(AttendanceRecord row) {
        details.getChildren().setAll(text(row.title(), 22),
                text("Checked in: " + SingaporeDateTimes.display(row.checkedInAt()), 16),
                text("Club: " + row.clubId(), 14),
                text("Starts: " + SingaporeDateTimes.display(row.startsAt()), 14),
                text("Ends: " + SingaporeDateTimes.display(row.endsAt()), 14),
                text(row.description().isBlank() ? "No description provided." : row.description(), 14),
                text(row.venue().isBlank() ? "Venue: unavailable" : "Venue: " + row.venue(), 14),
                text("Event status: " + row.eventStatus(), 14),
                text("Event details reflect current records, not a snapshot at check-in. Venue shows the current or latest recorded booking.", 12));
    }

    private static Label text(String value, int size) {
        Label label = new Label(value); label.setWrapText(true); label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle("-fx-text-fill: #172033; -fx-font-size: " + size + "px;"); return label;
    }

    @Override public void close() { controller.close(); rows.getItems().clear(); clearDetails(); }
}
