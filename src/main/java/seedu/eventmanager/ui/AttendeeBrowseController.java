package seedu.eventmanager.ui;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.BiFunction;
import javafx.concurrent.Task;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import seedu.eventmanager.attendee.AttendeeEventDetails;
import seedu.eventmanager.attendee.CatalogueEvent;
import seedu.eventmanager.attendee.CatalogueQuery;
import seedu.eventmanager.attendee.MyRegistration;
import seedu.eventmanager.registration.Registration;

/** JavaFX-thread coordinator. Business rules and persistence live behind session-bound callbacks. */
public final class AttendeeBrowseController implements AutoCloseable {
    public interface View {
        void searching();
        void searched(List<CatalogueEvent> events);
        void searchFailed(Throwable failure);
        void loadingDetails();
        void loadedDetails(AttendeeEventDetails details);
        void detailFailed(Throwable failure);
        void clearedDetails();
        void loadingRegistrations();
        void loadedRegistrations(List<MyRegistration> registrations);
        void registrationsFailed(Throwable failure);
        void commandFeedback(String message);
    }

    private final Function<CatalogueQuery, List<CatalogueEvent>> search;
    private final Function<UUID, AttendeeEventDetails> details;
    private final View view;
    private final AttendeeRegistrationActions actions;
    private final ReadOnlyBooleanWrapper busy = new ReadOnlyBooleanWrapper();
    private Task<List<CatalogueEvent>> searchTask;
    private Task<AttendeeEventDetails> detailTask;
    private Task<List<MyRegistration>> registrationsTask;
    private Task<Registration> commandTask;
    private UUID selectedEvent;
    private boolean showingRegistrations;
    private boolean closed;

    public AttendeeBrowseController(Function<CatalogueQuery, List<CatalogueEvent>> search,
            Function<UUID, AttendeeEventDetails> details, AttendeeRegistrationActions actions, View view) {
        this.search = Objects.requireNonNull(search);
        this.details = Objects.requireNonNull(details);
        this.view = Objects.requireNonNull(view);
        this.actions = Objects.requireNonNull(actions);
    }

    public ReadOnlyBooleanProperty busyProperty() { return busy.getReadOnlyProperty(); }

    public void search(CatalogueQuery query) {
        if (closed || busy.get()) return;
        showingRegistrations = false;
        cancel(registrationsTask);
        registrationsTask = null;
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
        if (closed || busy.get() || showingRegistrations) return;
        selectedEvent = id;
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
        selectedEvent = null;
        cancel(detailTask);
        detailTask = null;
        view.clearedDetails();
    }

    public void loadRegistrations() {
        if (closed || busy.get()) return;
        showingRegistrations = true;
        cancel(searchTask);
        searchTask = null;
        clearDetails();
        cancel(registrationsTask);
        view.loadingRegistrations();
        Task<List<MyRegistration>> task = new Task<>() {
            @Override protected List<MyRegistration> call() { return actions.myRegistrations().get(); }
        };
        registrationsTask = task;
        task.setOnSucceeded(ignored -> {
            if (!closed && registrationsTask == task) view.loadedRegistrations(task.getValue());
        });
        task.setOnFailed(ignored -> {
            if (!closed && registrationsTask == task) view.registrationsFailed(task.getException());
        });
        Thread.ofVirtual().name("attendee-my-registrations").start(task);
    }

    public void register(UUID eventId, long expectedVersion) {
        command(eventId, expectedVersion, actions.register(), "Registering…");
    }

    public void cancelRegistration(UUID eventId, long expectedVersion) {
        command(eventId, expectedVersion, actions.cancel(), "Cancelling registration…");
    }

    public void checkIn(UUID eventId, long expectedVersion) {
        command(eventId, expectedVersion, actions.checkIn(), "Checking in…");
    }

    private void command(UUID eventId, long expectedVersion, BiFunction<UUID, Long, Registration> action, String progress) {
        if (closed || busy.get()) return;
        busy.set(true);
        // Invalidate pre-command reads even if a driver ignores interruption.
        cancel(detailTask); detailTask = null;
        cancel(registrationsTask); registrationsTask = null;
        view.commandFeedback(progress);
        Task<Registration> task = new Task<>() {
            @Override protected Registration call() {
                return action.apply(eventId, expectedVersion);
            }
        };
        commandTask = task;
        task.setOnSucceeded(ignored -> finishCommand(task, RegistrationFeedback.success(task.getValue())));
        task.setOnFailed(ignored -> finishCommand(task, RegistrationFeedback.failure(task.getException())));
        Thread.ofVirtual().name("attendee-registration-command").start(task);
    }

    private void finishCommand(Task<Registration> task, String message) {
        if (closed || commandTask != task) return;
        commandTask = null;
        busy.set(false);
        view.commandFeedback(message);
        if (showingRegistrations) loadRegistrations();
        else if (selectedEvent != null) loadDetails(selectedEvent);
    }

    private static void cancel(Task<?> task) {
        if (task != null) task.cancel();
    }

    @Override public void close() {
        closed = true;
        cancel(searchTask);
        cancel(registrationsTask);
        // A database command may already be committing: never imply that closing rolls it back.
        clearDetails();
    }
}
