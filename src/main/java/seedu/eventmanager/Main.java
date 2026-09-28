package seedu.eventmanager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import seedu.eventmanager.demo.DemoDataSeeder;
import seedu.eventmanager.ui.EventManagerApplication;

/** Starts the Event Venue Manager application. */
public final class Main {
    /** The release version from the jar manifest (set by build.gradle), or "development" from source. */
    public static final String VERSION = versionOf(Main.class.getPackage().getImplementationVersion());

    static String versionOf(String manifestVersion) {
        return manifestVersion == null || manifestVersion.isBlank() ? "development" : manifestVersion.strip();
    }

    private Main() {
    }

    /** Starts the application. */
    public static void main(String[] args) {
        if (args.length == 1 && "--version".equals(args[0])) {
            System.out.println("Event Venue Manager " + VERSION);
            return;
        }
        if (args.length == 1 && "--seed-demo".equals(args[0])) {
            DemoDataSeeder.main(new String[0]);
            return;
        }
        if (args.length == 1 && "--check-javafx".equals(args[0])) {
            checkJavaFx();
            return;
        }
        Application.launch(EventManagerApplication.class, args);
    }

    /** Starts and stops the JavaFX toolkit so a release check proves this OS's native libraries load. */
    private static void checkJavaFx() {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        try {
            if (!started.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("JavaFX did not start within 30 seconds");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting JavaFX", exception);
        }
        Platform.exit();
        System.out.println("JavaFX runtime OK on " + System.getProperty("os.name") + " "
                + System.getProperty("os.arch"));
    }
}
