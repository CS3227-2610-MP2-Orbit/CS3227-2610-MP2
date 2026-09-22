package seedu.eventmanager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Verifies the initial application entry point. */
class MainTest {
    @Test
    void main_startsWithoutThrowing() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("DATABASE_INTEGRATION_TESTS")));
        assertDoesNotThrow(() -> Main.main(new String[0]));
    }
}
