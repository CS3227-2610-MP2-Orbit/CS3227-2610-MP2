package seedu.eventmanager.ui;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import seedu.eventmanager.attendee.InboxSnapshot;

/** Callbacks bound to the live login session in application composition. */
public record InboxActions(Supplier<InboxSnapshot> list, Consumer<UUID> markRead, Runnable markAllRead) {
    public InboxActions {
        Objects.requireNonNull(list); Objects.requireNonNull(markRead); Objects.requireNonNull(markAllRead);
    }
}
