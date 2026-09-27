package seedu.eventmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;

class VenueAdministratorServiceTest {
    private final UUID requestId = UUID.randomUUID();
    private final UUID organizerId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final Actor administrator = new Actor(UUID.randomUUID(), Role.VENUE_ADMINISTRATOR);
    private final OffsetDateTime start = OffsetDateTime.parse("2030-01-01T10:00:00Z");
    private FakeRequestRepository requests;
    private FakeBookingRepository bookings;
    private FakeNotificationService notifications;
    private FakeAuditLogService audit;
    private VenueAdministratorService service;

    @BeforeEach
    void setUp() {
        requests = new FakeRequestRepository(submittedRequest());
        bookings = new FakeBookingRepository();
        notifications = new FakeNotificationService();
        audit = new FakeAuditLogService();
        service = new VenueAdministratorService(requests, bookings, new TestAuthorizationService(), notifications,
                audit, new ImmediateTransactionManager());
    }

    @Test
    void approvalCreatesBookingNotificationAndAuditEvent() {
        VenueRequest result = service.approve(administrator, requestId);

        assertEquals(VenueRequestStatus.APPROVED, result.status());
        assertEquals(VenueRequestStatus.APPROVED, requests.saved.status());
        assertEquals(1, bookings.createCount);
        assertEquals(organizerId, notifications.recipientId);
        assertEquals("VENUE_REQUEST_APPROVED", notifications.event);
        assertEquals("VENUE_REQUEST_APPROVED", audit.action);
        assertEquals(VenueRequestStatus.SUBMITTED.name(), audit.previousState);
        assertEquals(VenueRequestStatus.APPROVED.name(), audit.newState);
    }

    @Test
    void rejectionRequiresReasonAndDoesNotCreateBooking() {
        assertCode("REJECTION_REASON_REQUIRED", () -> service.reject(administrator, requestId, ""));

        VenueRequest result = service.reject(administrator, requestId,
                VenueAdministratorService.REASON_VENUE_BOOKED);

        assertEquals(VenueRequestStatus.REJECTED, result.status());
        assertEquals(0, bookings.createCount);
        assertEquals("VENUE_REQUEST_REJECTED", notifications.event);
        assertEquals(VenueAdministratorService.REASON_VENUE_BOOKED, audit.reason);
    }

    @Test
    void occupiedVenuePreventsApprovalAndLeavesRequestSubmitted() {
        bookings.conflict = true;

        assertCode("BOOKING_CONFLICT", () -> service.approve(administrator, requestId));

        assertEquals(VenueRequestStatus.SUBMITTED, requests.current.status());
        assertEquals(0, bookings.createCount);
        assertFalse(notifications.sent);
        assertFalse(audit.recorded);
    }

    @Test
    void alreadyApprovedRequestCannotBeApprovedAgain() {
        requests.current = submittedRequest();
        requests.current = withStatus(VenueRequestStatus.APPROVED);

        assertCode("INVALID_STATE", () -> service.approve(administrator, requestId));
        assertEquals(0, bookings.createCount);
    }

    @Test
    void alreadyRejectedRequestCannotBeRejectedAgain() {
        requests.current = withStatus(VenueRequestStatus.REJECTED);

        assertCode("INVALID_STATE", () -> service.reject(administrator, requestId,
                VenueAdministratorService.REASON_VENUE_BOOKED));
        assertEquals(0, bookings.createCount);
    }

    @Test
    void missingRequestIsRejectedWithoutSideEffects() {
        requests.current = null;

        assertCode("REQUEST_NOT_FOUND", () -> service.approve(administrator, requestId));
        assertFalse(notifications.sent);
        assertFalse(audit.recorded);
    }

    @Test
    void duplicateActiveRequestIsRejectedByConflictBoundary() {
        bookings.conflict = true;

        assertCode("BOOKING_CONFLICT", () -> service.approve(administrator, requestId));
        assertEquals(0, bookings.createCount);
    }

    @Test
    void authorizationFailureStopsWorkflowBeforeSideEffects() {
        service = new VenueAdministratorService(requests, bookings, new DenyingAuthorizationService(), notifications,
                audit, new ImmediateTransactionManager());

        assertCode("FORBIDDEN", () -> service.approve(administrator, requestId));
        assertEquals(0, bookings.createCount);
        assertFalse(notifications.sent);
        assertFalse(audit.recorded);
    }

    @Test
    void transactionFailureIsPropagatedAndNoPostCommitSideEffectsOccur() {
        service = new VenueAdministratorService(requests, bookings, new TestAuthorizationService(), notifications,
                audit, new FailingTransactionManager());

        assertCode("TRANSACTION_FAILED", () -> service.approve(administrator, requestId));
        assertFalse(notifications.sent);
        assertFalse(audit.recorded);
    }

    private VenueRequest submittedRequest() {
        return new VenueRequest(requestId, eventId, venueId, organizerId, start, start.plusHours(1), 50,
                VenueRequestStatus.SUBMITTED);
    }

    private VenueRequest withStatus(VenueRequestStatus status) {
        VenueRequest source = submittedRequest();
        return new VenueRequest(source.requestId(), source.eventId(), source.venueId(), source.organizerId(),
                source.startsAt(), source.endsAt(), source.expectedAttendance(), status);
    }

    private static void assertCode(String code, org.junit.jupiter.api.function.Executable action) {
        ApplicationException exception = assertThrows(ApplicationException.class, action);
        assertEquals(code, exception.code());
    }

    private static final class FakeRequestRepository implements VenueRequestRepository {
        private VenueRequest current;
        private VenueRequest saved;
        private FakeRequestRepository(VenueRequest current) { this.current = current; }
        @Override public VenueRequest get(UUID requestId) { return current; }
        @Override public void save(VenueRequest request) { saved = request; current = request; }
    }

    private static final class FakeBookingRepository implements VenueBookingRepository {
        private boolean conflict;
        private int createCount;
        @Override public boolean hasConflict(UUID venueId, OffsetDateTime startsAt, OffsetDateTime endsAt) {
            return conflict;
        }
        @Override public void createFromApprovedRequest(VenueRequest request, UUID approverId) { createCount++; }
    }

    private static final class FakeNotificationService implements NotificationService {
        private boolean sent;
        private UUID recipientId;
        private String event;
        @Override public void notify(UUID recipientId, String event, Map<String, String> data) {
            sent = true;
            this.recipientId = recipientId;
            this.event = event;
        }
    }

    private static final class FakeAuditLogService implements AuditLogService {
        private boolean recorded;
        private String action;
        private String previousState;
        private String newState;
        private String reason;
        @Override public void record(Actor actor, String action, String entityType, UUID entityId,
                String previousState, String newState, String reason) {
            recorded = true;
            this.action = action;
            this.previousState = previousState;
            this.newState = newState;
            this.reason = reason;
        }
    }

    private static class TestAuthorizationService implements AuthorizationService {
        @Override public void requireVenueRequestAccess(Actor actor, VenueRequest request) {
            requireRole(actor, Role.VENUE_ADMINISTRATOR);
        }
    }

    private static final class DenyingAuthorizationService extends TestAuthorizationService {
        @Override public void requireVenueRequestAccess(Actor actor, VenueRequest request) {
            throw new ApplicationException("FORBIDDEN", "No access to this venue.");
        }
    }

    private static final class ImmediateTransactionManager implements TransactionManager {
        @Override public <T> T execute(Supplier<T> work) { return work.get(); }
    }

    private static final class FailingTransactionManager implements TransactionManager {
        @Override public <T> T execute(Supplier<T> work) {
            throw new ApplicationException("TRANSACTION_FAILED", "Database transaction failed.");
        }
    }
}
