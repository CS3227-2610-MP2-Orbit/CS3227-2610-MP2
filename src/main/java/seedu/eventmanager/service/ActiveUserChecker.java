package seedu.eventmanager.service;

import java.util.UUID;

/** Authorization boundary for operations performed by an already-created session. */
public interface ActiveUserChecker {
    void requireActive(UUID userId);
}
