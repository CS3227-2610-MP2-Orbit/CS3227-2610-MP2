package seedu.eventmanager.ui;

import java.util.Objects;
import java.util.UUID;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.concurrent.Task;
import seedu.eventmanager.attendee.InboxSnapshot;
import seedu.eventmanager.common.ApplicationException;

/** FX-thread coordinator; reads/writes run in virtual threads and late callbacks are discarded. */
public final class InboxController implements AutoCloseable {
    public interface View {
        void loading();
        void loaded(InboxSnapshot inbox);
        void failed(String message);
        void feedback(String message);
    }
    private final InboxActions actions;
    private final View view;
    private final ReadOnlyBooleanWrapper busy = new ReadOnlyBooleanWrapper();
    private Task<InboxSnapshot> read;
    private boolean closed;

    public InboxController(InboxActions actions, View view) {
        this.actions = Objects.requireNonNull(actions); this.view = Objects.requireNonNull(view);
    }
    public ReadOnlyBooleanProperty busyProperty() { return busy.getReadOnlyProperty(); }

    public void refresh() {
        if (closed || busy.get()) return;
        if (read != null) read.cancel();
        view.loading();
        Task<InboxSnapshot> task = new Task<>() {
            @Override protected InboxSnapshot call() { return actions.list().get(); }
        };
        read = task;
        task.setOnSucceeded(ignored -> { if (!closed && read == task) view.loaded(task.getValue()); });
        task.setOnFailed(ignored -> { if (!closed && read == task) view.failed(failure(task.getException())); });
        Thread.ofVirtual().name("attendee-inbox-read").start(task);
    }

    public void markRead(UUID id) { command(() -> actions.markRead().accept(id)); }
    public void markAllRead() { command(actions.markAllRead()); }

    private void command(Runnable action) {
        if (closed || busy.get()) return;
        if (read != null) read.cancel();
        read = null;
        busy.set(true);
        view.feedback("Updating read status…");
        Task<Void> task = new Task<>() {
            @Override protected Void call() { action.run(); return null; }
        };
        task.setOnSucceeded(ignored -> complete("Read status updated."));
        task.setOnFailed(ignored -> complete(failure(task.getException())));
        Thread.ofVirtual().name("attendee-inbox-mark-read").start(task);
    }

    private void complete(String message) {
        if (closed) return;
        busy.set(false);
        view.feedback(message);
        refresh();
    }

    private static String failure(Throwable failure) {
        if (failure instanceof ApplicationException application) {
            return switch (application.code()) {
                case "UNAUTHENTICATED", "FORBIDDEN" -> "Your attendee session is no longer valid. Return Home and log in again.";
                case "NOTIFICATION_NOT_FOUND" -> "This notification is not available. Refresh your inbox.";
                default -> "Unable to update or load notifications. Refresh to check their current status.";
            };
        }
        return "Unable to update or load notifications. Refresh to check their current status.";
    }

    @Override public void close() {
        closed = true;
        if (read != null) read.cancel();
        // A write may already be committed; ignore its UI callback rather than promising rollback.
    }
}
