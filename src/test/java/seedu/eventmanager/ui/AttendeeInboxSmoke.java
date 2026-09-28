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
import seedu.eventmanager.event.Event;

/** Real workspace/controller interactions with synthetic callbacks, not database E2E. */
public final class AttendeeInboxSmoke {
    private static volatile Throwable failure;
    public static void main(String[] args) {
        Application.launch(SmokeApplication.class, args);
        if (failure != null) throw new AssertionError("Inbox UI smoke failed", failure);
        System.out.println("Inbox UI smoke PASS: badge, own list, mark one/all, duplicate guard, refresh/error/expiry, stale reads and Home.");
    }

    public static final class SmokeApplication extends Application {
        private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");
        private final AtomicReference<List<InboxMessage>> records = new AtomicReference<>(List.of(
                new InboxMessage(new UUID(0, 2), NOW, null, "Campus workshop", "Bring your laptop. This is a synthetic announcement."),
                new InboxMessage(new UUID(0, 1), NOW.minusSeconds(60), null, "Campus workshop",
                        "Registration confirmed: Campus workshop · 1 Jan 2030, 9:00 AM SGT")));
        private final AtomicInteger marks = new AtomicInteger();
        private final AtomicInteger reads = new AtomicInteger();
        private final AtomicBoolean fail = new AtomicBoolean();
        private final AtomicBoolean expired = new AtomicBoolean();
        private final AtomicBoolean delayRead = new AtomicBoolean();
        private final AtomicBoolean delayMark = new AtomicBoolean();
        private volatile CountDownLatch entered, release, finished;
        private AttendeeBrowseView view;
        private Stage stage;

        @Override public void start(Stage stage) {
            this.stage = stage;
            var catalogue = new EventCatalogueService(new EventCatalogueRepository() {
                public List<Entry> findPublishedNotEnded(Instant now) { return List.of(); }
                public Optional<Entry> findPublishedById(UUID id) { return Optional.empty(); }
                public List<seedu.eventmanager.attendee.CatalogueClub> findClubs() { return List.of(); }
            }, Clock.fixed(NOW, ZoneOffset.UTC));
            view = new AttendeeBrowseView(() -> catalogue, id -> { throw new AssertionError("Unused details"); },
                    new AttendeeRegistrationActions((id, v) -> { throw new AssertionError("Unused register"); },
                            (id, v) -> { throw new AssertionError("Unused cancel"); },
                            (id, v) -> { throw new AssertionError("Unused check-in"); }, List::of),
                    new InboxActions(this::list, this::mark, () -> mark(null)), () -> { });
            stage.setTitle("Attendee inbox smoke — synthetic fixtures");
            stage.setScene(new Scene(view, 1280, 800)); stage.show();
            Thread.ofVirtual().start(() -> {
                try { exercise(); }
                catch (Throwable error) { failure = error; }
                finally {
                    if (release != null) release.countDown();
                    Platform.runLater(() -> { view.close(); stage.close(); Platform.exit(); });
                }
            });
        }

        private InboxSnapshot list() {
            require(!Platform.isFxApplicationThread(), "list off FX thread");
            reads.incrementAndGet();
            if (expired.get()) throw new ApplicationException("UNAUTHENTICATED", "synthetic-sensitive-value");
            if (fail.get()) throw new IllegalStateException("synthetic-sensitive-value");
            var snapshot = new InboxSnapshot(records.get());
            if (delayRead.compareAndSet(true, false)) pause();
            return snapshot;
        }
        private void mark(UUID id) {
            require(!Platform.isFxApplicationThread(), "mark off FX thread");
            marks.incrementAndGet();
            if (delayMark.compareAndSet(true, false)) pause();
            records.updateAndGet(rows -> rows.stream().map(row -> id == null || row.id().equals(id)
                    ? new InboxMessage(row.id(), row.createdAt(), NOW, row.title(), row.body()) : row).toList());
        }
        private void pause() {
            entered.countDown();
            boolean done = false;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (!done && System.nanoTime() < deadline) {
                try { done = release.await(5, TimeUnit.SECONDS); }
                catch (InterruptedException ignored) { } // Simulate JDBC finishing despite cancellation.
            }
            require(done, "release delayed callback");
            finished.countDown();
        }
        private void delay() { entered = new CountDownLatch(1); release = new CountDownLatch(1); finished = new CountDownLatch(1); }

        private void exercise() throws Exception {
            await(() -> button("attendee-notifications-nav").getText().contains("2 unread"));
            fx(() -> button("attendee-notifications-nav").fire());
            await(() -> messages().getItems().size() == 2);
            fx(() -> {
                require(filter() != null, "read/unread filter must be available");
                require(filter().getValue().toString().equals("All"), "default filter is All");
                chooseFilter("Read");
                require(messages().getItems().isEmpty(), "Read initially has no messages");
                require(((Label) messages().getPlaceholder()).getText().contains("filter"), "filtered empty state");
                require(button("attendee-notifications-nav").getText().contains("2 unread"), "badge is not filtered");
                chooseFilter("All");
                require(messages().getItems().size() == 2, "All restores messages");
            });
            fx(() -> screenshot("inbox-1280.png"));
            fx(() -> { stage.setWidth(1000); stage.setHeight(640); });
            await(() -> view.getWidth() <= 1000);
            fx(() -> { view.applyCss(); view.layout(); screenshot("inbox-1000.png"); });
            delay(); delayMark.set(true);
            fx(() -> {
                chooseFilter("Unread");
                messages().getSelectionModel().selectFirst();
                button("attendee-inbox-mark-read").fire();
                button("attendee-inbox-mark-read").fire();
            });
            require(entered.await(5, TimeUnit.SECONDS), "mark started");
            fx(() -> require(button("attendee-home").isDisabled(), "navigation blocked during command"));
            require(marks.get() == 1, "duplicate click suppressed");
            release.countDown();
            await(() -> button("attendee-notifications-nav").getText().contains("1 unread") && messages().getItems().size() == 1);
            fx(() -> {
                require(filter().getValue().toString().equals("Unread"), "filter retained after mark refresh");
                require(((InboxMessage) messages().getItems().getFirst()).unread(), "marked row leaves Unread");
                chooseFilter("Read");
                require(messages().getItems().size() == 1, "Read contains only marked row");
                messages().getSelectionModel().selectFirst();
                require(button("attendee-inbox-mark-read").isDisabled(), "read row cannot be marked again");
                button("attendee-inbox-mark-all").fire();
            });
            await(() -> button("attendee-notifications-nav").getText().contains("0 unread"));
            fx(() -> {
                require(button("attendee-inbox-mark-all").isDisabled(), "all-read disables action");
                require(filter().getValue().toString().equals("Read") && messages().getItems().size() == 2,
                        "mark-all includes unread rows hidden by Read filter");
                chooseFilter("Unread");
                require(messages().getItems().isEmpty(), "Unread is empty after mark-all");
                chooseFilter("All");
            });
            fail.set(true);
            fx(() -> button("attendee-inbox-refresh").fire());
            await(() -> summary().startsWith("Unable"));
            fx(() -> require(messages().getItems().isEmpty() && !summary().contains("synthetic-sensitive-value"), "safe failure clears rows"));
            fail.set(false); expired.set(true);
            fx(() -> button("attendee-inbox-refresh").fire());
            await(() -> summary().contains("log in again"));
            expired.set(false);
            delay(); delayRead.set(true);
            fx(() -> button("attendee-inbox-refresh").fire());
            require(entered.await(5, TimeUnit.SECONDS), "slow read started");
            records.set(List.of());
            fx(() -> button("attendee-inbox-refresh").fire());
            await(() -> summary().startsWith("0 notification"));
            release.countDown();
            require(finished.await(5, TimeUnit.SECONDS), "stale read finished");
            fx(() -> require(messages().getItems().isEmpty(), "late response cannot restore stale rows"));
            delay(); delayRead.set(true);
            fx(() -> button("attendee-inbox-refresh").fire());
            require(entered.await(5, TimeUnit.SECONDS), "read during Home");
            fx(() -> button("attendee-home").fire());
            release.countDown();
            require(finished.await(5, TimeUnit.SECONDS), "closed read finished");
            fx(() -> require(messages().getItems().isEmpty(), "closed view stays cleared"));
        }

        private Button button(String id) { return (Button) view.lookup("#" + id); }
        private ComboBox<?> filter() { return (ComboBox<?>) view.lookup("#attendee-inbox-filter"); }
        private void chooseFilter(String label) {
            for (int index = 0; index < filter().getItems().size(); index++) {
                if (filter().getItems().get(index).toString().equals(label)) {
                    filter().getSelectionModel().select(index);
                    return;
                }
            }
            throw new AssertionError("Missing filter: " + label);
        }
        private ListView<?> messages() { return (ListView<?>) view.lookup("#attendee-inbox-messages"); }
        private String summary() { return ((Label) view.lookup("#attendee-inbox-summary")).getText(); }
        private void screenshot(String name) {
            try {
                var image = view.snapshot(null, null);
                int width = (int) image.getWidth(), height = (int) image.getHeight();
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
                Platform.runLater(check);
                if (check.get(5, TimeUnit.SECONDS)) return;
                Thread.sleep(20);
            }
            throw new AssertionError("Timed out waiting for inbox UI");
        }
        private static void fx(Runnable action) throws Exception {
            FutureTask<Void> task = new FutureTask<>(action, null); Platform.runLater(task); task.get(5, TimeUnit.SECONDS);
        }
        private static void require(boolean condition, String label) { if (!condition) throw new AssertionError(label); }
    }
}
