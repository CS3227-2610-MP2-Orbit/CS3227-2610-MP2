package seedu.eventmanager.storage;

import java.sql.SQLException;
import java.util.Objects;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.service.AuthorizationService;
import seedu.eventmanager.venue.VenueRequest;

/** Enforces venue-admin access using persisted venue scope assignments. */
public final class JdbcAuthorizationService implements AuthorizationService {
    private final JdbcDatabase database;

    public JdbcAuthorizationService(JdbcDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    public void grantVenueAccess(java.util.UUID userId, java.util.UUID venueId) {
        database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO venue_administrator_venues (user_id, venue_id, created_at)
                    VALUES (?, ?, CURRENT_TIMESTAMP)
                    ON CONFLICT (user_id, venue_id) DO NOTHING""")) {
                statement.setObject(1, userId);
                statement.setObject(2, venueId);
                statement.executeUpdate();
                return null;
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not grant venue access.", exception);
            }
        });
    }

    @Override
    public void requireVenueRequestAccess(Actor actor, VenueRequest request) {
        requireRole(actor, Role.VENUE_ADMINISTRATOR);
        database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT EXISTS (
                        SELECT 1 FROM venue_administrator_venues
                        WHERE user_id = ? AND venue_id = ?
                    )""")) {
                statement.setObject(1, actor.userId());
                statement.setObject(2, request.venueId());
                try (var result = statement.executeQuery()) {
                    result.next();
                    if (!result.getBoolean(1)) {
                        throw new ApplicationException("FORBIDDEN", "The administrator has no access to this venue.");
                    }
                    return null;
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not verify venue access.", exception);
            }
        });
    }
}
