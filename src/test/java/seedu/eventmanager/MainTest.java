package seedu.eventmanager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Verifies the initial application entry point. */
class MainTest {
    @Test
    void main_startsWithoutThrowing() {
        assertDoesNotThrow(() -> Main.main(new String[] {"--version"}));
    }

    @Test
    void versionOf_usesTheJarManifestVersion() {
        assertEquals("1.0.0", Main.versionOf("1.0.0"));
    }

    @Test
    void versionOf_reportsDevelopmentWhenNotRunFromAJar() {
        assertEquals("development", Main.versionOf(null));
        assertEquals("development", Main.versionOf(" "));
    }
}
