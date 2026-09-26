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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelFormat;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import seedu.eventmanager.attendee.CatalogueEvent;
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
        System.out.println("Attendee JavaFX smoke PASS: browse, details, filters, empty, validation, error/retry, Home.");
    }

    public static final class SmokeApplication extends Application {
        private final AtomicBoolean failReads = new AtomicBoolean();
        private final AtomicInteger homes = new AtomicInteger();
        private AttendeeBrowseView view;
        private Stage stage;

        @Override
        public void start(Stage primaryStage) {
            stage = primaryStage;
            var service = fixtureService();
            view = new AttendeeBrowseView(() -> {
                if (failReads.get()) {
                    throw new IllegalStateException("synthetic-sensitive-value");
                }
                return service;
            }, homes::incrementAndGet);
            stage.setScene(new Scene(view, 1280, 800));
            stage.setTitle("Attendee smoke — synthetic fixtures");
            stage.show();
            Thread.ofVirtual().name("attendee-ui-smoke").start(() -> {
                try {
                    exercise();
                } catch (Throwable error) {
                    failure = error;
                } finally {
                    Platform.runLater(() -> { view.close(); stage.close(); Platform.exit(); });
                }
            });
        }

        private void exercise() throws Exception {
            await(() -> list().getItems().size() == 2);
            fx(() -> list().getSelectionModel().selectFirst());
            await(() -> detailText().contains("Event capacity: 20 (not remaining seats)"));
            fx(() -> screenshot("browse-1280.png"));
            fx(() -> { stage.setWidth(1000); stage.setHeight(640); });
            await(() -> view.getWidth() <= 1000 && view.getHeight() <= 640);
            fx(() -> { view.applyCss(); view.layout(); screenshot("browse-1000.png"); });

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
            fx(() -> button("attendee-home").fire());
            require(homes.get() == 1, "Home callback");
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

        private static EventCatalogueService fixtureService() {
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
                public List<Event> findUpcomingPublished(Instant time) { return events; }
                public Optional<Event> findPublishedById(UUID id) {
                    return events.stream().filter(event -> event.id().equals(id)).findFirst();
                }
            }, Clock.fixed(now, ZoneOffset.UTC));
        }
    }
}
