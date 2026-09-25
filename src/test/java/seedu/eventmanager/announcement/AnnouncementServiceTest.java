package seedu.eventmanager.announcement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.AccessDeniedException;
import seedu.eventmanager.common.EntityNotFoundException;
import seedu.eventmanager.common.ValidationException;
import seedu.eventmanager.event.Event;
import seedu.eventmanager.event.EventAuditRecord;
import seedu.eventmanager.event.EventDetails;
import seedu.eventmanager.event.EventRepository;
import seedu.eventmanager.event.EventService;
import seedu.eventmanager.event.OrganizerIdentity;
import seedu.eventmanager.registration.EventRegistrations;
import seedu.eventmanager.registration.RegisteredAttendee;
import seedu.eventmanager.service.NotificationService;

class AnnouncementServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-25T04:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final UUID OTHER_EVENT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e2");
    private static final UUID FIRST_ANNOUNCEMENT = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID SECOND_ANNOUNCEMENT = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000a11c");
    private static final UUID BEN = UUID.fromString("00000000-0000-0000-0000-000000000be1");
    private static final OrganizerIdentity ORGANIZER =
            new OrganizerIdentity("organizer-1", Set.of("club-1"));
    private static final OrganizerIdentity OTHER_ORGANIZER =
            new OrganizerIdentity("organizer-2", Set.of("club-2"));

    private MutableClock clock;
    private FakeRegistrations registrations;
    private InMemoryAnnouncementRepository announcements;
    private RecordingNotifications notifications;
    private AnnouncementService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(NOW);
        Deque<UUID> eventIds = new ArrayDeque<>(List.of(EVENT_ID, OTHER_EVENT_ID));
        EventService eventService = new EventService(new InMemoryEventRepository(), eventIds::pop, clock);
        createEvent(eventService, "club-1");
        createEvent(eventService, "club-1");
        registrations = new FakeRegistrations();
        announcements = new InMemoryAnnouncementRepository();
        notifications = new RecordingNotifications();
        Deque<UUID> announcementIds = new ArrayDeque<>(List.of(FIRST_ANNOUNCEMENT, SECOND_ANNOUNCEMENT));
        service = new AnnouncementService(
                eventService, registrations, announcements, notifications, announcementIds::pop, clock);
    }

    @Test
    void post_validMessage_persistsAnnouncementWithAudit() {
        AnnouncementResult result = service.post(ORGANIZER, EVENT_ID, "Doors open at 6pm");

        Announcement expected = new Announcement(FIRST_ANNOUNCEMENT, EVENT_ID, "organizer-1", "Doors open at 6pm", NOW);
        assertEquals(expected, result.announcement());
        assertEquals(List.of(expected), announcements.findByEventId(EVENT_ID));
        assertEquals(
                List.of(new AnnouncementAuditRecord(NOW, "organizer-1",
                        AnnouncementAuditRecord.Action.POST_ANNOUNCEMENT, EVENT_ID, FIRST_ANNOUNCEMENT)),
                announcements.auditRecords);
    }

    @Test
    void post_registrants_queuesOneNotificationEachWithIdsOnly() {
        registrations.register(EVENT_ID, ALICE, "Alice");
        registrations.register(EVENT_ID, BEN, "Ben");

        AnnouncementResult result = service.post(ORGANIZER, EVENT_ID, "Doors open at 6pm");

        Map<String, String> payload = Map.of(
                "announcementId", FIRST_ANNOUNCEMENT.toString(),
                "eventId", EVENT_ID.toString());
        assertEquals(
                List.of(
                        new Notification(ALICE, AnnouncementService.NOTIFICATION_EVENT, payload),
                        new Notification(BEN, AnnouncementService.NOTIFICATION_EVENT, payload)),
                notifications.sent);
        assertEquals(2, result.notificationsQueued());
        assertEquals(0, result.notificationFailures());
    }

    @Test
    void post_noRegistrants_savedWithNothingQueued() {
        AnnouncementResult result = service.post(ORGANIZER, EVENT_ID, "Doors open at 6pm");

        assertEquals(1, announcements.findByEventId(EVENT_ID).size());
        assertTrue(notifications.sent.isEmpty());
        assertEquals(0, result.notificationsQueued());
        assertEquals(0, result.notificationFailures());
    }

    @Test
    void post_blankMessage_rejectedWithoutChanges() {
        registrations.register(EVENT_ID, ALICE, "Alice");

        assertThrows(ValidationException.class, () -> service.post(ORGANIZER, EVENT_ID, "   "));
        assertThrows(ValidationException.class, () -> service.post(ORGANIZER, EVENT_ID, null));

        assertNothingChanged();
    }

    @Test
    void post_messageTrimmed() {
        AnnouncementResult result = service.post(ORGANIZER, EVENT_ID, "  Bring your student card  ");

        assertEquals("Bring your student card", result.announcement().message());
    }

    @Test
    void post_messageAtMaximumLength_accepted() {
        String message = "m".repeat(AnnouncementService.MAX_MESSAGE_LENGTH);

        assertEquals(message, service.post(ORGANIZER, EVENT_ID, message).announcement().message());
    }

    @Test
    void post_messageTooLong_rejectedWithoutChanges() {
        String message = "m".repeat(AnnouncementService.MAX_MESSAGE_LENGTH + 1);

        assertThrows(ValidationException.class, () -> service.post(ORGANIZER, EVENT_ID, message));

        assertNothingChanged();
    }

    @Test
    void post_unownedEvent_rejectedWithoutReadingRegistrations() {
        registrations.register(EVENT_ID, ALICE, "Alice");

        assertThrows(AccessDeniedException.class, () -> service.post(OTHER_ORGANIZER, EVENT_ID, "Hello"));

        assertNothingChanged();
        assertEquals(0, registrations.queries);
    }

    @Test
    void post_unknownEvent_rejectedWithoutReadingRegistrations() {
        UUID unknown = UUID.fromString("00000000-0000-0000-0000-00000000dead");

        assertThrows(EntityNotFoundException.class, () -> service.post(ORGANIZER, unknown, "Hello"));

        assertNothingChanged();
        assertEquals(0, registrations.queries);
    }

    @Test
    void post_notificationFailsForOneRecipient_announcementKeptAndOthersQueued() {
        registrations.register(EVENT_ID, ALICE, "Alice");
        registrations.register(EVENT_ID, BEN, "Ben");
        notifications.failFor.add(ALICE);

        AnnouncementResult result = service.post(ORGANIZER, EVENT_ID, "Doors open at 6pm");

        assertEquals(1, announcements.findByEventId(EVENT_ID).size());
        assertEquals(List.of(BEN), notifications.sent.stream().map(Notification::recipientId).toList());
        assertEquals(1, result.notificationsQueued());
        assertEquals(1, result.notificationFailures());
    }

    @Test
    void post_persistenceFails_errorPropagatesAndNothingQueued() {
        registrations.register(EVENT_ID, ALICE, "Alice");
        IllegalStateException failure = new IllegalStateException("database down");
        announcements.failWith = failure;

        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> service.post(ORGANIZER, EVENT_ID, "Hello"));

        assertEquals(failure, thrown);
        assertTrue(notifications.sent.isEmpty());
    }

    @Test
    void list_returnsOnlyThisEventsAnnouncementsNewestFirst() {
        service.post(ORGANIZER, EVENT_ID, "First");
        clock.advanceSeconds(60);
        service.post(ORGANIZER, EVENT_ID, "Second");
        announcements.add(
                new Announcement(UUID.randomUUID(), OTHER_EVENT_ID, "organizer-1", "Elsewhere", NOW),
                new AnnouncementAuditRecord(NOW, "organizer-1",
                        AnnouncementAuditRecord.Action.POST_ANNOUNCEMENT, OTHER_EVENT_ID, UUID.randomUUID()));

        List<Announcement> listed = service.list(ORGANIZER, EVENT_ID);

        assertEquals(List.of("Second", "First"), listed.stream().map(Announcement::message).toList());
    }

    @Test
    void list_unownedEvent_rejected() {
        assertThrows(AccessDeniedException.class, () -> service.list(OTHER_ORGANIZER, EVENT_ID));
    }

    private void assertNothingChanged() {
        assertTrue(announcements.findByEventId(EVENT_ID).isEmpty());
        assertTrue(announcements.auditRecords.isEmpty());
        assertTrue(notifications.sent.isEmpty());
    }

    private static void createEvent(EventService eventService, String clubId) {
        eventService.createEvent(ORGANIZER, clubId, new EventDetails(
                "Campus Night",
                "Demo",
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T12:00:00Z"),
                80));
    }

    private record Notification(UUID recipientId, String event, Map<String, String> data) { }

    private static final class RecordingNotifications implements NotificationService {
        private final List<Notification> sent = new ArrayList<>();
        private final Set<UUID> failFor = new HashSet<>();

        @Override
        public void notify(UUID recipientId, String event, Map<String, String> data) {
            if (failFor.contains(recipientId)) {
                throw new IllegalStateException("Could not enqueue notification.");
            }
            sent.add(new Notification(recipientId, event, Map.copyOf(data)));
        }
    }

    private static final class FakeRegistrations implements EventRegistrations {
        private final Map<UUID, List<RegisteredAttendee>> byEvent = new HashMap<>();
        private int queries;

        void register(UUID eventId, UUID attendeeId, String name) {
            byEvent.computeIfAbsent(eventId, ignored -> new ArrayList<>())
                    .add(new RegisteredAttendee(attendeeId, name));
        }

        @Override
        public List<RegisteredAttendee> registeredAttendees(UUID eventId) {
            queries++;
            return List.copyOf(byEvent.getOrDefault(eventId, List.of()));
        }
    }

    private static final class InMemoryAnnouncementRepository implements AnnouncementRepository {
        private final List<Announcement> stored = new ArrayList<>();
        private final List<AnnouncementAuditRecord> auditRecords = new ArrayList<>();
        private RuntimeException failWith;

        @Override
        public List<Announcement> findByEventId(UUID eventId) {
            return stored.stream()
                    .filter(a -> a.eventId().equals(eventId))
                    .sorted(Comparator.comparing(Announcement::createdAt).reversed())
                    .toList();
        }

        @Override
        public void add(Announcement announcement, AnnouncementAuditRecord auditRecord) {
            if (failWith != null) {
                throw failWith;
            }
            stored.add(announcement);
            auditRecords.add(auditRecord);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static final class InMemoryEventRepository implements EventRepository {
        private final Map<UUID, Event> store = new HashMap<>();

        @Override
        public Optional<Event> findById(UUID eventId) {
            return Optional.ofNullable(store.get(eventId));
        }

        @Override
        public List<Event> findByClubIds(Set<String> clubIds) {
            return store.values().stream().filter(e -> clubIds.contains(e.clubId())).toList();
        }

        @Override
        public void create(Event event, EventAuditRecord auditRecord) {
            store.put(event.id(), event);
        }

        @Override
        public void update(Event event, long expectedVersion, EventAuditRecord auditRecord) {
            store.put(event.id(), event);
        }
    }
}
