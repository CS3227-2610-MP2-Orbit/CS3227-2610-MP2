CREATE TABLE IF NOT EXISTS notification_outbox (
    notification_id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    recipient_id UUID NOT NULL,
    event VARCHAR(80) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL,
    last_error TEXT,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (status <> 'SENT' OR sent_at IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_delivery
    ON notification_outbox (status, next_attempt_at);
