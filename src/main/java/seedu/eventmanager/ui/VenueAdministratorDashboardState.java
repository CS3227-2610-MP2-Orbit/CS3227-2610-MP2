package seedu.eventmanager.ui;

import java.util.List;

/** Explicit UI state for loading, empty, successful, and failed dashboard views. */
public record VenueAdministratorDashboardState(Status status,
        VenueAdministratorApiClient.VenueAdministratorDashboardData data,
        String message) {
    public enum Status { LOADING, READY, EMPTY, ERROR }

    public static VenueAdministratorDashboardState loading() {
        return new VenueAdministratorDashboardState(Status.LOADING, emptyData(), null);
    }

    public static VenueAdministratorDashboardState ready(
            VenueAdministratorApiClient.VenueAdministratorDashboardData data) {
        boolean empty = data.pendingRequests().isEmpty() && data.approvedBookingDisplays().isEmpty();
        return new VenueAdministratorDashboardState(empty ? Status.EMPTY : Status.READY, data, null);
    }

    public static VenueAdministratorDashboardState error(String message) {
        return new VenueAdministratorDashboardState(Status.ERROR, emptyData(), message);
    }

    private static VenueAdministratorApiClient.VenueAdministratorDashboardData emptyData() {
        return new VenueAdministratorApiClient.VenueAdministratorDashboardData(
                List.of(), List.of(), List.of());
    }
}
