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
    public void requireRole(Actor actor, Role role) {
        AuthorizationService.super.requireRole(actor, role);
        database.withConnection(connection -> {
            try (var statement = connection.prepareStatement(
                    "SELECT active, role FROM users WHERE user_id = ?")) {
                statement.setObject(1, actor.userId());
                try (var result = statement.executeQuery()) {
                    if (!result.next() || !result.getBoolean("active")) {
                        revokeSessions(actor.userId(), connection);
                        throw new ApplicationException("ACCOUNT_INACTIVE", "This account is inactive.");
                    }
                    if (!role.name().equals(result.getString("role"))) {
                        throw new ApplicationException("FORBIDDEN",
                                "The actor is not authorized for this operation.");
                    }
                    return null;
                }
            } catch (java.sql.SQLException exception) {
                throw new IllegalStateException("Could not verify authorization status.", exception);
            }
        });
    }

    private static void revokeSessions(java.util.UUID userId, java.sql.Connection connection)
            throws java.sql.SQLException {
        try (var revoke = connection.prepareStatement(
                "UPDATE user_sessions SET revoked_at = CURRENT_TIMESTAMP "
                        + "WHERE user_id = ? AND revoked_at IS NULL")) {
            revoke.setObject(1, userId);
            revoke.executeUpdate();
        }
    }

    @Override
    public void requireVenueRequestAccess(Actor actor, VenueRequest request) {
        requireRole(actor, Role.VENUE_ADMINISTRATOR);
        // All authenticated Venue Administrators have the same venue-management access.
    }
}
