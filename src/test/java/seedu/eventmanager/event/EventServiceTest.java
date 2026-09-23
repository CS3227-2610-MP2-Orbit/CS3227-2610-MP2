package seedu.eventmanager.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
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

class EventServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-22T10:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final OrganizerIdentity ORGANIZER =
            new OrganizerIdentity("organizer-1", Set.of("club-1"));
    private static final EventDetails VALID_DETAILS = new EventDetails(
            "Campus Night",
            "An evening event",
            Instant.parse("2026-10-01T10:00:00Z"),
            Instant.parse("2026-10-01T12:00:00Z"),
            80);

    private InMemoryEventRepository repository;
    private EventService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEventRepository();
        service = new EventService(repository, () -> EVENT_ID, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createEvent_validCommand_persistsDraftAndAuditRecord() {
        Event created = service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);

        assertEquals(EVENT_ID, created.id());
        assertEquals("club-1", created.clubId());
        assertEquals("organizer-1", created.organizerId());
        assertEquals(EventStatus.DRAFT, created.status());
        assertEquals(0, created.version());
        assertEquals(created, repository.findById(EVENT_ID).orElseThrow());
        assertEquals(
                new EventAuditRecord(
                        NOW,
                        "organizer-1",
                        EventAuditRecord.Action.CREATE_EVENT,
                        EVENT_ID,
                        0),
                repository.auditRecords.getFirst());
    }

    @Test
    void createEvent_unownedClub_rejectedWithoutPersistenceOrAudit() {
        assertThrows(
                AccessDeniedException.class,
                () -> service.createEvent(ORGANIZER, "club-2", VALID_DETAILS));

        assertEquals(0, repository.events.size());
        assertEquals(0, repository.auditRecords.size());
    }

    @Test
    void createEvent_invalidDetails_rejectedWithoutPersistenceOrAudit() {
        List<EventDetails> invalidDetails = List.of(
                new EventDetails(" ", "Description", VALID_DETAILS.startsAt(), VALID_DETAILS.endsAt(), 10),
                new EventDetails(null, "Description", VALID_DETAILS.startsAt(), VALID_DETAILS.endsAt(), 10),
                new EventDetails("Title", "Description", VALID_DETAILS.startsAt(), VALID_DETAILS.startsAt(), 10),
                new EventDetails("Title", "Description", VALID_DETAILS.endsAt(), VALID_DETAILS.startsAt(), 10),
                new EventDetails("Title", "Description", VALID_DETAILS.startsAt(), VALID_DETAILS.endsAt(), 0),
                new EventDetails("Title", "Description", VALID_DETAILS.startsAt(), VALID_DETAILS.endsAt(), -1));

        for (EventDetails details : invalidDetails) {
            assertThrows(
                    ValidationException.class,
                    () -> service.createEvent(ORGANIZER, "club-1", details));
        }

        assertEquals(0, repository.events.size());
        assertEquals(0, repository.auditRecords.size());
    }

    @Test
    void editEvent_ownedDraft_updatesDetailsAndVersionWithAudit() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        EventDetails editedDetails = new EventDetails(
                "Updated Campus Night",
                "Updated description",
                Instant.parse("2026-10-02T10:00:00Z"),
                Instant.parse("2026-10-02T13:00:00Z"),
                100);

        Event edited = service.editEvent(ORGANIZER, EVENT_ID, 0, editedDetails);

        assertEquals("Updated Campus Night", edited.title());
        assertEquals("Updated description", edited.description());
        assertEquals(editedDetails.startsAt(), edited.startsAt());
        assertEquals(editedDetails.endsAt(), edited.endsAt());
        assertEquals(100, edited.capacity());
        assertEquals(1, edited.version());
        assertEquals(edited, repository.findById(EVENT_ID).orElseThrow());
        assertEquals(EventAuditRecord.Action.EDIT_EVENT, repository.auditRecords.getLast().action());
        assertEquals(1, repository.auditRecords.getLast().resultingVersion());
    }

    @Test
    void editEvent_unownedEvent_rejectedWithoutChangeOrAudit() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        OrganizerIdentity otherOrganizer = new OrganizerIdentity("organizer-2", Set.of("club-2"));
        int auditCountBeforeEdit = repository.auditRecords.size();

        assertThrows(
                AccessDeniedException.class,
                () -> service.editEvent(otherOrganizer, EVENT_ID, 0, VALID_DETAILS));

        assertEquals(0, repository.findById(EVENT_ID).orElseThrow().version());
        assertEquals(auditCountBeforeEdit, repository.auditRecords.size());
    }

    @Test
    void editEvent_staleVersion_rejectedWithoutChangeOrAudit() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        int auditCountBeforeEdit = repository.auditRecords.size();

        assertThrows(
                EventVersionConflictException.class,
                () -> service.editEvent(ORGANIZER, EVENT_ID, 9, VALID_DETAILS));

        assertEquals(0, repository.findById(EVENT_ID).orElseThrow().version());
        assertEquals(auditCountBeforeEdit, repository.auditRecords.size());
    }

    @Test
    void getAndListEvents_onlyExposeOwnedClubEvents() {
        Event owned = service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        Event unowned = new Event(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "club-2",
                "organizer-2",
                "Other event",
                "Other description",
                VALID_DETAILS.startsAt(),
                VALID_DETAILS.endsAt(),
                20,
                EventStatus.DRAFT,
                0);
        repository.events.put(unowned.id(), unowned);

        assertEquals(owned, service.getEvent(ORGANIZER, EVENT_ID));
        assertEquals(List.of(owned), service.listEvents(ORGANIZER));
        assertThrows(
                AccessDeniedException.class,
                () -> service.getEvent(ORGANIZER, unowned.id()));
        assertThrows(
                EntityNotFoundException.class,
                () -> service.getEvent(ORGANIZER, UUID.randomUUID()));
    }

    private static final class InMemoryEventRepository implements EventRepository {
        private final Map<UUID, Event> events = new HashMap<>();
        private final List<EventAuditRecord> auditRecords = new ArrayList<>();

        @Override
        public Optional<Event> findById(UUID eventId) {
            return Optional.ofNullable(events.get(eventId));
        }

        @Override
        public List<Event> findByClubIds(Set<String> clubIds) {
            return events.values().stream()
                    .filter(event -> clubIds.contains(event.clubId()))
                    .sorted(java.util.Comparator.comparing(Event::startsAt))
                    .toList();
        }

        @Override
        public void create(Event event, EventAuditRecord auditRecord) {
            events.put(event.id(), event);
            auditRecords.add(auditRecord);
        }

        @Override
        public void update(Event event, long expectedVersion, EventAuditRecord auditRecord) {
            Event current = events.get(event.id());
            if (current == null || current.version() != expectedVersion) {
                throw new EventVersionConflictException("Event was modified by another operation");
            }
            events.put(event.id(), event);
            auditRecords.add(auditRecord);
        }
    }
}
