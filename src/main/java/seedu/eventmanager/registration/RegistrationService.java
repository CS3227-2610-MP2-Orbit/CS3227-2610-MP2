package seedu.eventmanager.registration;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.service.AuditLogService;
import seedu.eventmanager.service.NotificationService;
import seedu.eventmanager.service.TransactionManager;

/** Authenticated attendee registration commands. Actor identity comes from a live session. */
public final class RegistrationService {
    private final RegistrationStore store;
    private final Function<String, Actor> sessions;
    private final TransactionManager transactions;
    private final AuditLogService audit;
    private final NotificationService notifications;
    private final Clock clock;

    public RegistrationService(RegistrationStore store, Function<String, Actor> sessions,
            TransactionManager transactions, AuditLogService audit, NotificationService notifications, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.sessions = Objects.requireNonNull(sessions);
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

    public List<Registration> myRegistrations(String sessionToken) {
        return transactions.execute(() -> {
            Actor actor = authenticate(sessionToken);
            requireActive(actor);
            return List.copyOf(store.findByAttendee(actor.userId()));
        });
    }

    private Registration change(String token, UUID eventId, long expectedVersion, boolean cancel) {
        return transactions.execute(() -> {
            Actor actor = authenticate(token);
            if (eventId == null) throw problem("EVENT_NOT_FOUND", "Event was not found.");
            if (expectedVersion < -1) throw problem("INVALID_VERSION", "Registration version is invalid.");
            // Consistent write lock order: event, account, booking/venue, registration.
            RegistrationEvent event = store.lockEvent(eventId);
            if (event == null) throw problem("EVENT_NOT_FOUND", "Event was not found.");
            requireActive(actor);
            Registration current = store.find(eventId, actor.userId());
            if (cancel && current == null) throw problem("REGISTRATION_NOT_FOUND", "Your registration was not found.");
            Registration.Status target = cancel ? Registration.Status.CANCELLED : Registration.Status.CONFIRMED;
            if (current != null && current.status() == target
                    && (expectedVersion == current.version() || expectedVersion == current.version() - 1)) {
                return current; // Exact retry, including a lost successful response: no extra effects.
            }
            if (current == null ? expectedVersion != -1 : expectedVersion != current.version()) {
                throw problem("REGISTRATION_CHANGED", "Registration changed. Refresh before trying again.");
            }
            if (current != null && current.status() == Registration.Status.CHECKED_IN) {
                throw problem("ALREADY_CHECKED_IN", "A checked-in registration cannot be changed.");
            }
            var now = clock.instant();
            if (cancel) {
                if (!now.isBefore(event.startsAt())) {
                    throw problem("CANCELLATION_CLOSED", "Cancellation closes when the event starts.");
                }
            } else {
                if (!"PUBLISHED".equals(event.status()) || !now.isBefore(event.startsAt())) {
                    throw problem("EVENT_NOT_REGISTERABLE", "Only future published events can be registered.");
                }
                if (!store.lockConfirmedActiveBooking(event)) {
                    throw problem("VENUE_NOT_CONFIRMED", "A matching confirmed booking at an active venue is required.");
                }
                if (store.occupiedPlaces(eventId) >= event.capacity()) {
                    throw problem("EVENT_FULL", "This event is full.");
                }
                // Lock acquisition/counting can wait; do not use a pre-wait time.
                now = clock.instant();
                if (!now.isBefore(event.startsAt())) {
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

    private Actor authenticate(String token) {
        if (token == null || token.isBlank()) throw problem("UNAUTHENTICATED", "Please log in again.");
        final Actor actor;
        try {
            actor = sessions.apply(token);
        } catch (IllegalArgumentException invalidSession) {
            throw problem("UNAUTHENTICATED", "Please log in again.");
        }
        if (actor == null || actor.userId() == null) throw problem("UNAUTHENTICATED", "Please log in again.");
        if (actor.role() != Role.ATTENDEE) throw problem("FORBIDDEN", "An attendee account is required.");
        return actor;
    }

    private void requireActive(Actor actor) {
        if (!store.lockActiveAttendee(actor.userId())) {
            throw problem("FORBIDDEN", "An active attendee account is required.");
        }
    }

    private static ApplicationException problem(String code, String message) {
        return new ApplicationException(code, message);
    }
}
