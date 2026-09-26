package seedu.eventmanager.ui;

import seedu.eventmanager.venue.VenueRequest;
import seedu.eventmanager.service.VenueRequestDisplay;
import seedu.eventmanager.service.ApprovedBookingDisplay;
import java.util.List;
import java.util.UUID;

/** Client boundary used by the Venue Administrator screen. */
public interface VenueAdministratorApiClient {
    VenueAdministratorDashboardData loadDashboard();
    VenueRequest approveRequest(UUID requestId);
    VenueRequest rejectRequest(UUID requestId, String reason);

    record VenueAdministratorDashboardData(
            List<VenueRequest> pendingRequests,
            List<VenueRequestDisplay> pendingRequestDisplays,
            List<ApprovedBookingDisplay> approvedBookingDisplays,
            List<BookingSummary> upcomingBookings,
            List<AvailabilitySummary> availability,
            List<WarningSummary> warnings,
            List<ActivitySummary> recentActivity) {
        public VenueAdministratorDashboardData {
            pendingRequests = List.copyOf(pendingRequests);
            pendingRequestDisplays = List.copyOf(pendingRequestDisplays);
            approvedBookingDisplays = List.copyOf(approvedBookingDisplays);
            upcomingBookings = List.copyOf(upcomingBookings);
            availability = List.copyOf(availability);
            warnings = List.copyOf(warnings);
            recentActivity = List.copyOf(recentActivity);
        }

        public VenueAdministratorDashboardData(List<VenueRequest> pendingRequests,
                List<BookingSummary> upcomingBookings, List<AvailabilitySummary> availability,
                List<WarningSummary> warnings, List<ActivitySummary> recentActivity) {
            this(pendingRequests,
                    pendingRequests.stream()
                            .map(request -> new VenueRequestDisplay(request, null, null, null, null))
                            .toList(),
                    List.of(),
                    upcomingBookings, availability, warnings, recentActivity);
        }
    }

    record BookingSummary(UUID bookingId, UUID eventId, UUID venueId, String startsAt, String endsAt) { }
    record AvailabilitySummary(UUID venueId, String venueName, String status, String interval) { }
    record WarningSummary(String code, String message, UUID requestId) { }
    record ActivitySummary(String action, UUID requestId, String timestamp, String message) { }
}
