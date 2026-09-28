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

/** Real JavaFX controls/controllers with synthetic callbacks; not PostgreSQL or login E2E. */
public final class AttendeeRegistrationSmoke {
    private static volatile Throwable failure;

    public static void main(String[] args) {
        Application.launch(SmokeApplication.class, args);
        if (failure != null) throw new AssertionError("Registration UI smoke failed", failure);
        System.out.println("Registration UI smoke PASS: register/cancel/re-register, exact versions, background/duplicate guard, My Registrations, rejections, refresh and expired session.");
    }

    public static final class SmokeApplication extends Application {
        private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");
        private static final UUID EVENT = new UUID(0, 1);
        private static final UUID OWNER = new UUID(0, 10);
        private final AtomicReference<Registration> current = new AtomicReference<>();
        private final AtomicReference<String> reject = new AtomicReference<>();
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicBoolean expired = new AtomicBoolean();
        private final AtomicBoolean empty = new AtomicBoolean();
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final List<Long> versions = new CopyOnWriteArrayList<>();
        private AttendeeBrowseView view;
        private Stage stage;

        @Override public void start(Stage stage) {
            this.stage = stage;
            var event = new Event(EVENT, "tech-club", "synthetic-organizer", "Campus AI workshop",
                    "Synthetic registration UI fixture.", NOW.plusSeconds(3600), NOW.plusSeconds(7200),
                    20, EventStatus.PUBLISHED, 0);
            var catalogue = new EventCatalogueService(new EventCatalogueRepository() {
                public List<Event> findUpcomingPublished(Instant now) { return List.of(event); }
                public Optional<Event> findPublishedById(UUID id) { return Optional.of(event); }
            }, Clock.fixed(NOW, ZoneOffset.UTC));
            view = new AttendeeBrowseView(() -> catalogue, id -> {
                checkSession();
                Registration row = current.get();
                return new AttendeeEventDetails(catalogue.getEvent(id),
                        Optional.of(new AttendeeEventDetails.Venue("Seminar Room", "COM1 Level 2", "CONFIRMED", "ACTIVE")),
                        row == null || row.status() == Registration.Status.CANCELLED ? 0 : 1,
                        row == null || row.status() == Registration.Status.CANCELLED ? 20 : 19,
                        Optional.ofNullable(row).map(Registration::status), row == null ? -1 : row.version(),
                        RegistrationEligibilityPolicy.Result.AVAILABLE);
            }, new AttendeeRegistrationActions((id, version) -> command(id, version, false),
                    (id, version) -> command(id, version, true), () -> {
                        require(!Platform.isFxApplicationThread(), "list read must run off FX thread");
                        checkSession();
                        if (empty.get() || current.get() == null) return List.of();
                        var row = current.get();
                        return List.of(new MyRegistration(EVENT, event.title(), event.startsAt(), "Seminar Room · COM1 Level 2",
                                row.status(), row.version(), row.status() == Registration.Status.CONFIRMED,
                                event.endsAt(), event.clubId(), event.description(), "PUBLISHED"),
                                new MyRegistration(new UUID(0, 2), "Past workshop", NOW.minusSeconds(3600), "Historic room",
                                        Registration.Status.CHECKED_IN, 3, false, NOW.minusSeconds(1),
                                        "test-club", "Past workshop description", "COMPLETED"),
                                new MyRegistration(new UUID(0, 3), "Cancelled music booking", NOW.plusSeconds(86400), "Music room",
                                        Registration.Status.CANCELLED, 1, false, NOW.plusSeconds(90000),
                                        "music-club", "Cancelled booking description", "PUBLISHED"));
                    }), new InboxActions(() -> new InboxSnapshot(List.of()), id -> { }, () -> { }), () -> { });
            stage.setScene(new Scene(view, 1280, 800));
            stage.setTitle("Registration smoke — synthetic fixtures");
            stage.show();
            Thread.ofVirtual().start(() -> {
                try { exercise(); }
                catch (Throwable error) { failure = error; }
                finally {
                    release.countDown();
                    Platform.runLater(() -> { view.close(); stage.close(); Platform.exit(); });
                }
            });
        }

        private void checkSession() {
            if (expired.get()) throw new ApplicationException("UNAUTHENTICATED", "synthetic-sensitive-value");
        }

        private Registration command(UUID id, long version, boolean cancelling) {
            require(!Platform.isFxApplicationThread(), "command must run off FX thread");
            require(EVENT.equals(id), "selected event ID");
            int count = calls.incrementAndGet();
            versions.add(version);
            if (count == 1) {
                entered.countDown();
                try { require(release.await(5, TimeUnit.SECONDS), "release command"); }
                catch (InterruptedException error) { throw new AssertionError(error); }
            }
            String code = reject.getAndSet(null);
            if (code != null) {
                if (code.equals("UNAUTHENTICATED")) expired.set(true);
                throw new ApplicationException(code, "synthetic-sensitive-value");
            }
            Registration before = current.get();
            require(version == (before == null ? -1 : before.version()), "must submit displayed version");
            Registration row = new Registration(new UUID(0, 100), EVENT, OWNER,
                    cancelling ? Registration.Status.CANCELLED : Registration.Status.CONFIRMED,
                    NOW, cancelling ? NOW : null, null, version + 1);
            current.set(row);
            return row;
        }

        private void exercise() throws Exception {
            await(() -> list("attendee-events").getItems().size() == 1);
            fx(() -> list("attendee-events").getSelectionModel().selectFirst());
            await(() -> view.lookup("#attendee-register") != null);
            fx(() -> { button("attendee-register").fire(); button("attendee-register").fire(); });
            require(entered.await(5, TimeUnit.SECONDS), "background command started");
            fx(() -> {
                require(button("attendee-register").isDisabled(), "register disabled while pending");
                require(button("attendee-home").isDisabled(), "Home blocked until outcome");
                require(button("attendee-registrations-nav").isDisabled(), "other command entry points blocked");
            });
            require(calls.get() == 1, "one command for double click");
            release.countDown();
            await(() -> buttonExists("attendee-cancel") && !button("attendee-cancel").isDisabled());
            fx(() -> require(message().contains("You are registered"), "register feedback persists after refresh"));
            fx(() -> button("attendee-cancel").fire());
            await(() -> buttonExists("attendee-register") && button("attendee-register").getText().equals("Re-register"));
            fx(() -> button("attendee-register").fire());
            await(() -> buttonExists("attendee-cancel") && !button("attendee-cancel").isDisabled());
            require(versions.equals(List.of(-1L, 0L, 1L)), "no record, cancellation and re-registration versions");
            fx(() -> button("attendee-registrations-nav").fire());
            // SplitPane children enter CSS lookup only after its first skin/layout pulse.
            await(() -> list("attendee-registrations") != null
                    && list("attendee-registrations").getItems().size() == 3);
            fx(() -> require(view.lookup("#attendee-registration-split") instanceof SplitPane,
                    "bookings and details must share a side-by-side split pane"));
            fx(() -> {
                selectRegistration(new UUID(0, 2));
                require(button("attendee-cancel-registration").isDisabled(), "checked-in cannot cancel");
                selectRegistration(new UUID(0, 3));
                require(button("attendee-cancel-registration").isDisabled(), "cancelled cannot cancel");
                selectRegistration(EVENT);
                screenshot("my-registrations-1280.png");
                stage.setWidth(1000); stage.setHeight(640);
            });
            await(() -> view.getWidth() <= 1000);
            fx(() -> { view.applyCss(); view.layout(); screenshot("my-registrations-1000.png"); });
            fx(() -> button("attendee-cancel-registration").fire());
            await(() -> list("attendee-registrations").getItems().size() == 3
                    && list("attendee-registrations").getItems().stream().map(MyRegistration.class::cast)
                    .anyMatch(row -> row.eventId().equals(EVENT) && row.status() == Registration.Status.CANCELLED));
            require(versions.getLast() == 2L, "My Registrations cancellation version");
            empty.set(true);
            fx(() -> button("attendee-refresh-registrations").fire());
            await(() -> ((Label) view.lookup("#attendee-registrations-feedback")).getText().startsWith("0 registration"));
            empty.set(false);
            fx(() -> button("attendee-browse-nav").fire());
            await(() -> list("attendee-events").getItems().size() == 1);
            fx(() -> list("attendee-events").getSelectionModel().selectFirst());
            await(() -> buttonExists("attendee-register"));
            for (var test : List.of(Map.entry("EVENT_FULL", "full"), Map.entry("EVENT_NOT_REGISTERABLE", "closed"),
                    Map.entry("REGISTRATION_CHANGED", "changed"), Map.entry("UNAUTHENTICATED", "log in again"))) {
                reject.set(test.getKey());
                fx(() -> button("attendee-register").fire());
                await(() -> message().contains(test.getValue()) && !button("attendee-home").isDisabled());
                if (!expired.get()) await(() -> buttonExists("attendee-register"));
                fx(() -> require(!message().contains("synthetic-sensitive-value"), "no exception details shown"));
            }
            await(() -> !buttonExists("attendee-register"));
            fx(() -> button("attendee-registrations-nav").fire());
            await(() -> ((Label) view.lookup("#attendee-registrations-feedback")).getText().contains("log in again"));
            fx(() -> require(list("attendee-registrations").getItems().isEmpty(), "expired session clears own bookings"));
            fx(this::exerciseFiltersAndDetails);
            // Let the new fixture scene complete real rendering pulses before capturing its ScrollPane.
            CountDownLatch rendered = new CountDownLatch(1);
            fx(() -> new javafx.animation.AnimationTimer() {
                private int frames;
                @Override public void handle(long now) {
                    if (++frames == 2) { stop(); rendered.countDown(); }
                }
            }.start());
            require(rendered.await(5, TimeUnit.SECONDS), "fixture scene rendered");
            fx(() -> screenshot("registration-details-1000.png"));
        }

        @SuppressWarnings("unchecked")
        private void exerciseFiltersAndDetails() {
            var samples = List.of(
                    sample(1, "Future workshop", 3600, 7200, Registration.Status.CONFIRMED),
                    sample(2, "Ongoing workshop", 0, 3600, Registration.Status.CONFIRMED),
                    sample(3, "Past workshop", -3600, 0, Registration.Status.CHECKED_IN),
                    sample(4, "Cancelled workshop", -3600, 3600, Registration.Status.CANCELLED));
            var screen = new MyRegistrationsView(() -> { }, row -> { }, Clock.fixed(NOW, ZoneOffset.UTC));
            screen.loaded(samples);
            stage.setScene(new Scene(screen, 1000, 640));
            screen.applyCss(); screen.layout();
            var split = (SplitPane) screen.lookup("#attendee-registration-split");
            var rows = (ListView<MyRegistration>) screen.lookup("#attendee-registrations");
            var details = (javafx.scene.layout.VBox) screen.lookup("#attendee-registration-details");
            require(screen.lookupAll(".tab-pane").isEmpty(), "no separate details tabs");
            require(split.getItems().getFirst() == rows && split.getItems().get(1) instanceof ScrollPane,
                    "bookings left, scrollable details right");
            var filter = (ComboBox<RegistrationListQuery.Filter>) screen.lookup("#attendee-registration-filter");
            var order = (ComboBox<RegistrationListQuery.Order>) screen.lookup("#attendee-registration-order");
            require(rows.getItems().size() == 4, "All includes cancelled and every event period");
            order.setValue(RegistrationListQuery.Order.LATEST);
            require(rows.getItems().getFirst().title().equals("Future workshop"), "latest first");
            order.setValue(RegistrationListQuery.Order.EARLIEST);
            require(rows.getItems().getFirst().title().equals("Past workshop"), "earliest first with stable ties");
            screenshot("registration-filters-1000.png");
            for (var expected : List.of(Map.entry(RegistrationListQuery.Filter.PAST, "Past workshop"),
                    Map.entry(RegistrationListQuery.Filter.ONGOING, "Ongoing workshop"),
                    Map.entry(RegistrationListQuery.Filter.UPCOMING, "Future workshop"),
                    Map.entry(RegistrationListQuery.Filter.CANCELLED, "Cancelled workshop"))) {
                filter.setValue(expected.getKey());
                require(rows.getItems().size() == 1 && rows.getItems().getFirst().title().equals(expected.getValue()),
                        "filter shows only " + expected.getValue());
                rows.getSelectionModel().selectFirst();
                screen.applyCss(); screen.layout();
                require(rows.localToScene(rows.getBoundsInLocal()).getMaxX()
                        <= details.localToScene(details.getBoundsInLocal()).getMinX(),
                        "selected details stay beside the visible bookings list");
                String text = details.getChildren().stream().filter(Label.class::isInstance).map(Label.class::cast)
                        .map(Label::getText).reduce("", (a, b) -> a + "\n" + b);
                require(text.contains(expected.getValue() + " description") && text.contains("Ends:")
                        && text.contains("Club: test-club") && text.contains("SGT"), "full owner event details");
            }
            screen.applyCss(); screen.layout();
            require(details.getChildren().size() > 1 && ((Label) details.getChildren().getFirst()).getText().equals("Cancelled workshop"),
                    "details remain selected after layout");
            // Refresh after a command must retain the chosen filter and remove stale selected details.
            screen.loading();
            require(details.getChildren().size() == 1 && ((Label) details.getChildren().getFirst()).getText().startsWith("Select a booking"),
                    "loading clears old event details");
            screen.loaded(samples.subList(0, 3));
            require(filter.getValue() == RegistrationListQuery.Filter.CANCELLED && rows.getItems().isEmpty(),
                    "filter retained and no-match empty state");
            require(rows.getSelectionModel().getSelectedItem() == null, "refresh clears selection");
            filter.setValue(RegistrationListQuery.Filter.ALL);
            require(rows.getItems().size() == 3, "All restores visible bookings");
            rows.getSelectionModel().selectFirst();
            screen.failed("Synthetic expired session");
            require(rows.getItems().isEmpty() && details.getChildren().size() == 1
                    && ((Label) details.getChildren().getFirst()).getText().startsWith("Select a booking"),
                    "failed read clears details and list");
            screen.loaded(samples);
            filter.setValue(RegistrationListQuery.Filter.CANCELLED);
            rows.getSelectionModel().selectFirst();
        }

        private MyRegistration sample(long id, String title, long start, long end, Registration.Status status) {
            return new MyRegistration(new UUID(0, id), title, NOW.plusSeconds(start), "Seminar Room · COM1",
                    status, 1, start > 0 && status == Registration.Status.CONFIRMED, NOW.plusSeconds(end),
                    "test-club", title + " description", end <= 0 ? "COMPLETED" : "PUBLISHED");
        }

        private String message() { return ((Label) view.lookup("#attendee-command-feedback")).getText(); }
        private boolean buttonExists(String id) { return view.lookup("#" + id) instanceof Button; }
        private Button button(String id) { return (Button) view.lookup("#" + id); }
        private ListView<?> list(String id) {
            return (ListView<?>) view.lookup("#" + id);
        }

        private void selectRegistration(UUID id) {
            var list = list("attendee-registrations");
            for (int index = 0; index < list.getItems().size(); index++) {
                if (((MyRegistration) list.getItems().get(index)).eventId().equals(id)) {
                    list.getSelectionModel().select(index);
                    return;
                }
            }
            throw new AssertionError("Missing synthetic registration");
        }

        private void screenshot(String name) {
            try {
                var image = stage.getScene().getRoot().snapshot(null, null);
                int width = (int) image.getWidth(), height = (int) image.getHeight();
                int[] pixels = new int[width * height];
                image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
                var bitmap = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                bitmap.setRGB(0, 0, width, height, pixels, 0, width);
                Path directory = Path.of("build", "attendee-smoke");
                Files.createDirectories(directory);
                ImageIO.write(bitmap, "png", directory.resolve(name).toFile());
            } catch (java.io.IOException error) { throw new IllegalStateException(error); }
        }

        private static void await(BooleanSupplier condition) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < deadline) {
                FutureTask<Boolean> check = new FutureTask<>(condition::getAsBoolean);
                Platform.runLater(check);
                if (check.get(5, TimeUnit.SECONDS)) return;
                Thread.sleep(20);
            }
            throw new AssertionError("Timed out waiting for registration UI state");
        }

        private static void fx(Runnable action) throws Exception {
            FutureTask<Void> task = new FutureTask<>(action, null);
            Platform.runLater(task); task.get(5, TimeUnit.SECONDS);
        }

        private static void require(boolean condition, String message) {
            if (!condition) throw new AssertionError(message);
        }
    }
}
