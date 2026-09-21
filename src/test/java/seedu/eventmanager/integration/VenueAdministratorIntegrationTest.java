package seedu.eventmanager.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
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
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;

/**
 * Cross-layer tests using the real workflow service and in-memory adapters.
 * PostgreSQL and HTTP integration are not available in the current skeleton.
 */
class VenueAdministratorIntegrationTest {
    private final UUID requestId = UUID.randomUUID();
    private final UUID organizerId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final Actor organizer = new Actor(organizerId, Role.CLUB_ORGANIZER);
    private final Actor administrator = new Actor(UUID.randomUUID(), Role.VENUE_ADMINISTRATOR);
    private InMemoryStore store;
    private VenueAdministratorService workflow;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        workflow = new VenueAdministratorService(store, store, store, store, store, store);
    }

    @Test
    void submittedRequestIsApprovedAndPropagatesBookingNotificationAndAudit() {
        store.submit(request());

        VenueRequest pending = store.get(requestId);
        assertEquals(VenueRequestStatus.SUBMITTED, pending.status());

        VenueRequest approved = workflow.approve(administrator, requestId);

        assertEquals(VenueRequestStatus.APPROVED, approved.status());
        assertEquals(VenueRequestStatus.APPROVED, store.get(requestId).status());
        assertEquals(VenueRequestStatus.APPROVED, store.bookingStatus(requestId));
        assertEquals("VENUE_REQUEST_APPROVED", store.lastNotification);
        assertEquals("VENUE_REQUEST_APPROVED", store.lastAuditAction);
    }

    @Test
    void conflictingBookingPreventsApprovalAndRollsBackState() {
        store.submit(request());
        store.conflict = true;

        assertCode("BOOKING_CONFLICT", () -> workflow.approve(administrator, requestId));

        assertEquals(VenueRequestStatus.SUBMITTED, store.get(requestId).status());
        assertFalse(store.bookings.containsKey(requestId));
        assertTrue(store.notifications.isEmpty());
        assertTrue(store.auditActions.isEmpty());
    }

    @Test
    void rejectionUpdatesRequestAndNotifiesOrganizer() {
        store.submit(request());

        VenueRequest rejected = workflow.reject(administrator, requestId, "Venue maintenance");

        assertEquals(VenueRequestStatus.REJECTED, rejected.status());
        assertFalse(store.bookings.containsKey(requestId));
        assertEquals("VENUE_REQUEST_REJECTED", store.lastNotification);
        assertEquals("Venue maintenance", store.lastAuditReason);
    }

    @Test
    void unauthorizedAdministratorActionHasNoSideEffects() {
        store.submit(request());

        assertCode("FORBIDDEN", () -> workflow.approve(organizer, requestId));

        assertEquals(VenueRequestStatus.SUBMITTED, store.get(requestId).status());
        assertTrue(store.bookings.isEmpty());
        assertTrue(store.notifications.isEmpty());
        assertTrue(store.auditActions.isEmpty());
    }

    @Test
    void duplicateApprovalIsRejectedAsInvalidTransition() {
        store.submit(request());
        workflow.approve(administrator, requestId);
        store.clearEvents();

        assertCode("INVALID_STATE", () -> workflow.approve(administrator, requestId));

        assertEquals(1, store.bookings.size());
        assertTrue(store.notifications.isEmpty());
        assertTrue(store.auditActions.isEmpty());
    }

    @Test
    void transactionFailureDoesNotPublishExternalEvents() {
        store.submit(request());
        store.transactionFailure = true;

        assertCode("TRANSACTION_FAILED", () -> workflow.approve(administrator, requestId));

        assertTrue(store.notifications.isEmpty());
        assertTrue(store.auditActions.isEmpty());
        assertTrue(store.bookings.isEmpty());
    }

    @Test
    void unavailableVenueIsRepresentedAsConflictByBookingBoundary() {
        store.submit(request());
        store.venueUnavailable = true;

        assertCode("BOOKING_CONFLICT", () -> workflow.approve(administrator, requestId));
        assertEquals(VenueRequestStatus.SUBMITTED, store.get(requestId).status());
    }

    private VenueRequest request() {
        OffsetDateTime start = OffsetDateTime.parse("2030-01-01T10:00:00Z");
        return new VenueRequest(requestId, eventId, venueId, organizerId, start, start.plusHours(1), 50,
                VenueRequestStatus.SUBMITTED);
    }

    private static void assertCode(String code, org.junit.jupiter.api.function.Executable action) {
        ApplicationException exception = assertThrows(ApplicationException.class, action);
        assertEquals(code, exception.code());
    }

    private static final class InMemoryStore implements VenueRequestRepository, VenueBookingRepository,
            AuthorizationService, NotificationService, AuditLogService, TransactionManager {
        private final Map<UUID, VenueRequest> requests = new HashMap<>();
        private final Map<UUID, VenueRequestStatus> bookings = new HashMap<>();
        private final ArrayList<String> notifications = new ArrayList<>();
        private final ArrayList<String> auditActions = new ArrayList<>();
        private VenueRequest saved;
        private String lastNotification;
        private String lastAuditAction;
        private String lastAuditReason;
        private boolean conflict;
        private boolean venueUnavailable;
        private boolean transactionFailure;

        void submit(VenueRequest request) {
            requests.put(request.requestId(), request);
        }

        VenueRequestStatus bookingStatus(UUID requestId) {
            return bookings.get(requestId);
        }

        void clearEvents() {
            notifications.clear();
            auditActions.clear();
            lastNotification = null;
            lastAuditAction = null;
        }

        @Override public VenueRequest get(UUID requestId) { return requests.get(requestId); }
        @Override public void save(VenueRequest request) { saved = request; requests.put(request.requestId(), request); }
        @Override public boolean hasConflict(UUID venueId, OffsetDateTime startsAt, OffsetDateTime endsAt) {
            return conflict || venueUnavailable;
        }
        @Override public void createFromApprovedRequest(VenueRequest request, UUID approverId) {
            bookings.put(request.requestId(), VenueRequestStatus.APPROVED);
        }
        @Override public void requireVenueRequestAccess(Actor actor, VenueRequest request) {
            requireRole(actor, Role.VENUE_ADMINISTRATOR);
        }
        @Override public void notify(UUID recipientId, String event, Map<String, String> data) {
            notifications.add(event);
            lastNotification = event;
        }
        @Override public void record(Actor actor, String action, String entityType, UUID entityId,
                String previousState, String newState, String reason) {
            auditActions.add(action);
            lastAuditAction = action;
            lastAuditReason = reason;
        }
        @Override public <T> T execute(Supplier<T> work) {
            if (transactionFailure) {
                throw new ApplicationException("TRANSACTION_FAILED", "Database transaction failed.");
            }
            return work.get();
        }
    }
}
