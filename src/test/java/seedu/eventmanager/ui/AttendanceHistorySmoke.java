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
import seedu.eventmanager.common.ApplicationException;
/** Real desktop controls with synthetic history callbacks, not real-login/database E2E. */
public final class AttendanceHistorySmoke {
    private static volatile Throwable failure;
    public static void main(String[] args) {
        Application.launch(SmokeApplication.class, args);
        if (failure != null) throw new AssertionError("Attendance history UI smoke failed", failure);
        System.out.println("History UI smoke PASS: navigation, side-by-side SGT details, empty/error/expiry, off-FX reads, stale refresh/navigation/Home, no commands.");
    }

    public static final class SmokeApplication extends Application {
        private final AttendanceRecord row = new AttendanceRecord(new UUID(0, 1), "Campus engineering workshop",
                "A completed workshop with event details and a recorded check-in.", "engineering-club",
                Instant.parse("2026-09-20T10:00:00Z"), Instant.parse("2026-09-20T12:00:00Z"),
                "COMPLETED", "Seminar room · Level 2", Instant.parse("2026-09-20T10:05:00Z"), "Campus Engineering");
        private final AtomicReference<Supplier<List<AttendanceRecord>>> reply = new AtomicReference<>(() -> List.of(row));
        private final AtomicInteger reads = new AtomicInteger();
        private final AtomicBoolean home = new AtomicBoolean();
        private final List<CompletableFuture<List<AttendanceRecord>>> pending = new ArrayList<>();
        private AttendeeBrowseView view;
        private Stage stage;

        @Override public void start(Stage stage) {
            this.stage = stage;
            var catalogue = new EventCatalogueService(new EventCatalogueRepository() {
                public List<Entry> findPublishedNotEnded(Instant now) { return List.of(); }
                public Optional<Entry> findPublishedById(UUID id) { return Optional.empty(); }
                public List<seedu.eventmanager.attendee.CatalogueClub> findClubs() { return List.of(); }
            }, Clock.systemUTC());
            view = new AttendeeBrowseView(() -> catalogue, id -> { throw new AssertionError("No catalogue detail read"); },
                    new AttendeeRegistrationActions((id, version) -> { throw new AssertionError("No history register"); },
                            (id, version) -> { throw new AssertionError("No history cancel"); },
                            (id, version) -> { throw new AssertionError("No history check-in"); }, List::of),
                    new InboxActions(() -> new InboxSnapshot(List.of()), id -> { }, () -> { }),
                    () -> {
                        require(!Platform.isFxApplicationThread(), "history off FX");
                        var response = reply.get(); reads.incrementAndGet(); return response.get();
                    }, () -> home.set(true));
            stage.setScene(new Scene(view, 1280, 800)); stage.setTitle("Attendance history — synthetic UI fixtures"); stage.show();
            Thread.ofVirtual().start(() -> {
                try { exercise(); } catch (Throwable error) { failure = error; }
                finally {
                    pending.forEach(future -> future.complete(List.of()));
                    Platform.runLater(() -> { view.close(); stage.close(); Platform.exit(); });
                }
            });
        }

        private void exercise() throws Exception {
            fx(() -> button("attendee-history-nav").fire());
            await(() -> rows() != null && rows().getItems().size() == 1);
            fx(() -> {
                rows().getSelectionModel().selectFirst();
                require(view.lookupAll(".split-pane").size() == 1 && view.lookupAll(".tab-pane").isEmpty(), "side-by-side, no new tab");
                String details = detailText();
                require(details.contains("20 Sept 2026, 6:05 PM SGT") || details.contains("20 Sep 2026, 6:05 PM SGT"), "SGT check-in time: " + details);
                require(details.contains("COMPLETED") && details.contains("Seminar room") && details.contains(row.description()), "event details");
                require(details.contains("Club: Campus Engineering") && !details.contains("engineering-club"),
                        "history uses human-readable club name");
                require(view.lookup("#attendee-register") == null && view.lookup("#attendee-check-in") == null
                        && view.lookup("#attendee-cancel") == null, "read-only history");
                screenshot("attendance-history-1280.png"); stage.setWidth(1000); stage.setHeight(640);
            });
            await(() -> view.getWidth() <= 1000);
            fx(() -> { view.applyCss(); view.layout(); screenshot("attendance-history-1000.png"); });
            reply.set(List::of); fx(() -> button("attendee-history-refresh").fire());
            await(() -> feedback().contains("No checked-in"));
            fx(() -> require(!detailText().contains(row.title()), "empty clears details"));
            for (String code : List.of("DATABASE_FAILURE", "UNAUTHENTICATED", "FORBIDDEN")) {
                reply.set(() -> { throw new ApplicationException(code, "synthetic-sensitive-detail"); });
                fx(() -> button("attendee-history-refresh").fire());
                await(() -> feedback().contains(code.equals("DATABASE_FAILURE") ? "Unable to load" : "log in again"));
                fx(() -> require(rows().getItems().isEmpty() && !feedback().contains("synthetic-sensitive"), "safe empty failure"));
            }
            // Two overlapping reads; old read ignores interruption and must never replace the newer result.
            var old = blockNextRead(); int before = reads.get();
            fx(() -> button("attendee-history-refresh").fire()); await(() -> reads.get() > before);
            reply.set(List::of); fx(() -> button("attendee-history-refresh").fire());
            await(() -> feedback().contains("No checked-in"));
            old.complete(List.of(row)); fx(() -> require(rows().getItems().isEmpty(), "stale refresh ignored"));
            var leaving = blockNextRead(); final int leavingBefore = reads.get();
            fx(() -> button("attendee-history-refresh").fire()); await(() -> reads.get() > leavingBefore);
            fx(() -> button("attendee-browse-nav").fire()); leaving.complete(List.of(row));
            reply.set(() -> List.of(row)); fx(() -> button("attendee-history-nav").fire());
            await(() -> rows() != null && rows().getItems().size() == 1);
            var closing = blockNextRead(); final int closingBefore = reads.get();
            fx(() -> button("attendee-history-refresh").fire()); await(() -> reads.get() > closingBefore);
            fx(() -> button("attendee-home").fire()); closing.complete(List.of(row));
            fx(() -> require(home.get() && rows().getItems().isEmpty(), "Home clears private data and cancels reads"));
        }

        private CompletableFuture<List<AttendanceRecord>> blockNextRead() {
            var future = new CompletableFuture<List<AttendanceRecord>>(); pending.add(future); reply.set(future::join); return future;
        }
        private Button button(String id) { return (Button) view.lookup("#" + id); }
        private ListView<?> rows() { return (ListView<?>) view.lookup("#attendee-history-list"); }
        private String feedback() { return ((Label) view.lookup("#attendee-history-feedback")).getText(); }
        private String detailText() {
            return view.lookup("#attendee-history-details").lookupAll(".label").stream()
                    .map(node -> ((Label) node).getText()).reduce("", (a, b) -> a + "\n" + b);
        }
        private void screenshot(String name) {
            try {
                view.applyCss(); view.layout();
                var snapshot = view.snapshot(null, null); int width = (int) snapshot.getWidth(), height = (int) snapshot.getHeight();
                int[] pixels = new int[width * height];
                snapshot.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
                var bitmap = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                bitmap.setRGB(0, 0, width, height, pixels, 0, width);
                Files.createDirectories(Path.of("build", "attendee-smoke"));
                ImageIO.write(bitmap, "png", Path.of("build", "attendee-smoke", name).toFile());
            } catch (java.io.IOException error) { throw new IllegalStateException(error); }
        }
        private static void await(BooleanSupplier condition) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < deadline) {
                FutureTask<Boolean> check = new FutureTask<>(condition::getAsBoolean); Platform.runLater(check);
                if (check.get(5, TimeUnit.SECONDS)) return; Thread.sleep(20);
            }
            throw new AssertionError("Timed out waiting for attendance history UI");
        }
        private static void fx(Runnable action) throws Exception {
            FutureTask<Void> task = new FutureTask<>(action, null); Platform.runLater(task); task.get(5, TimeUnit.SECONDS);
        }
        private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    }
}
