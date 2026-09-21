package seedu.eventmanager.service;

import seedu.eventmanager.common.Actor;
import java.util.UUID;

public interface AuditLogService {
    void record(Actor actor, String action, String entityType, UUID entityId,
            String previousState, String newState, String reason);
}
