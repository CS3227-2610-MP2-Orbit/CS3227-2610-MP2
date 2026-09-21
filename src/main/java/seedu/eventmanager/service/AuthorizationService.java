package seedu.eventmanager.service;

import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.venue.VenueRequest;

public interface AuthorizationService {
    default void requireAuthenticated(Actor actor) {
        if (actor == null || actor.userId() == null) {
            throw new ApplicationException("UNAUTHENTICATED", "Authentication is required.");
        }
    }

    default void requireRole(Actor actor, Role role) {
        requireAuthenticated(actor);
        if (actor.role() != role) {
            throw new ApplicationException("FORBIDDEN", "The actor is not authorized for this operation.");
        }
    }

    /** Implementations must enforce resource/tenant scope, not just the role. */
    void requireVenueRequestAccess(Actor actor, VenueRequest request);
}
