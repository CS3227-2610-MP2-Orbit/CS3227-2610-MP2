package seedu.eventmanager.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiagnosticLogTest {
    @TempDir
    Path directory;

    @AfterEach
    void removeHandlers() {
        Logger root = Logger.getLogger("");
        for (Handler handler : root.getHandlers()) {
            if (handler.getClass().getName().contains("FileHandler")) {
                root.removeHandler(handler);
                handler.close();
            }
        }
        DiagnosticLog.resetForTests();
    }

    @Test
    void defaultDirectory_prefersEnvironmentOverride() {
        assertEquals(Path.of("/tmp/custom-logs"),
                DiagnosticLog.defaultDirectory(Map.of("EVENT_MANAGER_LOG_DIR", "/tmp/custom-logs"), "/home/tester"));
        assertEquals(Path.of("/home/tester", ".orbit", "logs"),
                DiagnosticLog.defaultDirectory(Map.of(), "/home/tester"));
    }

    @Test
    void install_writesStructuredEventsToTheLogFile() throws Exception {
        Path file = DiagnosticLog.install(directory);
        new JavaUtilStructuredLogger(DiagnosticLogTest.class).info("app_started", Map.of("version", "0.1.0"));
        flush();

        String contents = Files.readString(file);
        assertTrue(contents.contains("INFO"), contents);
        assertTrue(contents.contains("event=app_started version=0.1.0"), contents);
    }

    @Test
    void install_isIdempotent() {
        Path first = DiagnosticLog.install(directory);
        Path second = DiagnosticLog.install(directory);

        assertEquals(first, second);
        long fileHandlers = java.util.Arrays.stream(Logger.getLogger("").getHandlers())
                .filter(handler -> handler.getClass().getName().contains("FileHandler")).count();
        assertEquals(1, fileHandlers);
    }

    private static void flush() {
        for (Handler handler : Logger.getLogger("").getHandlers()) {
            handler.flush();
        }
    }
}
