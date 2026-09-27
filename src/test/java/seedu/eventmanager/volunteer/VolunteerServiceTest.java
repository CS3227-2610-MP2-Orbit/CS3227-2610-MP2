package seedu.eventmanager.volunteer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import seedu.eventmanager.registration.NoEventRegistrations;
import seedu.eventmanager.registration.RegisteredAttendee;

class VolunteerServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-25T04:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000a11c");
    private static final UUID BEN = UUID.fromString("00000000-0000-0000-0000-000000000be1");
    private static final UUID STRANGER = UUID.fromString("00000000-0000-0000-0000-0000000005a1");
    private static final OrganizerIdentity ORGANIZER =
            new OrganizerIdentity("organizer-1", Set.of("club-1"));
    private static final OrganizerIdentity OTHER_ORGANIZER =
            new OrganizerIdentity("organizer-2", Set.of("club-2"));

    private FakeRegistrations registrations;
    private InMemoryVolunteerRepository volunteers;
    private EventService eventService;
    private VolunteerService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        eventService = new EventService(new InMemoryEventRepository(), () -> EVENT_ID, clock);
        eventService.createEvent(ORGANIZER, "club-1", new EventDetails(
                "Campus Night",
                "Demo",
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T12:00:00Z"),
                80));
        registrations = new FakeRegistrations();
        registrations.register(EVENT_ID, ALICE, "Alice");
        registrations.register(EVENT_ID, BEN, "Ben");
        volunteers = new InMemoryVolunteerRepository();
        service = new VolunteerService(eventService, registrations, volunteers, clock);
    }

    @Test
    void assign_registeredAttendee_persistsAssignmentWithAudit() {
        AssignedVolunteer assigned = service.assign(ORGANIZER, EVENT_ID, ALICE, "Registration desk");

        assertEquals(ALICE, assigned.attendeeId());
        assertEquals(Optional.of("Alice"), assigned.displayName());
        assertEquals("Registration desk", assigned.role());
        assertEquals(NOW, assigned.assignedAt());
        assertEquals(
                List.of(new VolunteerAssignment(EVENT_ID, ALICE, "Registration desk", "organizer-1", NOW)),
                volunteers.findByEventId(EVENT_ID));
        assertEquals(
                List.of(new VolunteerAuditRecord(
                        NOW, "organizer-1", VolunteerAuditRecord.Action.ASSIGN_VOLUNTEER, EVENT_ID, ALICE)),
                volunteers.auditRecords);
    }

    @Test
    void assign_roleIsOptionalAndTrimmed() {
        assertEquals("", service.assign(ORGANIZER, EVENT_ID, ALICE, null).role());
        assertEquals("Usher", service.assign(ORGANIZER, EVENT_ID, BEN, "  Usher  ").role());
    }

    @Test
    void assign_blankRole_storedAsNoRole() {
        assertEquals("", service.assign(ORGANIZER, EVENT_ID, ALICE, "   ").role());
    }

    @Test
    void assign_roleAtMaximumLength_accepted() {
        String role = "r".repeat(VolunteerService.MAX_ROLE_LENGTH);

        assertEquals(role, service.assign(ORGANIZER, EVENT_ID, ALICE, role).role());
    }

    @Test
    void assign_roleTooLong_rejectedWithoutChanges() {
        String role = "r".repeat(VolunteerService.MAX_ROLE_LENGTH + 1);

        assertThrows(ValidationException.class, () -> service.assign(ORGANIZER, EVENT_ID, ALICE, role));

        assertTrue(volunteers.findByEventId(EVENT_ID).isEmpty());
        assertTrue(volunteers.auditRecords.isEmpty());
    }

    @Test
    void assign_attendeeNotRegistered_rejectedWithoutChanges() {
        assertThrows(ValidationException.class, () -> service.assign(ORGANIZER, EVENT_ID, STRANGER, "Usher"));

        assertTrue(volunteers.findByEventId(EVENT_ID).isEmpty());
        assertTrue(volunteers.auditRecords.isEmpty());
    }

    @Test
    void assign_withNoRegistrationsAvailable_rejected() {
        VolunteerService withoutRegistrations = new VolunteerService(
                eventService, new NoEventRegistrations(), volunteers, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(ValidationException.class,
                () -> withoutRegistrations.assign(ORGANIZER, EVENT_ID, ALICE, "Usher"));
        assertTrue(volunteers.findByEventId(EVENT_ID).isEmpty());
    }

    @Test
    void assign_duplicate_rejectedWithoutSecondAssignmentOrAudit() {
        service.assign(ORGANIZER, EVENT_ID, ALICE, "Usher");

        assertThrows(ValidationException.class, () -> service.assign(ORGANIZER, EVENT_ID, ALICE, "Desk"));

        assertEquals(1, volunteers.findByEventId(EVENT_ID).size());
        assertEquals("Usher", volunteers.findByEventId(EVENT_ID).getFirst().role());
        assertEquals(1, volunteers.auditRecords.size());
    }

    @Test
    void assign_unownedEvent_rejectedWithoutChanges() {
        assertThrows(AccessDeniedException.class,
                () -> service.assign(OTHER_ORGANIZER, EVENT_ID, ALICE, "Usher"));

        assertTrue(volunteers.findByEventId(EVENT_ID).isEmpty());
        assertTrue(volunteers.auditRecords.isEmpty());
    }

    @Test
    void assign_unknownEvent_rejected() {
        assertThrows(EntityNotFoundException.class,
                () -> service.assign(ORGANIZER, UUID.randomUUID(), ALICE, "Usher"));
        assertTrue(volunteers.auditRecords.isEmpty());
    }

    @Test
    void remove_assignedVolunteer_deletesWithAudit() {
        service.assign(ORGANIZER, EVENT_ID, ALICE, "Usher");

        service.remove(ORGANIZER, EVENT_ID, ALICE);

        assertTrue(volunteers.findByEventId(EVENT_ID).isEmpty());
        assertEquals(
                new VolunteerAuditRecord(
                        NOW, "organizer-1", VolunteerAuditRecord.Action.REMOVE_VOLUNTEER, EVENT_ID, ALICE),
                volunteers.auditRecords.getLast());
    }

    @Test
    void remove_notAssigned_rejectedWithoutAudit() {
        assertThrows(EntityNotFoundException.class, () -> service.remove(ORGANIZER, EVENT_ID, ALICE));
        assertTrue(volunteers.auditRecords.isEmpty());
    }

    @Test
    void remove_unownedEvent_rejectedAndAssignmentKept() {
        service.assign(ORGANIZER, EVENT_ID, ALICE, "Usher");

        assertThrows(AccessDeniedException.class, () -> service.remove(OTHER_ORGANIZER, EVENT_ID, ALICE));

        assertEquals(1, volunteers.findByEventId(EVENT_ID).size());
        assertEquals(1, volunteers.auditRecords.size());
    }

    @Test
    void listVolunteers_showsNamesAndFlagsAttendeesNoLongerRegistered() {
        service.assign(ORGANIZER, EVENT_ID, ALICE, "Usher");
        service.assign(ORGANIZER, EVENT_ID, BEN, "");
        registrations.unregister(EVENT_ID, BEN);

        List<AssignedVolunteer> listed = service.listVolunteers(ORGANIZER, EVENT_ID);

        assertEquals(2, listed.size());
        AssignedVolunteer alice = listed.stream().filter(v -> v.attendeeId().equals(ALICE)).findFirst().orElseThrow();
        AssignedVolunteer ben = listed.stream().filter(v -> v.attendeeId().equals(BEN)).findFirst().orElseThrow();
        assertEquals(Optional.of("Alice"), alice.displayName());
        assertTrue(alice.currentlyRegistered());
        assertEquals(Optional.empty(), ben.displayName());
        assertFalse(ben.currentlyRegistered());
    }

    @Test
    void listVolunteers_unownedEvent_rejected() {
        assertThrows(AccessDeniedException.class, () -> service.listVolunteers(OTHER_ORGANIZER, EVENT_ID));
    }

    @Test
    void availableAttendees_excludesAlreadyAssigned() {
        service.assign(ORGANIZER, EVENT_ID, ALICE, "Usher");

        assertEquals(
                List.of(new RegisteredAttendee(BEN, "Ben")),
                service.availableAttendees(ORGANIZER, EVENT_ID));
    }

    @Test
    void availableAttendees_unownedEvent_rejected() {
        assertThrows(AccessDeniedException.class, () -> service.availableAttendees(OTHER_ORGANIZER, EVENT_ID));
    }

    private static final class FakeRegistrations implements EventRegistrations {
        private final Map<UUID, Map<UUID, RegisteredAttendee>> byEvent = new HashMap<>();

        void register(UUID eventId, UUID attendeeId, String name) {
            byEvent.computeIfAbsent(eventId, ignored -> new LinkedHashMap<>())
                    .put(attendeeId, new RegisteredAttendee(attendeeId, name));
        }

        void unregister(UUID eventId, UUID attendeeId) {
            byEvent.getOrDefault(eventId, Map.of()).remove(attendeeId);
        }

        @Override
        public List<RegisteredAttendee> registeredAttendees(UUID eventId) {
            return List.copyOf(byEvent.getOrDefault(eventId, Map.of()).values());
        }
    }

    private static final class InMemoryVolunteerRepository implements VolunteerRepository {
        private final List<VolunteerAssignment> assignments = new ArrayList<>();
        private final List<VolunteerAuditRecord> auditRecords = new ArrayList<>();

        @Override
        public List<VolunteerAssignment> findByEventId(UUID eventId) {
            return assignments.stream().filter(a -> a.eventId().equals(eventId)).toList();
        }

        @Override
        public boolean exists(UUID eventId, UUID attendeeId) {
            return assignments.stream()
                    .anyMatch(a -> a.eventId().equals(eventId) && a.attendeeId().equals(attendeeId));
        }

        @Override
        public void add(VolunteerAssignment assignment, VolunteerAuditRecord auditRecord) {
            if (exists(assignment.eventId(), assignment.attendeeId())) {
                throw new ValidationException("duplicate");
            }
            assignments.add(assignment);
            auditRecords.add(auditRecord);
        }

        @Override
        public boolean remove(UUID eventId, UUID attendeeId, VolunteerAuditRecord auditRecord) {
            boolean removed = assignments.removeIf(
                    a -> a.eventId().equals(eventId) && a.attendeeId().equals(attendeeId));
            if (removed) {
                auditRecords.add(auditRecord);
            }
            return removed;
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
