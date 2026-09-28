package seedu.eventmanager.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
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
import seedu.eventmanager.service.VenueRequestRepository;
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;

class EventServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-22T10:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID REQUEST_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID VENUE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final OrganizerIdentity ORGANIZER =
            new OrganizerIdentity("organizer-1", Set.of("club-1"));
    private static final EventDetails VALID_DETAILS = new EventDetails(
            "Campus Night",
            "An evening event",
            Instant.parse("2026-10-01T10:00:00Z"),
            Instant.parse("2026-10-01T12:00:00Z"),
            80);

    private InMemoryEventRepository repository;
    private FakeVenueRequestRepository requests;
    private FakeBookingCheck bookings;
    private FakeDeletion deletion;
    private EventService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEventRepository();
        requests = new FakeVenueRequestRepository();
        bookings = new FakeBookingCheck();
        deletion = new FakeDeletion(repository);
        service = new EventService(
                repository, () -> EVENT_ID, Clock.fixed(NOW, ZoneOffset.UTC), requests, bookings, deletion);
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

        Event edited = service.editEvent(ORGANIZER, EVENT_ID, 0, editedDetails).event();

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
    void editEvent_capacityChange_withOpenSubmitted_syncsAttendance() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        OffsetDateTime start = OffsetDateTime.parse("2026-10-01T10:00:00Z");
        requests.save(new VenueRequest(
                REQUEST_ID,
                EVENT_ID,
                VENUE_ID,
                OrganizerIds.toUuid("organizer-1"),
                start,
                start.plusHours(2),
                80,
                VenueRequestStatus.SUBMITTED));

        CapacityUpdateResult result = service.editEvent(
                ORGANIZER,
                EVENT_ID,
                0,
                new EventDetails(
                        VALID_DETAILS.title(),
                        VALID_DETAILS.description(),
                        VALID_DETAILS.startsAt(),
                        VALID_DETAILS.endsAt(),
                        95));

        assertEquals(95, result.event().capacity());
        assertEquals(CapacityUpdateResult.SyncStatus.PENDING_REQUEST_SYNCED, result.syncStatus());
        assertEquals(95, requests.get(REQUEST_ID).expectedAttendance());
        assertEquals(VenueRequestStatus.SUBMITTED, requests.get(REQUEST_ID).status());
    }

    @Test
    void editEvent_capacityChange_whenApproved_doesNotChangeRequestAttendance() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        OffsetDateTime start = OffsetDateTime.parse("2026-10-01T10:00:00Z");
        requests.save(new VenueRequest(
                REQUEST_ID,
                EVENT_ID,
                VENUE_ID,
                OrganizerIds.toUuid("organizer-1"),
                start,
                start.plusHours(2),
                80,
                VenueRequestStatus.APPROVED));

        CapacityUpdateResult result = service.editEvent(
                ORGANIZER,
                EVENT_ID,
                0,
                new EventDetails(
                        VALID_DETAILS.title(),
                        VALID_DETAILS.description(),
                        VALID_DETAILS.startsAt(),
                        VALID_DETAILS.endsAt(),
                        50));

        assertEquals(50, result.event().capacity());
        assertEquals(CapacityUpdateResult.SyncStatus.DECIDED_REQUEST_UNCHANGED, result.syncStatus());
        assertEquals(80, requests.get(REQUEST_ID).expectedAttendance());
    }

    @Test
    void editEvent_capacityChange_withOpenDraft_syncsAttendance() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        OffsetDateTime start = OffsetDateTime.parse("2026-10-01T10:00:00Z");
        requests.save(new VenueRequest(
                REQUEST_ID,
                EVENT_ID,
                VENUE_ID,
                OrganizerIds.toUuid("organizer-1"),
                start,
                start.plusHours(2),
                80,
                VenueRequestStatus.DRAFT));

        CapacityUpdateResult result = service.editEvent(
                ORGANIZER,
                EVENT_ID,
                0,
                new EventDetails(
                        VALID_DETAILS.title(),
                        VALID_DETAILS.description(),
                        VALID_DETAILS.startsAt(),
                        VALID_DETAILS.endsAt(),
                        110));

        assertEquals(CapacityUpdateResult.SyncStatus.PENDING_REQUEST_SYNCED, result.syncStatus());
        assertEquals(110, requests.get(REQUEST_ID).expectedAttendance());
        assertEquals(VenueRequestStatus.DRAFT, requests.get(REQUEST_ID).status());
    }

    @Test
    void editEvent_capacityChange_whenRejected_doesNotChangeRequestAttendance() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        OffsetDateTime start = OffsetDateTime.parse("2026-10-01T10:00:00Z");
        requests.save(new VenueRequest(
                REQUEST_ID,
                EVENT_ID,
                VENUE_ID,
                OrganizerIds.toUuid("organizer-1"),
                start,
                start.plusHours(2),
                80,
                VenueRequestStatus.REJECTED));

        CapacityUpdateResult result = service.editEvent(
                ORGANIZER,
                EVENT_ID,
                0,
                new EventDetails(
                        VALID_DETAILS.title(),
                        VALID_DETAILS.description(),
                        VALID_DETAILS.startsAt(),
                        VALID_DETAILS.endsAt(),
                        40));

        assertEquals(40, result.event().capacity());
        assertEquals(CapacityUpdateResult.SyncStatus.DECIDED_REQUEST_UNCHANGED, result.syncStatus());
        assertEquals(80, requests.get(REQUEST_ID).expectedAttendance());
        assertTrue(requests.attendanceUpdates.isEmpty());
    }

    @Test
    void editEvent_capacityChange_withNoRequest_reportsNoOpenRequest() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);

        CapacityUpdateResult result = service.editEvent(
                ORGANIZER,
                EVENT_ID,
                0,
                new EventDetails(
                        VALID_DETAILS.title(),
                        VALID_DETAILS.description(),
                        VALID_DETAILS.startsAt(),
                        VALID_DETAILS.endsAt(),
                        60));

        assertEquals(60, result.event().capacity());
        assertEquals(CapacityUpdateResult.SyncStatus.NO_OPEN_REQUEST, result.syncStatus());
        assertTrue(requests.attendanceUpdates.isEmpty());
    }

    @Test
    void editEvent_staleVersion_withOpenRequest_leavesAttendanceUnchanged() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        OffsetDateTime start = OffsetDateTime.parse("2026-10-01T10:00:00Z");
        requests.save(new VenueRequest(
                REQUEST_ID,
                EVENT_ID,
                VENUE_ID,
                OrganizerIds.toUuid("organizer-1"),
                start,
                start.plusHours(2),
                80,
                VenueRequestStatus.SUBMITTED));

        assertThrows(
                EventVersionConflictException.class,
                () -> service.editEvent(
                        ORGANIZER,
                        EVENT_ID,
                        9,
                        new EventDetails(
                                VALID_DETAILS.title(),
                                VALID_DETAILS.description(),
                                VALID_DETAILS.startsAt(),
                                VALID_DETAILS.endsAt(),
                                95)));

        assertEquals(80, repository.findById(EVENT_ID).orElseThrow().capacity());
        assertEquals(80, requests.get(REQUEST_ID).expectedAttendance());
        assertTrue(requests.attendanceUpdates.isEmpty());
    }

    @Test
    void editEvent_capacityUnchanged_doesNotTouchVenueRequest() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        OffsetDateTime start = OffsetDateTime.parse("2026-10-01T10:00:00Z");
        requests.save(new VenueRequest(
                REQUEST_ID,
                EVENT_ID,
                VENUE_ID,
                OrganizerIds.toUuid("organizer-1"),
                start,
                start.plusHours(2),
                80,
                VenueRequestStatus.SUBMITTED));

        CapacityUpdateResult result = service.editEvent(
                ORGANIZER,
                EVENT_ID,
                0,
                new EventDetails(
                        "Renamed only",
                        VALID_DETAILS.description(),
                        VALID_DETAILS.startsAt(),
                        VALID_DETAILS.endsAt(),
                        80));

        assertEquals("Renamed only", result.event().title());
        assertEquals(CapacityUpdateResult.SyncStatus.NO_OPEN_REQUEST, result.syncStatus());
        assertEquals(80, requests.get(REQUEST_ID).expectedAttendance());
        assertTrue(requests.attendanceUpdates.isEmpty());
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
    void publishEvent_ownedDraftWithMatchingBooking_publishesWithAudit() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        bookings.confirm(EVENT_ID, VALID_DETAILS.startsAt(), VALID_DETAILS.endsAt());

        Event published = service.publishEvent(ORGANIZER, EVENT_ID, 0);

        assertEquals(EventStatus.PUBLISHED, published.status());
        assertEquals(1, published.version());
        assertEquals(VALID_DETAILS.title(), published.title());
        assertEquals(VALID_DETAILS.startsAt(), published.startsAt());
        assertEquals(published, repository.findById(EVENT_ID).orElseThrow());
        assertEquals(
                new EventAuditRecord(NOW, "organizer-1", EventAuditRecord.Action.PUBLISH_EVENT, EVENT_ID, 1),
                repository.auditRecords.getLast());
    }

    @Test
    void publishEvent_unownedEvent_rejectedWithoutChangeOrAudit() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        bookings.confirm(EVENT_ID, VALID_DETAILS.startsAt(), VALID_DETAILS.endsAt());
        OrganizerIdentity otherOrganizer = new OrganizerIdentity("organizer-2", Set.of("club-2"));

        assertThrows(AccessDeniedException.class, () -> service.publishEvent(otherOrganizer, EVENT_ID, 0));

        assertUnchangedDraft();
    }

    @Test
    void publishEvent_unknownEvent_notFound() {
        assertThrows(EntityNotFoundException.class, () -> service.publishEvent(ORGANIZER, EVENT_ID, 0));
        assertTrue(repository.auditRecords.isEmpty());
    }

    @Test
    void publishEvent_staleOrNegativeVersion_rejectedWithoutChangeOrAudit() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        bookings.confirm(EVENT_ID, VALID_DETAILS.startsAt(), VALID_DETAILS.endsAt());

        assertThrows(EventVersionConflictException.class, () -> service.publishEvent(ORGANIZER, EVENT_ID, 3));
        assertThrows(ValidationException.class, () -> service.publishEvent(ORGANIZER, EVENT_ID, -1));

        assertUnchangedDraft();
    }

    @Test
    void publishEvent_alreadyPublished_rejectedWithoutSecondAudit() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        bookings.confirm(EVENT_ID, VALID_DETAILS.startsAt(), VALID_DETAILS.endsAt());
        service.publishEvent(ORGANIZER, EVENT_ID, 0);
        int auditCount = repository.auditRecords.size();

        assertThrows(ValidationException.class, () -> service.publishEvent(ORGANIZER, EVENT_ID, 1));

        assertEquals(1, repository.findById(EVENT_ID).orElseThrow().version());
        assertEquals(auditCount, repository.auditRecords.size());
    }

    @Test
    void publishEvent_atOrAfterStart_rejected() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        bookings.confirm(EVENT_ID, VALID_DETAILS.startsAt(), VALID_DETAILS.endsAt());

        for (Instant now : List.of(VALID_DETAILS.startsAt(), VALID_DETAILS.startsAt().plusSeconds(60))) {
            EventService late = new EventService(
                    repository, () -> EVENT_ID, Clock.fixed(now, ZoneOffset.UTC), requests, bookings);
            assertThrows(ValidationException.class, () -> late.publishEvent(ORGANIZER, EVENT_ID, 0));
        }

        assertUnchangedDraft();
    }

    @Test
    void publishEvent_withoutMatchingConfirmedBooking_rejected() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);

        ValidationException missing = assertThrows(
                ValidationException.class, () -> service.publishEvent(ORGANIZER, EVENT_ID, 0));
        assertTrue(missing.getMessage().contains("confirmed venue booking"));

        bookings.confirm(EVENT_ID, VALID_DETAILS.startsAt(), VALID_DETAILS.endsAt().plusSeconds(1800));
        assertThrows(ValidationException.class, () -> service.publishEvent(ORGANIZER, EVENT_ID, 0));

        assertUnchangedDraft();
    }

    @Test
    void publishEvent_withoutBookingCheckConfigured_failsClosed() {
        EventService unchecked = new EventService(
                repository, () -> EVENT_ID, Clock.fixed(NOW, ZoneOffset.UTC), requests);
        unchecked.createEvent(ORGANIZER, "club-1", VALID_DETAILS);

        assertThrows(IllegalStateException.class, () -> unchecked.publishEvent(ORGANIZER, EVENT_ID, 0));

        assertUnchangedDraft();
    }

    @Test
    void editEvent_publishedEvent_rejected() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        bookings.confirm(EVENT_ID, VALID_DETAILS.startsAt(), VALID_DETAILS.endsAt());
        service.publishEvent(ORGANIZER, EVENT_ID, 0);

        assertThrows(ValidationException.class, () -> service.editEvent(ORGANIZER, EVENT_ID, 1, VALID_DETAILS));

        assertEquals(EventStatus.PUBLISHED, repository.findById(EVENT_ID).orElseThrow().status());
    }

    @Test
    void editEvent_afterVenueApproved_changingTimes_rejectedWithBookedTimes() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        bookings.confirm(EVENT_ID, VALID_DETAILS.startsAt(), VALID_DETAILS.endsAt());
        EventDetails moved = new EventDetails(VALID_DETAILS.title(), VALID_DETAILS.description(),
                VALID_DETAILS.startsAt().plusSeconds(86_400), VALID_DETAILS.endsAt().plusSeconds(86_400), 80);

        ValidationException rejected = assertThrows(
                ValidationException.class, () -> service.editEvent(ORGANIZER, EVENT_ID, 0, moved));

        assertTrue(rejected.getMessage().contains("1 Oct 2026, 6:00 PM SGT"), rejected.getMessage());
        assertTrue(rejected.getMessage().contains("8:00 PM SGT"), rejected.getMessage());
        assertUnchangedDraft();
    }

    @Test
    void editEvent_afterVenueApproved_otherFieldsStillEditable() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        bookings.confirm(EVENT_ID, VALID_DETAILS.startsAt(), VALID_DETAILS.endsAt());

        Event edited = service.editEvent(ORGANIZER, EVENT_ID, 0, new EventDetails(
                "Renamed", "New description", VALID_DETAILS.startsAt(), VALID_DETAILS.endsAt(), 60)).event();

        assertEquals("Renamed", edited.title());
        assertEquals(60, edited.capacity());
        assertEquals(1, edited.version());
    }

    @Test
    void editEvent_timesAlreadyDriftedFromBooking_restoringBookedTimesAllowsPublish() {
        Instant bookedStart = VALID_DETAILS.startsAt().minusSeconds(86_400);
        Instant bookedEnd = VALID_DETAILS.endsAt();
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        bookings.confirm(EVENT_ID, bookedStart, bookedEnd);

        Event restored = service.editEvent(ORGANIZER, EVENT_ID, 0, new EventDetails(
                VALID_DETAILS.title(), VALID_DETAILS.description(), bookedStart, bookedEnd, 80)).event();
        Event published = service.publishEvent(ORGANIZER, EVENT_ID, restored.version());

        assertEquals(bookedStart, published.startsAt());
        assertEquals(EventStatus.PUBLISHED, published.status());
    }

    @Test
    void publishEvent_bookingTimesDiffer_messageNamesBookedTimes() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        bookings.confirm(EVENT_ID, VALID_DETAILS.startsAt().minusSeconds(86_400), VALID_DETAILS.endsAt());

        ValidationException rejected = assertThrows(
                ValidationException.class, () -> service.publishEvent(ORGANIZER, EVENT_ID, 0));

        assertTrue(rejected.getMessage().contains("30 Sep 2026, 6:00 PM SGT"), rejected.getMessage());
        assertUnchangedDraft();
    }

    @Test
    void publishEvent_bookedVenueInactive_explainsVenue() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        bookings.confirmAtInactiveVenue(EVENT_ID, VALID_DETAILS.startsAt(), VALID_DETAILS.endsAt());

        ValidationException rejected = assertThrows(
                ValidationException.class, () -> service.publishEvent(ORGANIZER, EVENT_ID, 0));

        assertTrue(rejected.getMessage().contains("not active"), rejected.getMessage());
        assertUnchangedDraft();
    }

    @Test
    void deleteEvent_ownedDraft_softDeletesWithAuditAndHidesEverywhere() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);

        Event deleted = service.deleteEvent(ORGANIZER, EVENT_ID, 0);

        assertEquals(EventStatus.DELETED, deleted.status());
        assertEquals(1, deleted.version());
        assertEquals(1, deletion.calls.size());
        FakeDeletion.Call call = deletion.calls.getFirst();
        assertEquals(deleted, call.deleted());
        assertEquals(0, call.expectedVersion());
        assertEquals(OrganizerIds.toUuid("organizer-1"), call.organizerId());
        assertEquals(new EventAuditRecord(NOW, "organizer-1", EventAuditRecord.Action.DELETE_EVENT, EVENT_ID, 1),
                call.audit());
        assertTrue(service.listEvents(ORGANIZER).isEmpty());
        assertThrows(EntityNotFoundException.class, () -> service.getEvent(ORGANIZER, EVENT_ID));
        assertThrows(EntityNotFoundException.class,
                () -> service.editEvent(ORGANIZER, EVENT_ID, 1, VALID_DETAILS));
        assertThrows(EntityNotFoundException.class, () -> service.publishEvent(ORGANIZER, EVENT_ID, 1));
        assertThrows(EntityNotFoundException.class, () -> service.deleteEvent(ORGANIZER, EVENT_ID, 1));
        assertEquals(1, deletion.calls.size());
    }

    @Test
    void deleteEvent_publishedEvent_rejectedWithoutDeletion() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        bookings.confirm(EVENT_ID, VALID_DETAILS.startsAt(), VALID_DETAILS.endsAt());
        service.publishEvent(ORGANIZER, EVENT_ID, 0);

        ValidationException rejected = assertThrows(
                ValidationException.class, () -> service.deleteEvent(ORGANIZER, EVENT_ID, 1));

        assertTrue(rejected.getMessage().contains("Only draft events can be deleted"), rejected.getMessage());
        assertTrue(deletion.calls.isEmpty());
        assertEquals(EventStatus.PUBLISHED, service.getEvent(ORGANIZER, EVENT_ID).status());
    }

    @Test
    void deleteEvent_unownedEvent_rejectedWithoutDeletion() {
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);
        OrganizerIdentity otherOrganizer = new OrganizerIdentity("organizer-2", Set.of("club-2"));

        assertThrows(AccessDeniedException.class, () -> service.deleteEvent(otherOrganizer, EVENT_ID, 0));

        assertTrue(deletion.calls.isEmpty());
        assertUnchangedDraft();
    }

    @Test
    void deleteEvent_staleNegativeOrUnknown_rejectedWithoutDeletion() {
        assertThrows(EntityNotFoundException.class, () -> service.deleteEvent(ORGANIZER, EVENT_ID, 0));
        service.createEvent(ORGANIZER, "club-1", VALID_DETAILS);

        assertThrows(EventVersionConflictException.class, () -> service.deleteEvent(ORGANIZER, EVENT_ID, 4));
        assertThrows(ValidationException.class, () -> service.deleteEvent(ORGANIZER, EVENT_ID, -1));

        assertTrue(deletion.calls.isEmpty());
        assertUnchangedDraft();
    }

    @Test
    void deleteEvent_withoutDeletionConfigured_failsClosed() {
        EventService noDeletion = new EventService(
                repository, () -> EVENT_ID, Clock.fixed(NOW, ZoneOffset.UTC), requests, bookings);
        noDeletion.createEvent(ORGANIZER, "club-1", VALID_DETAILS);

        assertThrows(IllegalStateException.class, () -> noDeletion.deleteEvent(ORGANIZER, EVENT_ID, 0));

        assertUnchangedDraft();
    }

    private void assertUnchangedDraft() {
        Event stored = repository.findById(EVENT_ID).orElseThrow();
        assertEquals(EventStatus.DRAFT, stored.status());
        assertEquals(0, stored.version());
        assertEquals(1, repository.auditRecords.size());
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

    private static final class FakeDeletion implements DraftEventDeletion {
        record Call(Event deleted, long expectedVersion, EventAuditRecord audit, UUID organizerId) { }

        private final InMemoryEventRepository repository;
        private final List<Call> calls = new ArrayList<>();

        FakeDeletion(InMemoryEventRepository repository) {
            this.repository = repository;
        }

        @Override
        public void deleteDraft(Event deleted, long expectedVersion, EventAuditRecord audit, UUID organizerId) {
            calls.add(new Call(deleted, expectedVersion, audit, organizerId));
            repository.update(deleted, expectedVersion, audit);
        }
    }

    private static final class FakeBookingCheck implements EventBookingCheck {
        private final Map<UUID, ActiveBooking> bookings = new HashMap<>();

        void confirm(UUID eventId, Instant startsAt, Instant endsAt) {
            bookings.put(eventId, new ActiveBooking(startsAt, endsAt, true));
        }

        void confirmAtInactiveVenue(UUID eventId, Instant startsAt, Instant endsAt) {
            bookings.put(eventId, new ActiveBooking(startsAt, endsAt, false));
        }

        @Override
        public boolean hasConfirmedActiveBooking(UUID eventId, Instant startsAt, Instant endsAt) {
            ActiveBooking booking = bookings.get(eventId);
            return booking != null && booking.venueActive()
                    && booking.startsAt().equals(startsAt) && booking.endsAt().equals(endsAt);
        }

        @Override
        public Optional<ActiveBooking> findActiveBooking(UUID eventId) {
            return Optional.ofNullable(bookings.get(eventId));
        }
    }

    private static final class FakeVenueRequestRepository implements VenueRequestRepository {
        private final List<VenueRequest> saved = new ArrayList<>();
        private final Map<UUID, VenueRequest> byId = new HashMap<>();
        private final List<Integer> attendanceUpdates = new ArrayList<>();

        @Override
        public VenueRequest get(UUID requestId) {
            return byId.get(requestId);
        }

        @Override
        public void save(VenueRequest request) {
            saved.add(request);
            byId.put(request.requestId(), request);
        }

        @Override
        public Optional<VenueRequest> findOpenByEventId(UUID eventId) {
            return saved.stream()
                    .filter(r -> r.eventId().equals(eventId))
                    .filter(r -> r.status() == VenueRequestStatus.SUBMITTED
                            || r.status() == VenueRequestStatus.DRAFT)
                    .findFirst();
        }

        @Override
        public Optional<VenueRequest> findLatestByEventId(UUID eventId) {
            for (int i = saved.size() - 1; i >= 0; i--) {
                if (saved.get(i).eventId().equals(eventId)) {
                    return Optional.of(saved.get(i));
                }
            }
            return Optional.empty();
        }

        @Override
        public boolean updateExpectedAttendance(UUID requestId, int expectedAttendance) {
            VenueRequest current = byId.get(requestId);
            if (current == null) {
                return false;
            }
            if (current.status() != VenueRequestStatus.SUBMITTED
                    && current.status() != VenueRequestStatus.DRAFT) {
                return false;
            }
            attendanceUpdates.add(expectedAttendance);
            VenueRequest updated = new VenueRequest(
                    current.requestId(),
                    current.eventId(),
                    current.venueId(),
                    current.organizerId(),
                    current.startsAt(),
                    current.endsAt(),
                    expectedAttendance,
                    current.status());
            byId.put(requestId, updated);
            saved.add(updated);
            return true;
        }
    }
}
