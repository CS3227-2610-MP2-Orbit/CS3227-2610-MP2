package seedu.eventmanager.ui;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javafx.application.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.PixelFormat;
import javafx.stage.Stage;
import seedu.eventmanager.attendee.*;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.event.*;
import seedu.eventmanager.registration.*;

/** Actual JavaFX controls with synthetic callbacks; not login/PostgreSQL E2E. */
public final class AttendeeCheckInSmoke {
    private static volatile Throwable failure;
    public static void main(String[] args) {
        Application.launch(SmokeApplication.class, args);
        if (failure != null) throw new AssertionError("Check-in UI smoke failed", failure);
        System.out.println("Check-in UI smoke PASS: ongoing visibility/no Register, both entry points, exact versions, duplicate guard, background work, safe rejection feedback.");
    }
    public static final class SmokeApplication extends Application {
        private static final UUID EVENT = new UUID(0, 1), OWNER = new UUID(0, 10);
        private final Instant start = Instant.now().minusSeconds(60), end = start.plusSeconds(3600);
        private final AtomicReference<Registration> current = new AtomicReference<>(row(Registration.Status.CONFIRMED, 0));
        private final AtomicReference<String> reject = new AtomicReference<>();
        private final AtomicInteger calls = new AtomicInteger();
        private final CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        private AttendeeBrowseView view;
        private Stage stage;
        private Registration row(Registration.Status status, long version) {
            return new Registration(new UUID(0, 100), EVENT, OWNER, status, start.minusSeconds(3600),
                    status == Registration.Status.CANCELLED ? start.minusSeconds(1) : null,
                    status == Registration.Status.CHECKED_IN ? Instant.now() : null, version);
        }
        private final Event event = new Event(EVENT, "test-club", "synthetic-owner", "Ongoing campus workshop",
                "Synthetic UI fixture — normal self-check-in, no QR.", start, end, 20, EventStatus.PUBLISHED, 0);

        @Override public void start(Stage stage) {
            this.stage = stage;
            var catalogue = new EventCatalogueService(new EventCatalogueRepository() {
                public List<Entry> findPublishedNotEnded(Instant now) { return List.of(new Entry(event, "Campus Technology")); }
                public Optional<Entry> findPublishedById(UUID id) { return Optional.of(new Entry(event, "Campus Technology")); }
                public List<seedu.eventmanager.attendee.CatalogueClub> findClubs() { return List.of(); }
            }, Clock.systemUTC());
            view = new AttendeeBrowseView(() -> catalogue, id -> {
                require(!Platform.isFxApplicationThread(), "details off FX");
                var own = current.get();
                return new AttendeeEventDetails(catalogue.getEvent(id),
                        Optional.of(new AttendeeEventDetails.Venue("Room 1", "Level 2", "CONFIRMED", "ACTIVE")),
                        1, 19, own == null ? Optional.empty() : Optional.of(own.status()), own == null ? -1 : own.version(),
                        RegistrationEligibilityPolicy.Result.EVENT_NOT_REGISTERABLE,
                        own != null && own.status() == Registration.Status.CONFIRMED);
            }, new AttendeeRegistrationActions(
                    (id, version) -> { throw new AssertionError("Ongoing event must never offer Register"); },
                    (id, version) -> { throw new AssertionError("Ongoing event must never allow cancellation"); },
                    this::checkIn, () -> {
                        require(!Platform.isFxApplicationThread(), "my registrations off FX");
                        var own = current.get();
                        return own == null ? List.of() : List.of(new MyRegistration(EVENT, event.title(), start, "Room 1",
                                own.status(), own.version(), false, end, "test-club", event.description(), "PUBLISHED",
                                own.status() == Registration.Status.CONFIRMED));
                    }), new InboxActions(() -> new InboxSnapshot(List.of()), id -> { }, () -> { }), () -> { });
            stage.setScene(new Scene(view, 1280, 800)); stage.setTitle("Check-in smoke — synthetic fixtures"); stage.show();
            Thread.ofVirtual().start(() -> {
                try { exercise(); } catch (Throwable error) { failure = error; }
                finally { release.countDown(); Platform.runLater(() -> { view.close(); stage.close(); Platform.exit(); }); }
            });
        }
        private Registration checkIn(UUID id, Long version) {
            require(!Platform.isFxApplicationThread(), "check-in off FX");
            require(EVENT.equals(id), "selected event ID");
            require(version == current.get().version(), "displayed expected version");
            if (calls.incrementAndGet() == 1) {
                entered.countDown();
                try { require(release.await(5, TimeUnit.SECONDS), "release command"); }
                catch (InterruptedException error) { throw new AssertionError(error); }
            }
            String code = reject.getAndSet(null);
            if (code != null) throw new ApplicationException(code, "synthetic-sensitive-value");
            var result = row(Registration.Status.CHECKED_IN, version + 1); current.set(result); return result;
        }
        private void exercise() throws Exception {
            await(() -> list("attendee-events") != null && list("attendee-events").getItems().size() == 1);
            fx(() -> list("attendee-events").getSelectionModel().selectFirst());
            await(() -> view.lookup("#attendee-check-in") != null);
            fx(() -> {
                require(view.lookup("#attendee-register") == null, "no Register node for ongoing event");
                require(button("attendee-cancel").isDisabled(), "ongoing cancellation disabled");
                screenshot("check-in-details-1280.png");
                button("attendee-check-in").fire(); button("attendee-check-in").fire();
            });
            require(entered.await(5, TimeUnit.SECONDS), "command started");
            fx(() -> require(button("attendee-home").isDisabled(), "navigation disabled while checking in"));
            require(calls.get() == 1, "double submission prevented");
            release.countDown();
            await(() -> feedback().contains("You are checked in") && view.lookup("#attendee-check-in") == null);
            for (var outcome : List.of(
                    Map.entry("CHECK_IN_TOO_EARLY", "not open yet"), Map.entry("CHECK_IN_CLOSED", "closed"),
                    Map.entry("CHECK_IN_VENUE_UNAVAILABLE", "active venue"), Map.entry("REGISTRATION_NOT_FOUND", "not registered"),
                    Map.entry("REGISTRATION_CANCELLED", "cancelled"), Map.entry("ALREADY_CHECKED_IN", "already checked in"),
                    Map.entry("REGISTRATION_CHANGED", "changed"), Map.entry("UNAUTHENTICATED", "log in again"))) {
                current.set(row(Registration.Status.CONFIRMED, 2)); reject.set(outcome.getKey());
                fx(() -> button("attendee-refresh-details").fire());
                await(() -> view.lookup("#attendee-check-in") != null);
                fx(() -> button("attendee-check-in").fire());
                await(() -> feedback().contains(outcome.getValue()) && view.lookup("#attendee-check-in") != null);
                fx(() -> require(!feedback().contains("synthetic-sensitive-value"), "safe outcome text"));
            }
            current.set(null);
            fx(() -> button("attendee-refresh-details").fire());
            await(() -> view.lookup("#attendee-cancel") != null && view.lookup("#attendee-check-in") == null);
            fx(() -> require(view.lookup("#attendee-register") == null, "unregistered ongoing event has no Register"));
            current.set(row(Registration.Status.CONFIRMED, 7));
            fx(() -> button("attendee-registrations-nav").fire());
            await(() -> list("attendee-registrations") != null && list("attendee-registrations").getItems().size() == 1);
            fx(() -> list("attendee-registrations").getSelectionModel().selectFirst());
            fx(() -> {
                require(button("attendee-check-in-registration").isVisible(), "My Registrations check-in available");
                require(button("attendee-cancel-registration").isDisabled(), "no ongoing cancellation");
                screenshot("check-in-registrations-1280.png");
                stage.setWidth(1000); stage.setHeight(640);
            });
            await(() -> view.getWidth() <= 1000);
            fx(() -> { view.applyCss(); view.layout(); screenshot("check-in-registrations-1000.png"); });
            int before = calls.get();
            fx(() -> { button("attendee-check-in-registration").fire(); button("attendee-check-in-registration").fire(); });
            await(() -> list("attendee-registrations").getItems().size() == 1
                    && ((MyRegistration) list("attendee-registrations").getItems().getFirst()).status() == Registration.Status.CHECKED_IN);
            require(calls.get() == before + 1 && current.get().version() == 8, "one My Registrations command with exact version");
            fx(() -> {
                list("attendee-registrations").getSelectionModel().selectFirst();
                require(!button("attendee-check-in-registration").isVisible(), "checked-in cannot check in again");
            });
        }
        private Button button(String id) { return (Button) view.lookup("#" + id); }
        private ListView<?> list(String id) { return (ListView<?>) view.lookup("#" + id); }
        private String feedback() { return ((Label) view.lookup("#attendee-command-feedback")).getText(); }
        private void screenshot(String name) {
            try {
                var image = view.snapshot(null, null); int width = (int) image.getWidth(), height = (int) image.getHeight();
                int[] pixels = new int[width * height];
                image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
                var bitmap = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                bitmap.setRGB(0, 0, width, height, pixels, 0, width);
                var directory = Path.of("build", "attendee-smoke"); Files.createDirectories(directory);
                ImageIO.write(bitmap, "png", directory.resolve(name).toFile());
            } catch (java.io.IOException error) { throw new IllegalStateException(error); }
        }
        private static void await(BooleanSupplier condition) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < deadline) {
                FutureTask<Boolean> check = new FutureTask<>(condition::getAsBoolean);
                Platform.runLater(check); if (check.get(5, TimeUnit.SECONDS)) return; Thread.sleep(20);
            }
            throw new AssertionError("Timed out waiting for check-in UI");
        }
        private static void fx(Runnable action) throws Exception {
            FutureTask<Void> task = new FutureTask<>(action, null); Platform.runLater(task); task.get(5, TimeUnit.SECONDS);
        }
        private static void require(boolean condition, String label) { if (!condition) throw new AssertionError(label); }
    }
}
