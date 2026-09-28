package seedu.eventmanager.ui;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import javax.imageio.ImageIO;
import javafx.application.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.PixelFormat;
import javafx.stage.Stage;
import seedu.eventmanager.attendee.*;
import seedu.eventmanager.common.*;
import seedu.eventmanager.event.Event;
import seedu.eventmanager.registration.*;
import seedu.eventmanager.service.TransactionManager;

/** Real JavaFX screens and command service; synthetic reads/store, not PostgreSQL or login E2E. */
public final class CheckInAvailabilitySmoke {
    private static volatile Throwable failure;
    public static void main(String[] args) {
        Application.launch(Smoke.class, args);
        if (failure != null) throw new AssertionError("Availability UI smoke failed", failure);
        System.out.println("Availability UI smoke PASS: seven results on both screens, four controlled boundaries, SGT window, stale command rejection, wrapping.");
    }

    public static final class Smoke extends Application {
        private static final UUID EVENT = new UUID(0, 1), OWNER = new UUID(0, 2);
        private static final Instant START = Instant.parse("2030-01-01T10:00:00Z"), END = START.plusSeconds(3600);
        private final AtomicReference<Instant> now = new AtomicReference<>(START);
        private final Clock clock = new Clock() {
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return now.get(); }
        };
        private final AtomicReference<Registration.Status> status = new AtomicReference<>(Registration.Status.CONFIRMED);
        private final AtomicReference<String> eventStatus = new AtomicReference<>("PUBLISHED");
        private final AtomicBoolean booking = new AtomicBoolean(true);
        private final AtomicInteger commands = new AtomicInteger(), audits = new AtomicInteger();
        private AttendeeBrowseView view;
        private Stage stage;

        private RegistrationEvent event() { return new RegistrationEvent(EVENT, eventStatus.get(), 20, START, END); }
        private Registration row() {
            var state = status.get();
            return state == null ? null : new Registration(new UUID(0, 3), EVENT, OWNER, state,
                    START.minusSeconds(600), state == Registration.Status.CANCELLED ? START.minusSeconds(1) : null,
                    state == Registration.Status.CHECKED_IN ? START : null, 7);
        }
        private CheckInPolicy.Result result() { return CheckInPolicy.evaluate(event(), status.get(), booking.get(), now.get()); }
        private AttendeeEventDetails details() {
            require(!Platform.isFxApplicationThread(), "detail reads off FX");
            return new AttendeeEventDetails(new CatalogueEvent(EVENT, "club", "Campus workshop", "Synthetic UI evidence",
                    START, END, 20, "Campus Club"), Optional.empty(), 1, 19, Optional.ofNullable(status.get()),
                    status.get() == null ? -1 : 7, RegistrationEligibilityPolicy.Result.EVENT_NOT_REGISTERABLE, result());
        }
        private List<MyRegistration> mine() {
            require(!Platform.isFxApplicationThread(), "registration reads off FX");
            // NOT_REGISTERED tests renderer exhaustiveness only: a real missing registration produces no row.
            return List.of(new MyRegistration(EVENT, "Campus workshop", START, "Room", status.get() == null
                    ? Registration.Status.CONFIRMED : status.get(), 7, false, END, "club", "Synthetic UI evidence",
                    eventStatus.get(), result(), "Campus Club"));
        }

        @Override public void start(Stage stage) {
            this.stage = stage;
            var commandService = new RegistrationService(new RegistrationStore() {
                public RegistrationEvent lockEvent(UUID id) { return event(); }
                public boolean lockActiveAttendee(UUID id) { return true; }
                public boolean lockConfirmedActiveBooking(RegistrationEvent event) { return booking.get(); }
                public Registration find(UUID event, UUID attendee) { return row(); }
                public int occupiedPlaces(UUID event) { return 1; }
                public void save(Registration updated) { status.set(updated.status()); }
                public List<Registration> findByAttendee(UUID attendee) { return row() == null ? List.of() : List.of(row()); }
            }, token -> new Actor(OWNER, Role.ATTENDEE), new TransactionManager() {
                public <T> T execute(Supplier<T> work) { return work.get(); }
            }, (actor, action, type, id, previous, next, reason) -> audits.incrementAndGet(),
                    (recipient, action, payload) -> { throw new AssertionError("No notification expected"); }, clock);
            var catalogue = new EventCatalogueService(new EventCatalogueRepository() {
                private Entry entry() {
                    return new Entry(new Event(EVENT, "club", "synthetic-organizer", "Campus workshop",
                            "Synthetic UI evidence", START, END, 20,
                            seedu.eventmanager.event.EventStatus.PUBLISHED, 0), "Campus Club");
                }
                public List<Entry> findPublishedNotEnded(Instant now) { return List.of(entry()); }
                public Optional<Entry> findPublishedById(UUID id) { return Optional.of(entry()); }
                public List<CatalogueClub> findClubs() { return List.of(); }
            }, clock);
            view = new AttendeeBrowseView(() -> catalogue, id -> details(), new AttendeeRegistrationActions(
                    (id, version) -> { throw new AssertionError("No register"); },
                    (id, version) -> { throw new AssertionError("No cancel"); },
                    (id, version) -> {
                        require(!Platform.isFxApplicationThread(), "command off FX");
                        require(version == 7, "displayed version");
                        commands.incrementAndGet();
                        return commandService.checkIn("synthetic-session", id, version);
                    }, this::mine), new InboxActions(() -> new InboxSnapshot(List.of()), id -> { }, () -> { }),
                    () -> { }, clock);
            stage.setScene(new Scene(view, 1280, 800)); stage.show();
            Thread.ofVirtual().start(() -> {
                try { exercise(); } catch (Throwable problem) { failure = problem; }
                finally { Platform.runLater(() -> { view.close(); stage.close(); Platform.exit(); }); }
            });
        }

        private void exercise() throws Exception {
            await(() -> view.lookup("#attendee-feedback") instanceof Label label && !label.getText().startsWith("Loading"));
            for (var expected : CheckInPolicy.Result.values()) {
                status.set(switch (expected) {
                    case NOT_REGISTERED -> null;
                    case CANCELLED -> Registration.Status.CANCELLED;
                    case ALREADY_CHECKED_IN -> Registration.Status.CHECKED_IN;
                    default -> Registration.Status.CONFIRMED;
                });
                booking.set(expected != CheckInPolicy.Result.VENUE_UNAVAILABLE);
                now.set(expected == CheckInPolicy.Result.TOO_EARLY ? START.minusNanos(1)
                        : expected == CheckInPolicy.Result.CLOSED ? END : START);
                showBoth(expected);
            }
            status.set(Registration.Status.CONFIRMED); booking.set(true);
            for (var boundary : List.of(START.minusNanos(1), START, END.minusNanos(1), END)) {
                now.set(boundary);
                showBoth(boundary.isBefore(START) ? CheckInPolicy.Result.TOO_EARLY
                        : boundary.isBefore(END) ? CheckInPolicy.Result.AVAILABLE : CheckInPolicy.Result.CLOSED);
            }
            // Render AVAILABLE, then let time expire without refreshing. The service must reject both clicks.
            for (boolean registrations : List.of(false, true)) {
                now.set(START); showBoth(CheckInPolicy.Result.AVAILABLE);
                if (!registrations) {
                    fx(() -> ((Button) view.lookup("#attendee-browse-nav")).fire());
                    await(() -> ((ListView<?>) view.lookup("#attendee-events")).getItems().size() == 1);
                    fx(() -> ((ListView<?>) view.lookup("#attendee-events")).getSelectionModel().selectFirst());
                    await(() -> view.lookup("#attendee-check-in") != null);
                }
                now.set(END);
                int before = commands.get();
                fx(() -> ((Button) view.lookup(registrations ? "#attendee-check-in-registration" : "#attendee-check-in")).fire());
                await(() -> ((Label) view.lookup("#attendee-command-feedback")).getText().contains("Check-in is closed"));
                require(commands.get() == before + 1 && audits.get() == 0, "stale click reaches service; no audit on rejection");
            }
        }

        private void showBoth(CheckInPolicy.Result expected) throws Exception {
            var snapshot = details();
            fx(() -> ((Button) view.lookup("#attendee-browse-nav")).fire());
            await(() -> !((Label) view.lookup("#attendee-feedback")).getText().startsWith("Loading"));
            if (now.get().isBefore(END)) {
                fx(() -> ((ListView<?>) view.lookup("#attendee-events")).getSelectionModel().selectFirst());
                await(() -> view.lookup("#attendee-check-in-availability") instanceof Label);
            } else {
                // Ended events cannot be freshly opened from Browse: exercise its existing-detail renderer.
                fx(() -> view.loadedDetails(snapshot));
            }
            fx(() -> verify("#attendee-check-in-availability", expected));
            if (expected == CheckInPolicy.Result.VENUE_UNAVAILABLE) fx(() -> screenshot("check-in-availability-browse-1280.png"));
            fx(() -> ((Button) view.lookup("#attendee-registrations-nav")).fire());
            await(() -> view.lookup("#attendee-registrations") instanceof ListView<?> list && list.getItems().size() == 1);
            fx(() -> {
                ((ListView<?>) view.lookup("#attendee-registrations")).getSelectionModel().selectFirst();
                verify("#attendee-registration-check-in-availability", expected);
                require(((Button) view.lookup("#attendee-check-in-registration")).isVisible()
                        == (expected == CheckInPolicy.Result.AVAILABLE), "button agrees with explanation");
            });
            if (expected == CheckInPolicy.Result.VENUE_UNAVAILABLE) {
                fx(() -> { stage.setWidth(1000); stage.setHeight(640); });
                await(() -> view.getWidth() <= 1000);
                fx(() -> { view.applyCss(); view.layout(); screenshot("check-in-availability-registrations-1000.png"); });
            }
        }

        private void verify(String id, CheckInPolicy.Result expected) {
            require(view.lookup(id) instanceof Label, "visible check-in explanation on " + id);
            var label = (Label) view.lookup(id);
            require(label.getText().equals(CheckInAvailabilityText.message(expected, START, END)), "matching explanation: " + expected);
            require(label.isWrapText() && label.getText().contains("6:00 PM SGT") && label.getText().contains("7:00 PM SGT"),
                    "wrapping SGT boundaries");
        }
        private void screenshot(String file) {
            try {
                var image = view.snapshot(null, null); int w = (int) image.getWidth(), h = (int) image.getHeight();
                int[] pixels = new int[w * h];
                image.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w);
                var bitmap = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB); bitmap.setRGB(0, 0, w, h, pixels, 0, w);
                Files.createDirectories(Path.of("build/attendee-smoke"));
                ImageIO.write(bitmap, "png", Path.of("build/attendee-smoke", file).toFile());
            } catch (java.io.IOException error) { throw new IllegalStateException(error); }
        }
        private static void await(BooleanSupplier ready) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < deadline) {
                var task = new FutureTask<Boolean>(ready::getAsBoolean); Platform.runLater(task);
                if (task.get(5, TimeUnit.SECONDS)) return;
                Thread.sleep(20);
            }
            throw new AssertionError("Timed out waiting for UI");
        }
        private static void fx(Runnable action) throws Exception {
            var task = new FutureTask<Void>(action, null); Platform.runLater(task); task.get(5, TimeUnit.SECONDS);
        }
        private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    }
}
