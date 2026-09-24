package seedu.eventmanager.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.event.CapacityUpdateResult;
import seedu.eventmanager.event.Event;
import seedu.eventmanager.event.EventAuditRecord;
import seedu.eventmanager.event.EventDetails;
import seedu.eventmanager.event.EventRepository;
import seedu.eventmanager.event.EventService;
import seedu.eventmanager.event.OrganizerIdentity;
import seedu.eventmanager.event.OrganizerVenueRequestService;
import seedu.eventmanager.service.AuditLogService;
import seedu.eventmanager.service.AuthorizationService;
import seedu.eventmanager.service.NotificationService;
import seedu.eventmanager.service.TransactionManager;
import seedu.eventmanager.service.VenueAdministratorService;
import seedu.eventmanager.service.VenueBookingRepository;
import seedu.eventmanager.service.VenueRepository;
import seedu.eventmanager.service.VenueRequestRepository;
import seedu.eventmanager.ui.JdbcVenueAdministratorApiClient;
import seedu.eventmanager.ui.VenueAdministratorDashboardController;
import seedu.eventmanager.ui.VenueAdministratorDashboardState;
import seedu.eventmanager.venue.Venue;
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;
import seedu.eventmanager.venue.VenueStatus;

/**
 * Dual-role smoke without JavaFX: Organizer submit → Admin pending list → approve.
 */
class OrganizerToAdminVenuePipelineE2ETest {
    private static final Instant NOW = Instant.parse("2026-09-24T05:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID VENUE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
    private static final UUID REQUEST_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-ccccccccccc1");
    private static final UUID ADMIN_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-ddddddddddd1");

    @Test
    void organizerSubmitAppearsForAdminAndCanBeApproved() {
        OrganizerIdentity organizer = new OrganizerIdentity("demo-organizer", Set.of("club-1"));
        FakeEvents events = new FakeEvents();
        FakeVenues venues = new FakeVenues();
        FakeRequests requests = new FakeRequests();
        EventService eventService = new EventService(
                events, () -> EVENT_ID, Clock.fixed(NOW, ZoneOffset.UTC), requests);
        eventService.createEvent(
                organizer,
                "club-1",
                new EventDetails(
                        "Campus Night",
                        "Demo",
                        Instant.parse("2026-10-01T10:00:00Z"),
                        Instant.parse("2026-10-01T12:00:00Z"),
                        80));
        venues.save(new Venue(VENUE_ID, "LT1", "COM1", 120, "", VenueStatus.ACTIVE));

        OrganizerVenueRequestService submit = new OrganizerVenueRequestService(
                eventService, venues, requests, () -> REQUEST_ID);
        VenueRequest submitted = submit.submit(organizer, EVENT_ID, VENUE_ID);
        assertEquals(VenueRequestStatus.SUBMITTED, submitted.status());
        assertEquals(80, submitted.expectedAttendance());

        CapacityUpdateResult capacity = eventService.editEvent(
                organizer,
                EVENT_ID,
                0,
                new EventDetails(
                        "Campus Night",
                        "Demo",
                        Instant.parse("2026-10-01T10:00:00Z"),
                        Instant.parse("2026-10-01T12:00:00Z"),
                        95));
        assertEquals(CapacityUpdateResult.SyncStatus.PENDING_REQUEST_SYNCED, capacity.syncStatus());
        assertEquals(95, requests.get(REQUEST_ID).expectedAttendance());

        requests.grant(ADMIN_ID, VENUE_ID);
        List<VenueAdministratorDashboardState> states = new ArrayList<>();
        Actor admin = new Actor(ADMIN_ID, Role.VENUE_ADMINISTRATOR);
        VenueAdministratorDashboardController dashboard = new VenueAdministratorDashboardController(
                admin,
                requests,
                new JdbcVenueAdministratorApiClient(requests.workflow, requests, admin),
                states::add);

        dashboard.load();
        assertEquals(1, dashboard.state().data().pendingRequests().size());
        assertEquals(REQUEST_ID, dashboard.state().data().pendingRequests().getFirst().requestId());
        assertEquals(95, dashboard.state().data().pendingRequests().getFirst().expectedAttendance());

        dashboard.approve(REQUEST_ID);
        assertEquals(VenueRequestStatus.APPROVED, requests.get(REQUEST_ID).status());
        assertTrue(dashboard.state().data().pendingRequests().isEmpty());
        assertTrue(states.stream().anyMatch(state ->
                state.status() == VenueAdministratorDashboardState.Status.READY
                        || state.status() == VenueAdministratorDashboardState.Status.EMPTY));
    }

    private static final class FakeEvents implements EventRepository {
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

    private static final class FakeVenues implements VenueRepository {
        private final Map<UUID, Venue> byId = new HashMap<>();

        @Override
        public List<Venue> findAll() {
            return List.copyOf(byId.values());
        }

        @Override
        public Venue findById(UUID venueId) {
            return byId.get(venueId);
        }

        @Override
        public void save(Venue venue) {
            byId.put(venue.venueId(), venue);
        }
    }

    private static final class FakeRequests implements VenueRequestRepository, VenueBookingRepository,
            AuthorizationService, NotificationService, AuditLogService, TransactionManager {
        private final Map<UUID, VenueRequest> byId = new HashMap<>();
        private final Map<UUID, UUID> grants = new HashMap<>(); // venueId -> adminId
        private final VenueAdministratorService workflow = new VenueAdministratorService(
                this, this, this, this, this, this);

        void grant(UUID adminId, UUID venueId) {
            grants.put(venueId, adminId);
        }

        @Override
        public VenueRequest get(UUID requestId) {
            return byId.get(requestId);
        }

        @Override
        public void save(VenueRequest request) {
            byId.put(request.requestId(), request);
        }

        @Override
        public void save(VenueRequest request, UUID decidedBy, String decisionReason) {
            byId.put(request.requestId(), request);
        }

        @Override
        public List<VenueRequest> findSubmitted() {
            return byId.values().stream()
                    .filter(request -> request.status() == VenueRequestStatus.SUBMITTED)
                    .toList();
        }

        @Override
        public Optional<VenueRequest> findOpenByEventId(UUID eventId) {
            return byId.values().stream()
                    .filter(request -> request.eventId().equals(eventId))
                    .filter(request -> request.status() == VenueRequestStatus.SUBMITTED
                            || request.status() == VenueRequestStatus.DRAFT)
                    .findFirst();
        }

        @Override
        public Optional<VenueRequest> findLatestByEventId(UUID eventId) {
            return byId.values().stream()
                    .filter(request -> request.eventId().equals(eventId))
                    .reduce((first, second) -> second);
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
            return true;
        }

        @Override
        public boolean hasConflict(UUID venueId, OffsetDateTime startsAt, OffsetDateTime endsAt) {
            return false;
        }

        @Override
        public void createFromApprovedRequest(VenueRequest request, UUID approverId) { }

        @Override
        public void requireVenueRequestAccess(Actor actor, VenueRequest request) {
            requireRole(actor, Role.VENUE_ADMINISTRATOR);
            UUID granted = grants.get(request.venueId());
            if (granted == null || !granted.equals(actor.userId())) {
                throw new seedu.eventmanager.common.ApplicationException(
                        "FORBIDDEN", "The administrator has no access to this venue.");
            }
        }

        @Override
        public void notify(UUID recipientId, String event, Map<String, String> data) { }

        @Override
        public void record(Actor actor, String action, String entityType, UUID entityId,
                String previousState, String newState, String reason) { }

        @Override
        public <T> T execute(Supplier<T> work) {
            return work.get();
        }
    }
}
