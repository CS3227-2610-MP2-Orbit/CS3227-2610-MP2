package seedu.eventmanager.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.service.AuditLogService;
import seedu.eventmanager.service.AuthorizationService;
import seedu.eventmanager.service.NotificationService;
import seedu.eventmanager.service.TransactionManager;
import seedu.eventmanager.service.VenueAdministratorService;
import seedu.eventmanager.service.VenueBookingRepository;
import seedu.eventmanager.service.VenueRequestRepository;
import seedu.eventmanager.ui.VenueAdministratorApiClient;
import seedu.eventmanager.ui.VenueAdministratorDashboardController;
import seedu.eventmanager.ui.VenueAdministratorDashboardState;
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;

/** Deterministic in-process E2E tests for the current UI/service architecture. */
class VenueAdministratorWorkflowE2ETest {
    private final UUID requestId = UUID.randomUUID();
    private final UUID organizerId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    @Test
    void administratorCanReviewApproveAndObserveSideEffects() {
        Scenario scenario = new Scenario();
        List<VenueAdministratorDashboardState> states = new ArrayList<>();
        VenueAdministratorDashboardController dashboard = scenario.dashboard(states::add,
                new Actor(adminId, Role.VENUE_ADMINISTRATOR));

        dashboard.load();

        assertEquals(VenueAdministratorDashboardState.Status.READY, dashboard.state().status());
        assertEquals(requestId, dashboard.state().data().pendingRequests().get(0).requestId());
        VenueRequest request = dashboard.state().data().pendingRequests().get(0);
        assertEquals(scenario.venueId, request.venueId());
        assertEquals(scenario.eventId, request.eventId());

        dashboard.approve(request.requestId());

        assertEquals(VenueRequestStatus.APPROVED, scenario.requests.get(requestId).status());
        assertEquals(VenueAdministratorDashboardState.Status.EMPTY, dashboard.state().status());
        assertEquals("VENUE_REQUEST_APPROVED", scenario.requests.notificationEvent);
        assertEquals("VENUE_REQUEST_APPROVED", scenario.requests.auditEvent);
        assertTrue(states.stream().anyMatch(state -> state.status() == VenueAdministratorDashboardState.Status.LOADING));
    }

    @Test
    void administratorCanRejectAndUiRefreshesStatus() {
        Scenario scenario = new Scenario();
        List<VenueAdministratorDashboardState> states = new ArrayList<>();
        VenueAdministratorDashboardController dashboard = scenario.dashboard(states::add,
                new Actor(adminId, Role.VENUE_ADMINISTRATOR));

        dashboard.load();
        dashboard.reject(requestId, "Capacity requirements cannot be met.");

        assertEquals(VenueRequestStatus.REJECTED, scenario.requests.get(requestId).status());
        assertEquals("VENUE_REQUEST_REJECTED", scenario.requests.notificationEvent);
        assertEquals("VENUE_REQUEST_REJECTED", scenario.requests.auditEvent);
        assertEquals("Capacity requirements cannot be met.", scenario.requests.auditReason);
        assertEquals(VenueAdministratorDashboardState.Status.EMPTY, dashboard.state().status());
    }

    @Test
    void unauthorizedRoleCannotOpenDashboardOrPerformAction() {
        Scenario scenario = new Scenario();

        assertThrows(ApplicationException.class, () -> scenario.dashboard(ignored -> { },
                new Actor(UUID.randomUUID(), Role.CLUB_ORGANIZER)));
        assertEquals(VenueRequestStatus.SUBMITTED, scenario.requests.get(requestId).status());
        assertEquals(null, scenario.requests.notificationEvent);
        assertEquals(null, scenario.requests.auditEvent);
    }

    private final class Scenario {
        private final UUID eventId = UUID.randomUUID();
        private final UUID venueId = UUID.randomUUID();
        private final Store requests = new Store();
        private Scenario() {
            OffsetDateTime start = OffsetDateTime.parse("2030-01-01T10:00:00Z");
            requests.getMap().put(requestId, new VenueRequest(requestId, eventId, venueId, organizerId, start,
                    start.plusHours(1), 50, VenueRequestStatus.SUBMITTED));
        }

        private VenueAdministratorDashboardController dashboard(
                Consumer<VenueAdministratorDashboardState> listener, Actor actor) {
            VenueAdministratorApiClient client = new Client(requests, this, actor);
            return new VenueAdministratorDashboardController(actor, requests, client, listener);
        }
    }

    private static final class Client implements VenueAdministratorApiClient {
        private final Store store;
        private final Scenario scenario;
        private final Actor actor;

        private Client(Store store, Scenario scenario, Actor actor) {
            this.store = store;
            this.scenario = scenario;
            this.actor = actor;
        }

        @Override public VenueAdministratorDashboardData loadDashboard() {
            List<VenueRequest> pending = store.getMap().values().stream()
                    .filter(request -> request.status() == VenueRequestStatus.SUBMITTED).toList();
            return new VenueAdministratorDashboardData(pending, List.of(), List.of(), List.of(), List.of());
        }

        @Override public VenueRequest approveRequest(UUID requestId) {
            return store.workflow.approve(actor, requestId);
        }

        @Override public VenueRequest rejectRequest(UUID requestId, String reason) {
            return store.workflow.reject(actor, requestId, reason);
        }
    }

    private static final class Store implements VenueRequestRepository, VenueBookingRepository,
            AuthorizationService, NotificationService, AuditLogService, TransactionManager {
        private final Map<UUID, VenueRequest> map = new HashMap<>();
        private VenueAdministratorService workflow;
        private Actor actor;
        private String notificationEvent;
        private String auditEvent;
        private String auditReason;

        private Store() {
            workflow = new VenueAdministratorService(this, this, this, this, this, this);
        }

        private Map<UUID, VenueRequest> getMap() { return map; }
        @Override public VenueRequest get(UUID requestId) { return map.get(requestId); }
        @Override public void save(VenueRequest request) { map.put(request.requestId(), request); }
        @Override public boolean hasConflict(UUID venueId, OffsetDateTime startsAt, OffsetDateTime endsAt) {
            return false;
        }
        @Override public void createFromApprovedRequest(VenueRequest request, UUID approverId) { }
        @Override public void requireVenueRequestAccess(Actor actor, VenueRequest request) {
            requireRole(actor, Role.VENUE_ADMINISTRATOR);
            this.actor = actor;
        }
        @Override public void notify(UUID recipientId, String event, Map<String, String> data) {
            notificationEvent = event;
        }
        @Override public void record(Actor actor, String action, String entityType, UUID entityId,
                String previousState, String newState, String reason) {
            auditEvent = action;
            auditReason = reason;
        }
        @Override public <T> T execute(Supplier<T> work) { return work.get(); }
    }
}
