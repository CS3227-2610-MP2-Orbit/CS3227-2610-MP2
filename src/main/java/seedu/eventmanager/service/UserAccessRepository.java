package seedu.eventmanager.service;

import java.util.List;
import java.util.UUID;
import seedu.eventmanager.common.Role;

public interface UserAccessRepository {
    List<UserSummary> findAllUsers();
    void createUser(String username, String password, Role role);
    void grantVenueAccess(UUID userId, UUID venueId);

    record UserSummary(UUID userId, String username, Role role, boolean active) { }
}
