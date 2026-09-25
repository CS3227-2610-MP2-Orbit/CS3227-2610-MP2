package seedu.eventmanager.storage;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import seedu.eventmanager.common.Role;
import seedu.eventmanager.service.UserAccessRepository;

/** PostgreSQL user and venue-scope persistence for local administration. */
public final class JdbcUserAccessRepository implements UserAccessRepository {
    private final JdbcDatabase database;
    private final PasswordHasher passwords;

    public JdbcUserAccessRepository(JdbcDatabase database, PasswordHasher passwords) {
        this.database = Objects.requireNonNull(database);
        this.passwords = Objects.requireNonNull(passwords);
    }

    @Override
    public List<UserSummary> findAllUsers() {
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement(
                    "SELECT user_id, username, role, active FROM users ORDER BY username")) {
                try (var result = statement.executeQuery()) {
                    List<UserSummary> users = new ArrayList<>();
                    while (result.next()) {
                        users.add(new UserSummary(result.getObject("user_id", UUID.class),
                                result.getString("username"), Role.valueOf(result.getString("role")),
                                result.getBoolean("active")));
                    }
                    return users;
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not load users.", exception);
            }
        });
    }

    @Override
    public void createUser(String username, String password, Role role) {
        database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO users (user_id, username, password_hash, role, active, created_at, updated_at)
                    VALUES (?, ?, ?, ?, TRUE, ?, ?)""")) {
                UUID userId = UUID.randomUUID();
                OffsetDateTime now = OffsetDateTime.now();
                statement.setObject(1, userId);
                statement.setString(2, username);
                statement.setString(3, passwords.hash(password));
                statement.setString(4, role.name());
                statement.setObject(5, now);
                statement.setObject(6, now);
                statement.executeUpdate();
                return null;
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not create user.", exception);
            }
        });
    }

    @Override
    public void updateUser(UUID userId, String username, Role role, boolean active) {
        if (userId == null || username == null || username.isBlank() || role == null) {
            throw new IllegalArgumentException("User ID, username, and role are required.");
        }
        database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    UPDATE users
                    SET username = ?, role = ?, active = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE user_id = ?""")) {
                statement.setString(1, username.trim());
                statement.setString(2, role.name());
                statement.setBoolean(3, active);
                statement.setObject(4, userId);
                if (statement.executeUpdate() == 0) {
                    throw new IllegalArgumentException("User account was not found.");
                }
                return null;
            } catch (SQLException exception) {
                if ("23505".equals(exception.getSQLState())) {
                    throw new IllegalArgumentException("That username is already in use.");
                }
                throw new IllegalStateException("Could not update user.", exception);
            }
        });
    }
}
