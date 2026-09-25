CREATE TABLE IF NOT EXISTS event_announcement (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES organizer_event (id),
    author_id VARCHAR(100) NOT NULL,
    message VARCHAR(1000) NOT NULL CHECK (length(btrim(message)) > 0),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS event_announcement_event_idx
    ON event_announcement (event_id, created_at DESC);

CREATE TABLE IF NOT EXISTS event_announcement_audit_record (
    id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    actor_id VARCHAR(100) NOT NULL,
    action VARCHAR(60) NOT NULL,
    event_id UUID NOT NULL,
    announcement_id UUID NOT NULL
);

CREATE INDEX IF NOT EXISTS event_announcement_audit_event_idx
    ON event_announcement_audit_record (event_id, occurred_at);
