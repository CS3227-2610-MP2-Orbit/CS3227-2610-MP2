CREATE TABLE attendee_notification_inbox (
    notification_id UUID PRIMARY KEY REFERENCES notification_outbox(notification_id),
    recipient_id UUID NOT NULL REFERENCES users(user_id),
    event_type VARCHAR(80) NOT NULL CHECK
        (event_type IN ('REGISTRATION_CONFIRMED', 'REGISTRATION_CANCELLED', 'EVENT_ANNOUNCEMENT')),
    event_id UUID NOT NULL,
    registration_id UUID,
    registration_version BIGINT,
    announcement_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ,
    CHECK (
        (event_type = 'EVENT_ANNOUNCEMENT' AND announcement_id IS NOT NULL
            AND registration_id IS NULL AND registration_version IS NULL)
        OR (event_type IN ('REGISTRATION_CONFIRMED', 'REGISTRATION_CANCELLED')
            AND registration_id IS NOT NULL AND registration_version >= 0
            AND registration_version IS NOT NULL AND announcement_id IS NULL)
    )
);

-- Source IDs deliberately have no announcement/event FK: removal must not erase an inbox entry.
-- The notification PK gives one durable entry per outbox event, including retry after markSent failure.
CREATE INDEX attendee_inbox_owner_order_idx
    ON attendee_notification_inbox(recipient_id, created_at DESC, notification_id DESC);
