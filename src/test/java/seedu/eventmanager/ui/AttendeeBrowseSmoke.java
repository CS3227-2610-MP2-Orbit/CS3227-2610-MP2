package seedu.eventmanager.ui;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.FutureTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelFormat;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import seedu.eventmanager.attendee.CatalogueEvent;
import seedu.eventmanager.attendee.CatalogueClub;
import seedu.eventmanager.attendee.AttendeeEventDetails;
import seedu.eventmanager.attendee.AttendeeEventDetailsService;
import seedu.eventmanager.attendee.AttendeeEventDetailsRepository;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.registration.Registration;
import seedu.eventmanager.registration.RegistrationEligibilityPolicy;
import seedu.eventmanager.attendee.EventCatalogueRepository;
import seedu.eventmanager.attendee.EventCatalogueService;
import seedu.eventmanager.event.Event;
import seedu.eventmanager.event.EventStatus;

/** Opt-in real JavaFX smoke test with synthetic service data, not database E2E. */
public final class AttendeeBrowseSmoke {
    private static volatile Throwable failure;

    private AttendeeBrowseSmoke() { }

    public static void main(String[] args) {
        Application.launch(SmokeApplication.class, args);
        if (failure != null) {
            throw new AssertionError("Attendee JavaFX smoke failed", failure);
        }
        System.out.println("Attendee JavaFX smoke PASS: personalized venue/seats/status, refresh, stale responses, session expiry, filters, error/retry, Home.");
    }

    public static final class SmokeApplication extends Application {
        private final AtomicBoolean failReads = new AtomicBoolean();
        private final AtomicBoolean failClubs = new AtomicBoolean();
        private final AtomicReference<CompletableFuture<List<CatalogueClub>>> pendingClubs = new AtomicReference<>();
        private volatile CountDownLatch clubsEntered;
        private final AtomicInteger homes = new AtomicInteger();
        private final AtomicBoolean failDetails = new AtomicBoolean();
        private final AtomicBoolean expired = new AtomicBoolean();
        private final AtomicInteger occupied = new AtomicInteger(3);
        private final AtomicBoolean delayNext = new AtomicBoolean();
        private volatile CountDownLatch entered;
        private volatile CountDownLatch release;
        private volatile CountDownLatch finished;
        private AttendeeBrowseView view;
        private Stage stage;

        @Override
        public void start(Stage primaryStage) {
            stage = primaryStage;
            var service = fixtureService();
            var detailService = new AttendeeEventDetailsService((id, attendee) -> {
                var event = service.getEvent(id);
                return Optional.of(new AttendeeEventDetailsRepository.Snapshot(event, "PUBLISHED",
                        Optional.of(new AttendeeEventDetails.Venue("Campus Seminar Room", "COM1 Level 2", "CONFIRMED", "ACTIVE")),
                        RegistrationEligibilityPolicy.Booking.CONFIRMED_ACTIVE, occupied.get(),
                        id.equals(new UUID(0, 1)) ? Optional.of(Registration.Status.CONFIRMED) : Optional.empty(),
                        id.equals(new UUID(0, 1)) ? 0 : -1));
            }, token -> {
                if (expired.get()) throw new IllegalArgumentException("Synthetic expired session");
                return new Actor(new UUID(0, 100), Role.ATTENDEE);
            }, Clock.fixed(Instant.parse("2026-09-25T00:00:00Z"), ZoneOffset.UTC));
            view = new AttendeeBrowseView(() -> {
                if (failReads.get()) {
                    throw new IllegalStateException("synthetic-sensitive-value");
                }
                return service;
            }, id -> {
                if (failDetails.get()) throw new IllegalStateException("synthetic-sensitive-value");
                var result = detailService.getEvent("synthetic-session", id);
                if (delayNext.compareAndSet(true, false)) {
                    entered.countDown();
                    // Simulate a driver returning after cancellation; it must never overwrite newer UI.
                    boolean done = false;
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                    while (!done && System.nanoTime() < deadline) {
                        try { done = release.await(5, TimeUnit.SECONDS); }
                        catch (InterruptedException ignored) { }
                    }
                    if (!done) throw new AssertionError("Delayed smoke fixture was not released");
                    finished.countDown();
                }
                return result;
            }, new AttendeeRegistrationActions((id, version) -> { throw new AssertionError("Unexpected register"); },
                    (id, version) -> { throw new AssertionError("Unexpected cancel"); },
                    (id, version) -> { throw new AssertionError("Unexpected check-in"); }, List::of),
                    new InboxActions(() -> new seedu.eventmanager.attendee.InboxSnapshot(List.of()), id -> { }, () -> { }),
                    homes::incrementAndGet, Clock.fixed(Instant.parse("2026-09-25T00:00:00Z"), ZoneOffset.UTC));
            stage.setScene(new Scene(view, 1280, 800));
            stage.setTitle("Attendee smoke — synthetic fixtures");
            stage.show();
            Thread.ofVirtual().name("attendee-ui-smoke").start(() -> {
                try {
                    exercise();
                } catch (Throwable error) {
                    failure = error;
                } finally {
                    if (release != null) release.countDown();
                    Platform.runLater(() -> { view.close(); stage.close(); Platform.exit(); });
                }
            });
        }

        private void exercise() throws Exception {
            await(() -> list().getItems().size() == 2);
            fx(() -> require(view.lookup("#attendee-club") instanceof ComboBox,
                    "club filter must be a dropdown, not a raw ID field"));
            await(() -> clubs().getItems().size() == 3);
            fx(() -> {
                require(clubs().getItems().stream().map(seedu.eventmanager.attendee.CatalogueClub::name).toList()
                        .equals(List.of("All clubs", "Campus Music", "Campus Technology")), "sorted named options");
                clubs().getSelectionModel().select(2);
                button("attendee-search").fire();
            });
            await(() -> list().getItems().size() == 1);
            fx(() -> {
                require(list().getItems().getFirst().clubId().equals("tech-club"), "filter uses ID, not display name");
                button("attendee-clear").fire();
            });
            await(() -> list().getItems().size() == 2);
            fx(() -> require(clubs().getValue().name().equals("All clubs"), "Clear restores All clubs"));
            fx(() -> list().getSelectionModel().selectFirst());
            await(() -> detailText().contains("Remaining seats: 17 / 20"));
            fx(() -> {
                require(detailText().contains("Campus Seminar Room · COM1 Level 2"), "venue detail");
                require(detailText().contains("Your registration: Registered"), "own status");
                require(detailText().contains("Club: Campus Technology") && !detailText().contains("tech-club"),
                        "details show club name, not internal ID");
                require(view.lookup("#attendee-register") != null && view.lookup("#attendee-cancel") != null,
                        "registration actions must replace the placeholder");
            });
            fx(() -> screenshot("browse-1280.png"));
            fx(() -> { stage.setWidth(1000); stage.setHeight(640); });
            await(() -> view.getWidth() <= 1000 && view.getHeight() <= 640);
            fx(() -> { view.applyCss(); view.layout(); screenshot("browse-1000.png"); });

            occupied.set(20);
            fx(() -> button("attendee-refresh-details").fire());
            await(() -> detailText().contains("Event is full"));
            fx(() -> require(detailText().contains("Your registration: Registered"), "registered status survives fullness"));
            occupied.set(3);
            failDetails.set(true);
            fx(() -> button("attendee-refresh-details").fire());
            await(() -> detailText().startsWith("\nUnable to load events."));
            fx(() -> require(!detailText().contains("synthetic-sensitive-value") && !detailText().contains("Your registration:"), "failure clears private state"));
            failDetails.set(false);
            fx(() -> button("attendee-refresh-details").fire());
            await(() -> detailText().contains("Remaining seats: 17 / 20"));

            entered = new CountDownLatch(1); release = new CountDownLatch(1); finished = new CountDownLatch(1);
            delayNext.set(true);
            fx(() -> button("attendee-refresh-details").fire());
            require(entered.await(5, TimeUnit.SECONDS), "slow detail started");
            fx(() -> list().getSelectionModel().select(1));
            await(() -> detailText().contains("Campus music evening"));
            release.countDown();
            require(finished.await(5, TimeUnit.SECONDS), "cancelled read finished");
            fx(() -> require(detailText().contains("Your registration: Not registered")
                    && !detailText().contains("AI workshop"), "stale result ignored"));

            expired.set(true);
            fx(() -> button("attendee-refresh-details").fire());
            await(() -> detailText().contains("Return Home and log in again"));
            fx(() -> require(!detailText().contains("Your registration:"), "expired session clears status"));
            expired.set(false);

            fx(() -> {
                ((TextField) view.lookup("#attendee-search-text")).setText("workshop");
                button("attendee-search").fire();
            });
            await(() -> list().getItems().size() == 1);
            fx(() -> {
                require(list().getItems().getFirst().title().equals("AI workshop"), "search result");
                ((TextField) view.lookup("#attendee-search-text")).setText("no-match");
                button("attendee-search").fire();
            });
            await(() -> feedback().startsWith("No matches."));

            fx(() -> {
                ((DatePicker) view.lookup("#attendee-from")).setValue(LocalDate.of(2026, 10, 2));
                ((DatePicker) view.lookup("#attendee-to")).setValue(LocalDate.of(2026, 10, 1));
                button("attendee-search").fire();
            });
            await(() -> feedback().startsWith("Check the date range"));

            failReads.set(true);
            fx(() -> button("attendee-clear").fire());
            await(() -> feedback().startsWith("Unable to load events."));
            fx(() -> {
                require(!feedback().contains("synthetic-sensitive-value"), "error redaction");
                screenshot("error-1000.png");
            });
            failReads.set(false);
            fx(() -> button("attendee-search").fire());
            await(() -> list().getItems().size() == 2);
            await(() -> !clubs().isDisabled());
            failClubs.set(true);
            fx(() -> {
                clubs().getSelectionModel().select(2);
                button("attendee-search").fire();
            });
            await(() -> ((Label) view.lookup("#attendee-club-feedback")).getText().startsWith("Unable to refresh clubs."));
            await(() -> list().getItems().size() == 1);
            fx(() -> require(clubs().getValue().id().equals("tech-club"), "club error retains selected filter"));
            failClubs.set(false);
            fx(() -> button("attendee-search").fire());
            await(() -> ((Label) view.lookup("#attendee-club-feedback")).getText().isEmpty());
            var staleClubs = new CompletableFuture<List<CatalogueClub>>();
            clubsEntered = new CountDownLatch(1);
            pendingClubs.set(staleClubs);
            fx(() -> button("attendee-search").fire());
            require(clubsEntered.await(5, TimeUnit.SECONDS), "slow club lookup started");
            fx(() -> button("attendee-clear").fire());
            await(() -> list().getItems().size() == 2 && !clubs().isDisabled());
            staleClubs.complete(List.of(new CatalogueClub("stale-club", "Stale response")));
            fx(() -> {
                require(clubs().getValue().name().equals("All clubs"), "Clear wins over stale club load");
                require(clubs().getItems().stream().noneMatch(item -> item.id().equals("stale-club")),
                        "stale club options ignored");
            });
            entered = new CountDownLatch(1); release = new CountDownLatch(1); finished = new CountDownLatch(1);
            delayNext.set(true);
            fx(() -> list().getSelectionModel().selectFirst());
            require(entered.await(5, TimeUnit.SECONDS), "close during load");
            fx(() -> button("attendee-home").fire());
            release.countDown();
            require(finished.await(5, TimeUnit.SECONDS), "closed read finished");
            fx(() -> require(!detailText().contains("Your registration:"), "Home clears personal state"));
            require(homes.get() == 1, "Home callback");
        }

        @SuppressWarnings("unchecked")
        private ComboBox<seedu.eventmanager.attendee.CatalogueClub> clubs() {
            return (ComboBox<seedu.eventmanager.attendee.CatalogueClub>) view.lookup("#attendee-club");
        }

        @SuppressWarnings("unchecked")
        private ListView<CatalogueEvent> list() {
            return (ListView<CatalogueEvent>) view.lookup("#attendee-events");
        }

        private String feedback() {
            return ((Label) view.lookup("#attendee-feedback")).getText();
        }

        private Button button(String id) {
            return (Button) view.lookup("#" + id);
        }

        private String detailText() {
            return ((VBox) view.lookup("#attendee-details")).getChildren().stream()
                    .filter(Label.class::isInstance).map(Label.class::cast)
                    .map(Label::getText).reduce("", (left, right) -> left + "\n" + right);
        }

        private void screenshot(String name) {
            try {
                var image = view.snapshot(null, null);
                int width = (int) image.getWidth();
                int height = (int) image.getHeight();
                int[] pixels = new int[width * height];
                image.getPixelReader().getPixels(0, 0, width, height,
                        PixelFormat.getIntArgbInstance(), pixels, 0, width);
                var bitmap = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                bitmap.setRGB(0, 0, width, height, pixels, 0, width);
                Path directory = Path.of("build", "attendee-smoke");
                Files.createDirectories(directory);
                ImageIO.write(bitmap, "png", directory.resolve(name).toFile());
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        private static void await(BooleanSupplier condition) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < deadline) {
                FutureTask<Boolean> check = new FutureTask<>(condition::getAsBoolean);
                Platform.runLater(check);
                if (check.get(5, TimeUnit.SECONDS)) {
                    return;
                }
                Thread.sleep(20);
            }
            throw new AssertionError("Timed out waiting for UI state");
        }

        private static void fx(Runnable action) throws Exception {
            FutureTask<Void> task = new FutureTask<>(action, null);
            Platform.runLater(task);
            task.get(5, TimeUnit.SECONDS);
        }

        private static void require(boolean condition, String label) {
            if (!condition) {
                throw new AssertionError(label);
            }
        }

        private EventCatalogueService fixtureService() {
            Instant now = Instant.parse("2026-09-25T00:00:00Z");
            List<Event> events = List.of(
                    new Event(new UUID(0, 1), "tech-club", "not-public", "AI workshop",
                            "A hands-on campus workshop. This is synthetic smoke-test data, not a published team event.",
                            now.plusSeconds(86400), now.plusSeconds(90000), 20, EventStatus.PUBLISHED, 0),
                    new Event(new UUID(0, 2), "music-club", "not-public", "Campus music evening",
                            "Synthetic fixture", now.plusSeconds(172800), now.plusSeconds(176400),
                            80, EventStatus.PUBLISHED, 0),
                    new Event(new UUID(0, 3), "private-club", "not-public", "Private draft",
                            "Must not appear", now.plusSeconds(86400), now.plusSeconds(90000),
                            20, EventStatus.DRAFT, 0));
            return new EventCatalogueService(new EventCatalogueRepository() {
                public List<Entry> findPublishedNotEnded(Instant time) {
                    return events.stream().map(event -> new Entry(event,
                            event.clubId().equals("tech-club") ? "Campus Technology" : "Campus Music")).toList();
                }
                public Optional<Entry> findPublishedById(UUID id) {
                    return findPublishedNotEnded(now).stream().filter(entry -> entry.event().id().equals(id)).findFirst();
                }
                public List<seedu.eventmanager.attendee.CatalogueClub> findClubs() {
                    require(!Platform.isFxApplicationThread(), "club lookup must run off FX thread");
                    if (failClubs.get()) throw new IllegalStateException("synthetic-sensitive-value");
                    var pending = pendingClubs.getAndSet(null);
                    if (pending != null) {
                        clubsEntered.countDown();
                        return pending.join(); // Driver may ignore cancellation; late results must be discarded.
                    }
                    return List.of(new seedu.eventmanager.attendee.CatalogueClub("tech-club", "Campus Technology"),
                            new seedu.eventmanager.attendee.CatalogueClub("music-club", "Campus Music"));
                }
            }, Clock.fixed(now, ZoneOffset.UTC));
        }
    }
}
