package seedu.eventmanager.event;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * Transitional mapper from Organizer string identity to Jordan's UUID organizer_id.
 * Merge debt: replace with shared users-table lookup when auth is unified.
 */
public final class OrganizerIds {
    private OrganizerIds() { }

    public static UUID toUuid(String userId) {
        Objects.requireNonNull(userId, "userId");
        String trimmed = userId.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        try {
            return UUID.fromString(trimmed);
        } catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes(("organizer:" + trimmed).getBytes(StandardCharsets.UTF_8));
        }
    }
}
