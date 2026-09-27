package seedu.eventmanager.storage;

import java.util.Objects;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.service.AuthorizationService;
import seedu.eventmanager.venue.VenueRequest;

/** Enforces the shared Venue Administrator role authorization policy. */
public final class JdbcAuthorizationService implements AuthorizationService {
    private final JdbcDatabase database;

    public JdbcAuthorizationService(JdbcDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public void requireVenueRequestAccess(Actor actor, VenueRequest request) {
        requireRole(actor, Role.VENUE_ADMINISTRATOR);
        // All authenticated Venue Administrators have the same venue-management access.
    }
}
