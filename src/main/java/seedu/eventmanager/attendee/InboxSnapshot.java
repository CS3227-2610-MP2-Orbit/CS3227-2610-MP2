package seedu.eventmanager.attendee;

import java.util.List;

/** List and count from the same read snapshot. */
public record InboxSnapshot(List<InboxMessage> messages) {
    public InboxSnapshot { messages = List.copyOf(messages); }
    public long unreadCount() { return messages.stream().filter(InboxMessage::unread).count(); }
}
