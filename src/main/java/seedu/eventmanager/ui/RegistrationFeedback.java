package seedu.eventmanager.ui;

import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.registration.Registration;

/** Allowlisted UI text shared by both command entry points; never displays raw exception messages. */
final class RegistrationFeedback {
    private RegistrationFeedback() { }

    static String success(Registration registration) {
        return switch (registration.status()) {
            case CONFIRMED -> "You are registered for this event.";
            case CANCELLED -> "Your registration is cancelled. You may re-register while eligible and seats remain.";
            case CHECKED_IN -> "You are checked in for this event.";
        };
    }

    static String failure(Throwable failure) {
        if (failure instanceof ApplicationException application) {
            return switch (application.code()) {
                case "UNAUTHENTICATED", "FORBIDDEN" -> "Your attendee session is no longer valid. Return Home and log in again.";
                case "EVENT_FULL" -> "Registration rejected: this event is full. No seat was reserved.";
                case "EVENT_NOT_REGISTERABLE" -> "Registration is closed: the event must be published and not yet started.";
                case "VENUE_NOT_CONFIRMED" -> "Registration unavailable: a matching confirmed booking at an active venue is required.";
                case "REGISTRATION_CHANGED", "INVALID_VERSION" -> "Your registration changed since it was displayed. Review the refreshed status before trying again.";
                case "CANCELLATION_CLOSED" -> "Cancellation is closed: the event has already started.";
                case "ALREADY_CHECKED_IN" -> "You are already checked in; this registration cannot be changed.";
                case "REGISTRATION_NOT_FOUND" -> "You are not registered for this event. Review the refreshed status.";
                case "REGISTRATION_CANCELLED" -> "Check-in rejected: your registration is cancelled.";
                case "CHECK_IN_TOO_EARLY" -> "Check-in is not open yet. It opens when the event starts.";
                case "CHECK_IN_CLOSED" -> "Check-in is closed: the event has ended or is no longer published.";
                case "CHECK_IN_VENUE_UNAVAILABLE" -> "Check-in unavailable: a matching confirmed booking at an active venue is required.";
                case "EVENT_NOT_FOUND" -> "This event is no longer available.";
                default -> uncertain();
            };
        }
        return uncertain();
    }

    private static String uncertain() {
        return "Unable to confirm the outcome. Refresh and check your registration before retrying.";
    }
}
