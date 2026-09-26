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
            List<ApprovedBookingDisplay> approvedBookingDisplays) {
        public VenueAdministratorDashboardData {
            pendingRequests = List.copyOf(pendingRequests);
            pendingRequestDisplays = List.copyOf(pendingRequestDisplays);
            approvedBookingDisplays = List.copyOf(approvedBookingDisplays);
        }
    }
}
