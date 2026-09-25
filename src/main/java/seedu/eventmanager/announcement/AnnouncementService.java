package seedu.eventmanager.announcement;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.event.EventService;
import seedu.eventmanager.event.OrganizerIdentity;
import seedu.eventmanager.registration.EventRegistrations;
import seedu.eventmanager.registration.RegisteredAttendee;
import seedu.eventmanager.service.NotificationService;

/** Organizer workflow: post announcements for an owned event and queue notifications to registrants. */
public final class AnnouncementService {
    public static final int MAX_MESSAGE_LENGTH = 1000;
    public static final String NOTIFICATION_EVENT = "EVENT_ANNOUNCEMENT";

    private final EventService eventService;
    private final EventRegistrations registrations;
    private final AnnouncementRepository announcements;
    private final NotificationService notifications;
    private final EventService.IdGenerator idGenerator;
    private final Clock clock;

    public AnnouncementService(
            EventService eventService,
            EventRegistrations registrations,
            AnnouncementRepository announcements,
            NotificationService notifications,
            EventService.IdGenerator idGenerator,
            Clock clock) {
        this.eventService = Objects.requireNonNull(eventService, "eventService");
        this.registrations = Objects.requireNonNull(registrations, "registrations");
        this.announcements = Objects.requireNonNull(announcements, "announcements");
        this.notifications = Objects.requireNonNull(notifications, "notifications");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public AnnouncementResult post(OrganizerIdentity actor, UUID eventId, String message) {
        requireOwnedEvent(actor, eventId);
        String normalized = normalizeMessage(message);

        Instant now = clock.instant();
        Announcement announcement = new Announcement(idGenerator.nextId(), eventId, actor.userId(), normalized, now);
        announcements.add(announcement, new AnnouncementAuditRecord(
                now, actor.userId(), AnnouncementAuditRecord.Action.POST_ANNOUNCEMENT, eventId, announcement.id()));

        // The outbox is written outside the announcement transaction, so queueing is best-effort per recipient.
        Map<String, String> payload = Map.of(
                "announcementId", announcement.id().toString(),
                "eventId", eventId.toString());
        int queued = 0;
        int failed = 0;
        for (RegisteredAttendee attendee : registrations.registeredAttendees(eventId)) {
            try {
                notifications.notify(attendee.attendeeId(), NOTIFICATION_EVENT, payload);
                queued++;
            } catch (RuntimeException notificationFailure) {
                failed++;
            }
        }
        return new AnnouncementResult(announcement, queued, failed);
    }

    public List<Announcement> list(OrganizerIdentity actor, UUID eventId) {
        requireOwnedEvent(actor, eventId);
        return announcements.findByEventId(eventId);
    }

    private void requireOwnedEvent(OrganizerIdentity actor, UUID eventId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(eventId, "eventId");
        eventService.getEvent(actor, eventId);
    }

    private static String normalizeMessage(String message) {
        String normalized = message == null ? "" : message.strip();
        if (normalized.isEmpty()) {
            throw new ValidationException("Announcement message must not be blank");
        }
        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            throw new ValidationException(
                    "Announcement message must not exceed " + MAX_MESSAGE_LENGTH + " characters");
        }
        return normalized;
    }
}
