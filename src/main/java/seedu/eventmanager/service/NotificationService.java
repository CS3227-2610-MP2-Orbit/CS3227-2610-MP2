package seedu.eventmanager.service;

import java.util.Map;
import java.util.UUID;

public interface NotificationService {
    void notify(UUID recipientId, String event, Map<String, String> data);
}
