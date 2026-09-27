package seedu.eventmanager.ui;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import javafx.concurrent.Task;
import seedu.eventmanager.attendee.AttendeeEventDetails;
import seedu.eventmanager.attendee.CatalogueEvent;
import seedu.eventmanager.attendee.CatalogueQuery;

/** JavaFX-thread coordinator. Business rules and persistence live behind the supplied reads. */
public final class AttendeeBrowseController implements AutoCloseable {
    public interface View {
        void searching();
        void searched(List<CatalogueEvent> events);
        void searchFailed(Throwable failure);
        void loadingDetails();
        void loadedDetails(AttendeeEventDetails details);
        void detailFailed(Throwable failure);
        void clearedDetails();
    }

    private final Function<CatalogueQuery, List<CatalogueEvent>> search;
    private final Function<UUID, AttendeeEventDetails> details;
    private final View view;
    private Task<List<CatalogueEvent>> searchTask;
    private Task<AttendeeEventDetails> detailTask;
    private boolean closed;

    public AttendeeBrowseController(Function<CatalogueQuery, List<CatalogueEvent>> search,
            Function<UUID, AttendeeEventDetails> details, View view) {
        this.search = Objects.requireNonNull(search);
        this.details = Objects.requireNonNull(details);
        this.view = Objects.requireNonNull(view);
    }

    public void search(CatalogueQuery query) {
        if (closed) return;
        cancel(searchTask);
        clearDetails();
        view.searching();
        Task<List<CatalogueEvent>> task = new Task<>() {
            @Override protected List<CatalogueEvent> call() { return search.apply(query); }
        };
        searchTask = task;
        task.setOnSucceeded(ignored -> {
            if (!closed && searchTask == task) view.searched(task.getValue());
        });
        task.setOnFailed(ignored -> {
            if (!closed && searchTask == task) view.searchFailed(task.getException());
        });
        Thread.ofVirtual().name("attendee-catalogue-search").start(task);
    }

    public void loadDetails(UUID id) {
        if (closed) return;
        cancel(detailTask);
        view.loadingDetails(); // Immediately clear the previous attendee/event's displayed state.
        Task<AttendeeEventDetails> task = new Task<>() {
            @Override protected AttendeeEventDetails call() { return details.apply(id); }
        };
        detailTask = task;
        task.setOnSucceeded(ignored -> {
            if (!closed && detailTask == task) view.loadedDetails(task.getValue());
        });
        task.setOnFailed(ignored -> {
            if (!closed && detailTask == task) view.detailFailed(task.getException());
        });
        Thread.ofVirtual().name("attendee-catalogue-details").start(task);
    }

    public void clearDetails() {
        cancel(detailTask);
        detailTask = null;
        view.clearedDetails();
    }

    private static void cancel(Task<?> task) {
        if (task != null) task.cancel();
    }

    @Override public void close() {
        closed = true;
        cancel(searchTask);
        clearDetails();
    }
}
