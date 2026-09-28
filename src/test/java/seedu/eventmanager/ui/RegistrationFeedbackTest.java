package seedu.eventmanager.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.ApplicationException;

class RegistrationFeedbackTest {
    @Test void allServiceRejectionsHaveSpecificSafeFeedback() {
        Map<String, String> cases = Map.ofEntries(
                Map.entry("UNAUTHENTICATED", "log in again"), Map.entry("FORBIDDEN", "log in again"),
                Map.entry("EVENT_FULL", "full"), Map.entry("EVENT_NOT_REGISTERABLE", "closed"),
                Map.entry("VENUE_NOT_CONFIRMED", "active venue"), Map.entry("REGISTRATION_CHANGED", "changed"),
                Map.entry("INVALID_VERSION", "changed"), Map.entry("CANCELLATION_CLOSED", "already started"),
                Map.entry("ALREADY_CHECKED_IN", "checked in"), Map.entry("REGISTRATION_NOT_FOUND", "not registered"),
                Map.entry("REGISTRATION_CANCELLED", "cancelled"), Map.entry("CHECK_IN_TOO_EARLY", "not open yet"),
                Map.entry("CHECK_IN_CLOSED", "closed"), Map.entry("CHECK_IN_VENUE_UNAVAILABLE", "active venue"),
                Map.entry("EVENT_NOT_FOUND", "no longer available"));
        cases.forEach((code, expected) -> {
            String message = RegistrationFeedback.failure(new ApplicationException(code, "synthetic-sensitive-value"));
            assertTrue(message.contains(expected), code);
            assertFalse(message.contains("synthetic-sensitive-value"));
        });
    }

    @Test void unknownFailureDoesNotClaimTransactionFailedOrExposeRawDetails() {
        for (Throwable failure : new Throwable[]{new IllegalStateException("synthetic-sensitive-value"),
                new ApplicationException("FUTURE_CODE", "synthetic-sensitive-value")}) {
            String message = RegistrationFeedback.failure(failure);
            assertTrue(message.contains("Unable to confirm the outcome"));
            assertFalse(message.contains("synthetic-sensitive-value"));
        }
    }
}
