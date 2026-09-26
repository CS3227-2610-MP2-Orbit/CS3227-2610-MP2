package seedu.eventmanager.service;

import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.common.JavaUtilStructuredLogger;
import seedu.eventmanager.common.Metrics;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.common.StructuredLogger;
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;
import seedu.eventmanager.venue.VenueRequestValidator;
import java.util.Map;
import java.util.UUID;

public final class VenueAdministratorService {
    public static final String REASON_VENUE_BOOKED = "Venue already booked";
    public static final String REASON_CAPACITY_EXCEEDED = "Requested capacity exceeds venue capacity";
    private final VenueRequestRepository requests;
    private final VenueBookingRepository bookings;
    private final AuthorizationService authorization;
    private final NotificationService notifications;
    private final AuditLogService audit;
    private final TransactionManager transactions;
    private final StructuredLogger logger;
    private final Metrics metrics;

    public VenueAdministratorService(VenueRequestRepository requests, VenueBookingRepository bookings,
            AuthorizationService authorization, NotificationService notifications,
            AuditLogService audit, TransactionManager transactions) {
        this(requests, bookings, authorization, notifications, audit, transactions,
                new JavaUtilStructuredLogger(VenueAdministratorService.class), new seedu.eventmanager.common.NoopMetrics());
    }

    public VenueAdministratorService(VenueRequestRepository requests, VenueBookingRepository bookings,
            AuthorizationService authorization, NotificationService notifications,
            AuditLogService audit, TransactionManager transactions, StructuredLogger logger, Metrics metrics) {
        this.requests = requests;
        this.bookings = bookings;
        this.authorization = authorization;
        this.notifications = notifications;
        this.audit = audit;
        this.transactions = transactions;
        this.logger = logger;
        this.metrics = metrics;
    }

    public VenueRequest approve(Actor administrator, UUID requestId) {
        return decideWithObservability(administrator, requestId, true, null);
    }

    public VenueRequest reject(Actor administrator, UUID requestId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ApplicationException("REJECTION_REASON_REQUIRED", "A rejection reason is required.");
        }
        if (!REASON_VENUE_BOOKED.equals(reason) && !REASON_CAPACITY_EXCEEDED.equals(reason)) {
            throw new ApplicationException("INVALID_DECISION_REASON", "Select a supported rejection reason.");
        }
        return decideWithObservability(administrator, requestId, false, reason);
    }

    private VenueRequest decideWithObservability(Actor administrator, UUID requestId, boolean approve, String reason) {
        String action = approve ? "approve" : "reject";
        String correlationId = UUID.randomUUID().toString();
        try {
            authorization.requireRole(administrator, Role.VENUE_ADMINISTRATOR);
            if (!approve && (reason == null || reason.isBlank())) {
                throw new ApplicationException("REJECTION_REASON_REQUIRED", "A rejection reason is required.");
            }
            logger.info("venue_request_decision_started", Map.of("correlationId", correlationId,
                    "requestId", String.valueOf(requestId), "action", action,
                    "userId", userId(administrator), "role", role(administrator)));
            VenueRequest result = transactions.execute(() -> decide(administrator, requestId, approve, reason, correlationId));
            logger.info("venue_request_decision_succeeded", Map.of("correlationId", correlationId,
                    "requestId", String.valueOf(requestId), "action", action, "result", result.status().name()));
            metrics.increment(approve ? "venue_requests.approved" : "venue_requests.rejected");
            return result;
        } catch (ApplicationException exception) {
            if ("FORBIDDEN".equals(exception.code())
                    || "UNAUTHENTICATED".equals(exception.code())) {
                metrics.increment("venue_requests.authorization_failures");
                logger.warn("venue_request_authorization_failed", Map.of("correlationId", correlationId,
                        "requestId", String.valueOf(requestId), "action", action, "userId", userId(administrator),
                        "role", role(administrator), "errorCode", exception.code()));
            } else {
                logger.warn("venue_request_decision_failed", Map.of("correlationId", correlationId,
                        "requestId", String.valueOf(requestId), "action", action, "errorCode", exception.code()));
            }
            throw exception;
        } catch (RuntimeException exception) {
            metrics.increment("venue_requests.service_failures");
            logger.error("venue_request_unexpected_failure", Map.of("correlationId", correlationId,
                    "requestId", String.valueOf(requestId), "action", action), exception);
            throw exception;
        }
    }

    private VenueRequest decide(Actor administrator, UUID requestId, boolean approve, String reason,
            String correlationId) {
        VenueRequest current = requests.get(requestId);
        if (current == null) {
            throw new ApplicationException("REQUEST_NOT_FOUND", "Venue request was not found.");
        }
        authorization.requireVenueRequestAccess(administrator, current);
        if (current.status() != VenueRequestStatus.SUBMITTED) {
            throw new ApplicationException("INVALID_STATE", "Only submitted requests can be decided.");
        }
        VenueRequestValidator.validate(current);
        if (approve && bookings.hasConflict(current.venueId(), current.startsAt(), current.endsAt())) {
            metrics.increment("venue_requests.conflicts");
            logger.warn("venue_request_conflict_detected", Map.of("correlationId", correlationId,
                    "requestId", requestId.toString(), "venueId", current.venueId().toString(),
                    "eventId", current.eventId().toString(), "result", "conflict"));
            throw new ApplicationException("BOOKING_CONFLICT", "The venue is already occupied for this interval.");
        }
        VenueRequestStatus next = approve ? VenueRequestStatus.APPROVED : VenueRequestStatus.REJECTED;
        VenueRequest updated = new VenueRequest(current.requestId(), current.eventId(), current.venueId(),
                current.organizerId(), current.startsAt(), current.endsAt(), current.expectedAttendance(), next);
        requests.save(updated, administrator.userId(), reason);
        if (approve) {
            bookings.createFromApprovedRequest(updated, administrator.userId());
        }
        audit.record(administrator, approve ? "VENUE_REQUEST_APPROVED" : "VENUE_REQUEST_REJECTED",
                "VENUE_REQUEST", requestId, current.status().name(), next.name(), reason);
        try {
            notifications.notify(current.organizerId(), approve ? "VENUE_REQUEST_APPROVED" : "VENUE_REQUEST_REJECTED",
                    Map.of("requestId", requestId.toString()));
        } catch (RuntimeException notificationFailure) {
            metrics.increment("venue_requests.notification_failures");
            logger.error("venue_request_notification_failed", Map.of("correlationId", correlationId,
                    "requestId", requestId.toString(), "action", approve ? "approve" : "reject"),
                    notificationFailure);
        }
        return updated;
    }

    private static String userId(Actor actor) {
        return actor == null || actor.userId() == null ? "anonymous" : actor.userId().toString();
    }

    private static String role(Actor actor) {
        return actor == null || actor.role() == null ? "none" : actor.role().name();
    }
}
