package seedu.eventmanager.storage;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.Role;

/** Local username/password session store for development and prototyping. */
public final class JdbcLocalSessionService {
    private static final long SESSION_HOURS = 8;
    private final JdbcDatabase database;
    private final PasswordHasher passwords;
    private final SecureRandom random = new SecureRandom();

    public JdbcLocalSessionService(JdbcDatabase database, PasswordHasher passwords) {
        this.database = Objects.requireNonNull(database);
        this.passwords = Objects.requireNonNull(passwords);
    }

    public String createUser(String username, String password, Role role) {
        String passwordHash = passwords.hash(password);
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO users (user_id, username, password_hash, role, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)""")) {
                UUID userId = UUID.randomUUID();
                OffsetDateTime now = OffsetDateTime.now();
                statement.setObject(1, userId);
                statement.setString(2, username);
                statement.setString(3, passwordHash);
                statement.setString(4, role.name());
                statement.setObject(5, now);
                statement.setObject(6, now);
                statement.executeUpdate();
                return userId.toString();
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not create local user.", exception);
            }
        });
    }

    public Session login(String username, String password) {
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement(
                    "SELECT user_id, password_hash, role FROM users WHERE username = ? AND active = TRUE")) {
                statement.setString(1, username);
                try (var result = statement.executeQuery()) {
                    if (!result.next() || !passwords.matches(password, result.getString("password_hash"))) {
                        throw new IllegalArgumentException("Invalid username or password.");
                    }
                    UUID userId = result.getObject("user_id", UUID.class);
                    String token = token();
                    OffsetDateTime now = OffsetDateTime.now();
                    try (var insert = connection.prepareStatement("""
                            INSERT INTO user_sessions (session_id, user_id, token_hash, expires_at, created_at)
                            VALUES (?, ?, ?, ?, ?)""")) {
                        insert.setObject(1, UUID.randomUUID());
                        insert.setObject(2, userId);
                        insert.setString(3, hashToken(token));
                        insert.setObject(4, now.plusHours(SESSION_HOURS));
                        insert.setObject(5, now);
                        insert.executeUpdate();
                    }
                    return new Session(token, new Actor(userId, Role.valueOf(result.getString("role"))));
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not create local session.", exception);
            }
        });
    }

    public Actor resolve(String token) {
        return database.withConnection(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT u.user_id, u.role
                    FROM user_sessions s
                    JOIN users u ON u.user_id = s.user_id
                    WHERE s.token_hash = ? AND s.revoked_at IS NULL
                      AND s.expires_at > CURRENT_TIMESTAMP AND u.active = TRUE""")) {
                statement.setString(1, hashToken(token));
                try (var result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new IllegalArgumentException("Invalid or expired session.");
                    }
                    return new Actor(result.getObject("user_id", UUID.class),
                            Role.valueOf(result.getString("role")));
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not resolve local session.", exception);
            }
        });
    }

    public void revoke(String token) {
        database.withConnection(connection -> {
            try (var statement = connection.prepareStatement(
                    "UPDATE user_sessions SET revoked_at = CURRENT_TIMESTAMP WHERE token_hash = ?")) {
                statement.setString(1, hashToken(token));
                statement.executeUpdate();
                return null;
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not revoke local session.", exception);
            }
        });
    }

    private String token() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String token) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    public record Session(String token, Actor actor) { }
}
