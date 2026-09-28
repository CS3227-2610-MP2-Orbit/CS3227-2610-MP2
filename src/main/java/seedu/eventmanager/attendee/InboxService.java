package seedu.eventmanager.attendee;

import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.registration.AttendeeSessionGuard;
import seedu.eventmanager.service.TransactionManager;

/** Live-session, owner-only inbox workflows. No caller-supplied account IDs. */
public final class InboxService {
    private static final DateTimeFormatter SGT = DateTimeFormatter.ofPattern("d MMM uuuu, h:mm a 'SGT'", Locale.ENGLISH)
            .withZone(ZoneId.of("Asia/Singapore"));
    private final InboxRepository repository;
    private final AttendeeSessionGuard sessions;
    private final TransactionManager transactions;
    private final Clock clock;

    public InboxService(InboxRepository repository, Function<String, Actor> sessions,
            TransactionManager transactions, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.sessions = new AttendeeSessionGuard(sessions);
        this.transactions = Objects.requireNonNull(transactions);
        this.clock = Objects.requireNonNull(clock);
    }

    public InboxSnapshot list(String token) {
        var actor = sessions.require(token);
        var entries = repository.list(actor.userId());
        revalidate(token, actor);
        return new InboxSnapshot(entries.stream().map(InboxService::display).toList());
    }

    public void markRead(String token, UUID notificationId) {
        transactions.execute(() -> {
            var actor = sessions.require(token);
            if (notificationId == null || !repository.markRead(actor.userId(), notificationId, clock.instant())) {
                throw new ApplicationException("NOTIFICATION_NOT_FOUND", "Notification is not available.");
            }
            revalidate(token, actor);
            return null;
        });
    }

    public void markAllRead(String token) {
        transactions.execute(() -> {
            var actor = sessions.require(token);
            repository.markAllRead(actor.userId(), clock.instant());
            revalidate(token, actor);
            return null;
        });
    }

    private void revalidate(String token, Actor actor) {
        if (!actor.equals(sessions.require(token))) {
            throw new ApplicationException("UNAUTHENTICATED", "Please log in again.");
        }
    }

    private static InboxMessage display(InboxRepository.Entry entry) {
        String title = entry.eventTitle() == null ? "Event unavailable" : entry.eventTitle();
        String time = entry.eventStart() == null ? "Time unavailable" : SGT.format(entry.eventStart());
        String body = switch (entry.type()) {
            case "REGISTRATION_CONFIRMED" -> "Registration confirmed: " + title + " · " + time;
            case "REGISTRATION_CANCELLED" -> "Registration cancelled: " + title + " · " + time;
            case "EVENT_ANNOUNCEMENT" -> entry.announcementText() == null ? "Announcement removed." : entry.announcementText();
            default -> throw new ApplicationException("UNSUPPORTED_NOTIFICATION", "Unsupported attendee notification type.");
        };
        return new InboxMessage(entry.id(), entry.createdAt(), entry.readAt(), title, body);
    }
}
