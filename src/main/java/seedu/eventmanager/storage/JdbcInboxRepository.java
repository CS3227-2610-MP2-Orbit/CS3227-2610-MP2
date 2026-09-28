package seedu.eventmanager.storage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Objects;
import java.sql.SQLException;
import java.sql.Timestamp;
import seedu.eventmanager.attendee.InboxRepository;
import seedu.eventmanager.attendee.InboxNotificationDelivery;
import seedu.eventmanager.common.ApplicationException;
import seedu.eventmanager.notification.NotificationOutboxRepository.OutboxEvent;

/** Durable inbox delivery and owner-scoped queries on canonical event/announcement data. */
public final class JdbcInboxRepository implements InboxRepository {
    private final JdbcDatabase database;
    public JdbcInboxRepository(JdbcDatabase database) { this.database = Objects.requireNonNull(database); }

    public void deliver(OutboxEvent event) {
        if (!InboxNotificationDelivery.EVENT_TYPES.contains(event.event())) {
            throw new ApplicationException("UNSUPPORTED_NOTIFICATION", "Unsupported attendee notification type.");
        }
        database.inTransaction(c -> {
            // Resolve the canonical persisted payload, not arbitrary text supplied to an adapter.
            try (var p = c.prepareStatement("""
                    SELECT n.payload->>'registrationId' AS registration_id,n.payload->>'version' AS version,
                           n.payload->>'announcementId' AS announcement_id,n.payload->>'eventId' AS event_id,n.created_at
                    FROM notification_outbox n JOIN users u ON u.user_id=n.recipient_id AND u.role='ATTENDEE'
                    WHERE n.notification_id=? AND n.recipient_id=? AND n.event=?
                    """)) {
                p.setObject(1, event.notificationId()); p.setObject(2, event.recipientId());
                p.setString(3, event.event()); p.setQueryTimeout(15);
                try (var row = p.executeQuery()) {
                    if (!row.next()) throw invalid();
                    UUID registration = null;
                    UUID announcement = null;
                    UUID eventId;
                    Long version = null;
                    if ("EVENT_ANNOUNCEMENT".equals(event.event())) {
                        announcement = uuid(row.getString("announcement_id"));
                        eventId = uuid(row.getString("event_id"));
                    } else {
                        registration = uuid(row.getString("registration_id"));
                        version = version(row.getString("version"));
                        try (var lookup = c.prepareStatement(
                                "SELECT event_id FROM event_registrations WHERE registration_id=? AND attendee_id=?")) {
                            lookup.setObject(1, registration); lookup.setObject(2, event.recipientId());
                            lookup.setQueryTimeout(15);
                            try (var registered = lookup.executeQuery()) {
                                if (!registered.next()) throw invalid();
                                eventId = registered.getObject(1, UUID.class);
                            }
                        }
                    }
                    try (var insert = c.prepareStatement("""
                            INSERT INTO attendee_notification_inbox
                            (notification_id,recipient_id,event_type,event_id,registration_id,registration_version,
                             announcement_id,created_at)
                            VALUES (?,?,?,?,?,?,?,?) ON CONFLICT (notification_id) DO NOTHING
                            """)) {
                        insert.setObject(1, event.notificationId()); insert.setObject(2, event.recipientId());
                        insert.setString(3, event.event()); insert.setObject(4, eventId);
                        insert.setObject(5, registration); insert.setObject(6, version); insert.setObject(7, announcement);
                        insert.setTimestamp(8, row.getTimestamp("created_at")); insert.setQueryTimeout(15);
                        insert.executeUpdate();
                    }
                }
            } catch (SQLException failure) { throw new IllegalStateException("Unable to persist inbox notification.", failure); }
            return null;
        });
    }

    @Override public List<Entry> list(UUID owner) {
        return database.withConnection(c -> {
            try (var p = c.prepareStatement("""
                    SELECT i.notification_id,i.created_at,i.read_at,i.event_type,e.title,e.starts_at,a.message
                    FROM attendee_notification_inbox i
                    LEFT JOIN organizer_event e ON e.id=i.event_id
                    LEFT JOIN event_announcement a ON a.id=i.announcement_id AND a.event_id=i.event_id
                    WHERE i.recipient_id=? ORDER BY i.created_at DESC,i.notification_id DESC
                    """)) {
                p.setObject(1, owner); p.setQueryTimeout(15);
                try (var rows = p.executeQuery()) {
                    List<Entry> result = new ArrayList<>();
                    while (rows.next()) result.add(new Entry(rows.getObject("notification_id", UUID.class),
                            rows.getTimestamp("created_at").toInstant(), instant(rows.getTimestamp("read_at")),
                            rows.getString("event_type"), rows.getString("title"),
                            instant(rows.getTimestamp("starts_at")), rows.getString("message")));
                    return List.copyOf(result);
                }
            } catch (SQLException failure) { throw new IllegalStateException("Unable to read inbox.", failure); }
        });
    }

    @Override public boolean markRead(UUID owner, UUID id, Instant now) {
        return database.withConnection(c -> {
            try (var p = c.prepareStatement("""
                    UPDATE attendee_notification_inbox SET read_at=COALESCE(read_at,?)
                    WHERE recipient_id=? AND notification_id=?
                    """)) {
                p.setTimestamp(1, Timestamp.from(now)); p.setObject(2, owner); p.setObject(3, id); p.setQueryTimeout(15);
                return p.executeUpdate() == 1;
            } catch (SQLException failure) { throw new IllegalStateException("Unable to mark notification read.", failure); }
        });
    }

    @Override public void markAllRead(UUID owner, Instant now) {
        database.withConnection(c -> {
            try (var p = c.prepareStatement("""
                    UPDATE attendee_notification_inbox SET read_at=? WHERE recipient_id=? AND read_at IS NULL
                    """)) {
                p.setTimestamp(1, Timestamp.from(now)); p.setObject(2, owner); p.setQueryTimeout(15);
                p.executeUpdate(); return null;
            } catch (SQLException failure) { throw new IllegalStateException("Unable to mark inbox read.", failure); }
        });
    }

    private static Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
    private static UUID uuid(String value) {
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException | NullPointerException failure) { throw invalid(); }
    }
    private static long version(String value) {
        try {
            long result = Long.parseLong(value);
            if (result < 0) throw invalid();
            return result;
        } catch (NumberFormatException failure) { throw invalid(); }
    }
    private static ApplicationException invalid() {
        return new ApplicationException("INVALID_NOTIFICATION", "Notification recipient or payload is invalid.");
    }
}
