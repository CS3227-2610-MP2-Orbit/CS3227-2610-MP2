package seedu.eventmanager.event;

import java.util.Objects;
import java.util.Set;

/** Authenticated organizer identity supplied by the shared authentication layer. */
public record OrganizerIdentity(String userId, Set<String> ownedClubIds) {
    public OrganizerIdentity {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(ownedClubIds, "ownedClubIds");
        if (userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        userId = userId.strip();
        ownedClubIds = Set.copyOf(ownedClubIds);
    }
}
