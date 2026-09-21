package seedu.eventmanager.service;

import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.common.Role;

public interface AuthorizationService {
    default void requireRole(Actor actor, Role role) {
        if (actor == null || actor.role() != role) {
            throw new ApplicationException("FORBIDDEN", "The actor is not authorized for this operation.");
        }
    }
}
