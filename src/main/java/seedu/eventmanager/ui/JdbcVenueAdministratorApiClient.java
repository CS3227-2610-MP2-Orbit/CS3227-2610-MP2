package seedu.eventmanager.ui;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.service.VenueAdministratorService;
import seedu.eventmanager.service.VenueRequestRepository;
import seedu.eventmanager.venue.VenueRequest;

/** PostgreSQL-backed client boundary for the Venue Administrator dashboard. */
public final class JdbcVenueAdministratorApiClient implements VenueAdministratorApiClient {
    private final VenueAdministratorService workflow;
    private final VenueRequestRepository requests;
    private final Actor actor;

    public JdbcVenueAdministratorApiClient(VenueAdministratorService workflow,
            VenueRequestRepository requests, Actor actor) {
        this.workflow = Objects.requireNonNull(workflow);
        this.requests = Objects.requireNonNull(requests);
        this.actor = Objects.requireNonNull(actor);
    }

    @Override
    public VenueAdministratorDashboardData loadDashboard() {
        List<VenueRequest> pending = requests.findSubmitted();
        return new VenueAdministratorDashboardData(pending, requests.findSubmittedDisplay(),
                requests.findApprovedBookingDisplays());
    }

    @Override
    public VenueRequest approveRequest(UUID requestId) {
        return workflow.approve(actor, requestId);
    }

    @Override
    public VenueRequest rejectRequest(UUID requestId, String reason) {
        return workflow.reject(actor, requestId, reason);
    }
}
