package seedu.eventmanager.service;

import java.util.Objects;
import java.util.UUID;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.storage.JdbcDatabase;

/** PostgreSQL-backed active-account check. */
public final class JdbcActiveUserChecker implements ActiveUserChecker {
    private final JdbcDatabase database;

    public JdbcActiveUserChecker(JdbcDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public void requireActive(UUID userId) {
        if (userId == null || !database.withConnection(connection -> {
            try (var statement = connection.prepareStatement(
                    "SELECT active FROM users WHERE user_id = ?")) {
                statement.setObject(1, userId);
                try (var result = statement.executeQuery()) {
                    return result.next() && result.getBoolean(1);
                }
            } catch (java.sql.SQLException exception) {
                throw new IllegalStateException("Could not verify account status.", exception);
            }
        })) {
            revokeSessions(userId);
            throw new ApplicationException("ACCOUNT_INACTIVE", "This account is inactive.");
        }
    }

    private void revokeSessions(UUID userId) {
        if (userId == null) return;
        database.withConnection(connection -> {
            try (var statement = connection.prepareStatement(
                    "UPDATE user_sessions SET revoked_at = CURRENT_TIMESTAMP "
                            + "WHERE user_id = ? AND revoked_at IS NULL")) {
                statement.setObject(1, userId);
                statement.executeUpdate();
                return null;
            } catch (java.sql.SQLException exception) {
                throw new IllegalStateException("Could not revoke inactive account sessions.", exception);
            }
        });
    }
}
