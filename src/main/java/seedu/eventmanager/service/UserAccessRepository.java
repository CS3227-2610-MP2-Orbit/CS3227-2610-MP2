package seedu.eventmanager.service;

import java.util.List;
import java.util.UUID;
import seedu.eventmanager.common.Actor;
import seedu.eventmanager.common.Role;

public interface UserAccessRepository {
    List<UserSummary> findAllUsers();
    void createUser(Actor actor, String username, String password, Role role);
    void updateUser(Actor actor, UUID userId, String username, Role role, boolean active);
    void grantVenueAccess(Actor actor, UUID userId, UUID venueId);

    record UserSummary(UUID userId, String username, Role role, boolean active) { }
}
