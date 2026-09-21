package seedu.eventmanager.service;

import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.venue.VenueRequestStatus;
import seedu.eventmanager.venue.VenueRequestValidator;
import java.util.Map;
import java.util.UUID;

public final class VenueAdministratorService {
    private final VenueRequestRepository requests;
    private final VenueBookingRepository bookings;
    private final AuthorizationService authorization;
    private final NotificationService notifications;
    private final AuditLogService audit;
    private final TransactionManager transactions;

    public VenueAdministratorService(VenueRequestRepository requests, VenueBookingRepository bookings,
            AuthorizationService authorization, NotificationService notifications,
            AuditLogService audit, TransactionManager transactions) {
        this.requests = requests;
        this.bookings = bookings;
        this.authorization = authorization;
        this.notifications = notifications;
        this.audit = audit;
        this.transactions = transactions;
    }

    public VenueRequest approve(Actor administrator, UUID requestId) {
        authorization.requireRole(administrator, Role.VENUE_ADMINISTRATOR);
        return transactions.execute(() -> decide(administrator, requestId, true, null));
    }

    public VenueRequest reject(Actor administrator, UUID requestId, String reason) {
        authorization.requireRole(administrator, Role.VENUE_ADMINISTRATOR);
        if (reason == null || reason.isBlank()) {
            throw new ApplicationException("REJECTION_REASON_REQUIRED", "A rejection reason is required.");
        }
        return transactions.execute(() -> decide(administrator, requestId, false, reason));
    }

    private VenueRequest decide(Actor administrator, UUID requestId, boolean approve, String reason) {
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
            throw new ApplicationException("BOOKING_CONFLICT", "The venue is already occupied for this interval.");
        }
        VenueRequestStatus next = approve ? VenueRequestStatus.APPROVED : VenueRequestStatus.REJECTED;
        VenueRequest updated = new VenueRequest(current.requestId(), current.eventId(), current.venueId(),
                current.organizerId(), current.startsAt(), current.endsAt(), current.expectedAttendance(), next);
        requests.save(updated);
        if (approve) {
            bookings.createFromApprovedRequest(updated, administrator.userId());
        }
        audit.record(administrator, approve ? "VENUE_REQUEST_APPROVED" : "VENUE_REQUEST_REJECTED",
                "VENUE_REQUEST", requestId, current.status().name(), next.name(), reason);
        notifications.notify(current.organizerId(), approve ? "VENUE_REQUEST_APPROVED" : "VENUE_REQUEST_REJECTED",
                Map.of("requestId", requestId.toString()));
        return updated;
    }
}
