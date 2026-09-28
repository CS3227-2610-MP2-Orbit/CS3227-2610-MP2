package seedu.eventmanager.common;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Routes java.util.logging output, including uncaught errors, to a rotating local log file.
 * Diagnostic only: business audit records stay in the database audit tables.
 */
public final class DiagnosticLog {
    private static final int FILE_LIMIT_BYTES = 1_000_000;
    private static final int FILE_COUNT = 5;
    private static final String FORMAT = "%1$tFT%1$tT.%1$tL%1$tz %4$s %3$s %5$s%6$s%n";
    private static Path installed;

    private DiagnosticLog() { }

    /** Directory from EVENT_MANAGER_LOG_DIR, otherwise ~/.event-venue-manager/logs. */
    public static Path defaultDirectory(Map<String, String> environment, String userHome) {
        String override = environment.get("EVENT_MANAGER_LOG_DIR");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return Path.of(userHome, ".event-venue-manager", "logs");
    }

    /** Installs the file handler once and returns the current log file. */
    public static synchronized Path install(Path directory) {
        if (installed != null) {
            return installed;
        }
        try {
            Files.createDirectories(directory);
            System.setProperty("java.util.logging.SimpleFormatter.format", FORMAT);
            FileHandler handler = new FileHandler(
                    directory.resolve("app-%g.log").toString(), FILE_LIMIT_BYTES, FILE_COUNT, true);
            handler.setFormatter(new SimpleFormatter());
            handler.setLevel(Level.INFO);
            Logger.getLogger("").addHandler(handler);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not open the diagnostic log in " + directory, exception);
        }
        Logger uncaught = Logger.getLogger("seedu.eventmanager.uncaught");
        Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
                uncaught.log(Level.SEVERE, "event=uncaught_exception thread=" + thread.getName(), error));
        installed = directory.resolve("app-0.log");
        return installed;
    }

    static synchronized void resetForTests() {
        installed = null;
        Thread.setDefaultUncaughtExceptionHandler(null);
    }
}
