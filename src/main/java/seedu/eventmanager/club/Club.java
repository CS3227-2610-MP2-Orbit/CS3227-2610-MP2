package seedu.eventmanager.club;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A club owned by exactly one Club Organizer account. */
public record Club(UUID id, String name, UUID ownerId, Instant createdAt) {
    public Club {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
