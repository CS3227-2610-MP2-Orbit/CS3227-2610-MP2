package seedu.eventmanager.ui;

import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.venue.VenueRequest;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/** Presentation controller; business rules remain in the backend service. */
public final class VenueAdministratorDashboardController {
    private final Actor actor;
    private final VenueAdministratorApiClient client;
    private final Consumer<VenueAdministratorDashboardState> stateListener;
    private VenueAdministratorDashboardState state = VenueAdministratorDashboardState.loading();

    public VenueAdministratorDashboardController(Actor actor, VenueAdministratorApiClient client,
            Consumer<VenueAdministratorDashboardState> stateListener) {
        this.actor = Objects.requireNonNull(actor);
        this.client = Objects.requireNonNull(client);
        this.stateListener = Objects.requireNonNull(stateListener);
        if (actor.role() != Role.VENUE_ADMINISTRATOR) {
            throw new ApplicationException("FORBIDDEN", "This dashboard is restricted to Venue Administrators.");
        }
    }

    public void load() {
        publish(VenueAdministratorDashboardState.loading());
        try {
            publish(VenueAdministratorDashboardState.ready(client.loadDashboard()));
        } catch (RuntimeException exception) {
            publish(VenueAdministratorDashboardState.error(safeMessage(exception)));
        }
    }

    public void approve(UUID requestId) {
        decide(() -> client.approveRequest(requestId));
    }

    public void reject(UUID requestId, String reason) {
        if (reason == null || reason.isBlank()) {
            publish(VenueAdministratorDashboardState.error("A rejection reason is required."));
            return;
        }
        decide(() -> client.rejectRequest(requestId, reason));
    }

    public VenueAdministratorDashboardState state() {
        return state;
    }

    private void decide(java.util.function.Supplier<VenueRequest> action) {
        try {
            action.get();
            load();
        } catch (RuntimeException exception) {
            publish(VenueAdministratorDashboardState.error(safeMessage(exception)));
        }
    }

    private void publish(VenueAdministratorDashboardState next) {
        state = next;
        stateListener.accept(next);
    }

    private static String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? "The request could not be completed." : exception.getMessage();
    }
}
