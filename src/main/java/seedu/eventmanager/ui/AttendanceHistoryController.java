package seedu.eventmanager.ui;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javafx.concurrent.Task;
import seedu.eventmanager.attendee.AttendanceRecord;
import seedu.eventmanager.common.ApplicationException;

/** FX-thread coordinator using the Browse controller's cancellable, latest-read-wins pattern. */
public final class AttendanceHistoryController implements AutoCloseable {
    public interface View {
        void loading();
        void loaded(List<AttendanceRecord> records);
        void failed(String message);
    }
    private final Supplier<List<AttendanceRecord>> reader;
    private final View view;
    private Task<List<AttendanceRecord>> read;
    private boolean closed;

    public AttendanceHistoryController(Supplier<List<AttendanceRecord>> reader, View view) {
        this.reader = Objects.requireNonNull(reader);
        this.view = Objects.requireNonNull(view);
    }

    public void refresh() {
        if (closed) return;
        suspend();
        view.loading();
        Task<List<AttendanceRecord>> task = new Task<>() {
            @Override protected List<AttendanceRecord> call() { return reader.get(); }
        };
        read = task;
        task.setOnSucceeded(ignored -> {
            if (!closed && read == task) view.loaded(task.getValue());
        });
        task.setOnFailed(ignored -> {
            if (!closed && read == task) view.failed(failure(task.getException()));
        });
        Thread.ofVirtual().name("attendee-attendance-history").start(task);
    }

    /** Navigation invalidates callbacks even if a JDBC driver ignores interruption. */
    public void suspend() {
        if (read != null) read.cancel();
        read = null;
    }

    private static String failure(Throwable failure) {
        if (failure instanceof ApplicationException error
                && ("UNAUTHENTICATED".equals(error.code()) || "FORBIDDEN".equals(error.code()))) {
            return "Your attendee session is no longer valid. Return Home and log in again.";
        }
        return "Unable to load attendance history. Use Refresh to retry.";
    }

    @Override public void close() { closed = true; suspend(); }
}
