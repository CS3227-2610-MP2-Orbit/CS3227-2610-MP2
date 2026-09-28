package seedu.eventmanager.registration;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.service.AuditLogService;
import seedu.eventmanager.service.NotificationService;
import seedu.eventmanager.service.TransactionManager;

/** Authenticated attendee registration commands. Actor identity comes from a live session. */
public final class RegistrationService {
    private final RegistrationStore store;
    private final AttendeeSessionGuard sessions;
    private final TransactionManager transactions;
    private final AuditLogService audit;
    private final NotificationService notifications;
    private final Clock clock;

    public RegistrationService(RegistrationStore store, Function<String, Actor> sessions,
            TransactionManager transactions, AuditLogService audit, NotificationService notifications, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.sessions = new AttendeeSessionGuard(sessions);
        this.transactions = Objects.requireNonNull(transactions);
        this.audit = Objects.requireNonNull(audit);
        this.notifications = Objects.requireNonNull(notifications);
        this.clock = Objects.requireNonNull(clock);
    }

    /** expectedVersion=-1 means no previous registration; otherwise use the current own record's version. */
    public Registration register(String sessionToken, UUID eventId, long expectedVersion) {
        return change(sessionToken, eventId, expectedVersion, false);
    }

    public Registration cancel(String sessionToken, UUID eventId, long expectedVersion) {
        return change(sessionToken, eventId, expectedVersion, true);
    }

    public Registration checkIn(String sessionToken, UUID eventId, long expectedVersion) {
        return transactions.execute(() -> {
            Actor actor = sessions.require(sessionToken);
            RegistrationEvent event = lockEventAndAccount(actor, eventId, expectedVersion);
            // Acquire booking/venue locks before reading and eventually updating the registration.
            boolean booking = store.lockConfirmedActiveBooking(event);
            Registration current = store.find(eventId, actor.userId());
            if (current != null) requireVersion(current, expectedVersion);
            if (!actor.equals(sessions.require(sessionToken))) {
                throw problem("UNAUTHENTICATED", "Please log in again.");
            }
            // Read time only after potentially blocking locks and session revalidation.
            var now = clock.instant();
            var eligibility = CheckInPolicy.evaluate(event, current == null ? null : current.status(), booking, now);
            if (eligibility != CheckInPolicy.Result.AVAILABLE) {
                String code = switch (eligibility) {
                    case NOT_REGISTERED -> "REGISTRATION_NOT_FOUND";
                    case CANCELLED -> "REGISTRATION_CANCELLED";
                    case ALREADY_CHECKED_IN -> "ALREADY_CHECKED_IN";
                    case TOO_EARLY -> "CHECK_IN_TOO_EARLY";
                    case CLOSED -> "CHECK_IN_CLOSED";
                    case VENUE_UNAVAILABLE -> "CHECK_IN_VENUE_UNAVAILABLE";
                    case AVAILABLE -> throw new AssertionError("Handled above");
                };
                throw problem(code, "Check-in rejected: " + eligibility.name());
            }
            Registration updated = new Registration(current.id(), eventId, actor.userId(),
                    Registration.Status.CHECKED_IN, current.registeredAt(), null, now, current.version() + 1);
            store.save(updated);
            audit.record(actor, "REGISTRATION_CHECKED_IN", "REGISTRATION", updated.id(),
                    current.status().name(), updated.status().name(), null);
            // Audit-only: no unsupported check-in event is put into the notification outbox.
            return updated;
        });
    }

    public List<Registration> myRegistrations(String sessionToken) {
        return transactions.execute(() -> {
            Actor actor = sessions.require(sessionToken);
            requireActive(actor);
            return List.copyOf(store.findByAttendee(actor.userId()));
        });
    }

    private Registration change(String token, UUID eventId, long expectedVersion, boolean cancel) {
        return transactions.execute(() -> {
            Actor actor = sessions.require(token);
            RegistrationEvent event = lockEventAndAccount(actor, eventId, expectedVersion);
            Registration current = store.find(eventId, actor.userId());
            if (cancel && current == null) throw problem("REGISTRATION_NOT_FOUND", "Your registration was not found.");
            Registration.Status target = cancel ? Registration.Status.CANCELLED : Registration.Status.CONFIRMED;
            if (current != null && current.status() == target
                    && (expectedVersion == current.version() || expectedVersion == current.version() - 1)) {
                return current; // Exact retry, including a lost successful response: no extra effects.
            }
            requireVersion(current, expectedVersion);
            if (current != null && current.status() == Registration.Status.CHECKED_IN) {
                throw problem("ALREADY_CHECKED_IN", "A checked-in registration cannot be changed.");
            }
            var now = clock.instant();
            if (cancel) {
                if (!now.isBefore(event.startsAt())) {
                    throw problem("CANCELLATION_CLOSED", "Cancellation closes when the event starts.");
                }
            } else {
                if (!RegistrationEligibilityPolicy.eventOpen(event, now)) {
                    throw problem("EVENT_NOT_REGISTERABLE", "Only future published events can be registered.");
                }
                if (!store.lockConfirmedActiveBooking(event)) {
                    throw problem("VENUE_NOT_CONFIRMED", "A matching confirmed booking at an active venue is required.");
                }
                if (!RegistrationEligibilityPolicy.hasCapacity(event.capacity(), store.occupiedPlaces(eventId))) {
                    throw problem("EVENT_FULL", "This event is full.");
                }
                // Lock acquisition/counting can wait; do not use a pre-wait time.
                now = clock.instant();
                if (!RegistrationEligibilityPolicy.eventOpen(event, now)) {
                    throw problem("EVENT_NOT_REGISTERABLE", "Registration closes when the event starts.");
                }
            }
            Registration updated = new Registration(current == null ? UUID.randomUUID() : current.id(),
                    eventId, actor.userId(), target, cancel ? current.registeredAt() : now,
                    cancel ? now : null, null, current == null ? 0 : current.version() + 1);
            store.save(updated);
            String action = cancel ? "REGISTRATION_CANCELLED" : "REGISTRATION_CONFIRMED";
            audit.record(actor, action, "REGISTRATION", updated.id(),
                    current == null ? null : current.status().name(), target.name(), null);
            notifications.notify(actor.userId(), action, Map.of(
                    "registrationId", updated.id().toString(), "version", Long.toString(updated.version())));
            return updated;
        });
    }

    private void requireActive(Actor actor) {
        if (!store.lockActiveAttendee(actor.userId())) {
            throw problem("FORBIDDEN", "An active attendee account is required.");
        }
    }

    private RegistrationEvent lockEventAndAccount(Actor actor, UUID eventId, long expectedVersion) {
        if (eventId == null) throw problem("EVENT_NOT_FOUND", "Event was not found.");
        if (expectedVersion < -1) throw problem("INVALID_VERSION", "Registration version is invalid.");
        // Consistent write lock order: event, account, booking/venue, registration.
        RegistrationEvent event = store.lockEvent(eventId);
        if (event == null) throw problem("EVENT_NOT_FOUND", "Event was not found.");
        requireActive(actor);
        return event;
    }

    private static void requireVersion(Registration current, long expectedVersion) {
        if (current == null ? expectedVersion != -1 : expectedVersion != current.version()) {
            throw problem("REGISTRATION_CHANGED", "Registration changed. Refresh before trying again.");
        }
    }

    private static ApplicationException problem(String code, String message) {
        return new ApplicationException(code, message);
    }
}
