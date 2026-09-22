ALTER TABLE notification_outbox
    DROP CONSTRAINT IF EXISTS notification_outbox_status_check;

ALTER TABLE notification_outbox
    ADD CONSTRAINT notification_outbox_status_check
    CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED'));

CREATE INDEX IF NOT EXISTS idx_notification_outbox_processing
    ON notification_outbox (status, next_attempt_at);
