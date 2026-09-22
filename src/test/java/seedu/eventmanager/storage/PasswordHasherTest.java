package seedu.eventmanager.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {
    @Test
    void hashesAndVerifiesWithoutStoringPlaintext() {
        PasswordHasher hasher = new PasswordHasher();
        String first = hasher.hash("correct horse battery staple");
        String second = hasher.hash("correct horse battery staple");

        assertNotEquals("correct horse battery staple", first);
        assertNotEquals(first, second);
        assertTrue(hasher.matches("correct horse battery staple", first));
        assertFalse(hasher.matches("wrong password", first));
    }
}
