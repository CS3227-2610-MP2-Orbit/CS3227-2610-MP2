CREATE TABLE IF NOT EXISTS event_volunteer (
    event_id UUID NOT NULL REFERENCES organizer_event (id),
    attendee_id UUID NOT NULL,
    role VARCHAR(60) NOT NULL,
    assigned_by VARCHAR(100) NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (event_id, attendee_id)
);

CREATE TABLE IF NOT EXISTS event_volunteer_audit_record (
    id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    actor_id VARCHAR(100) NOT NULL,
    action VARCHAR(60) NOT NULL,
    event_id UUID NOT NULL,
    attendee_id UUID NOT NULL
);

CREATE INDEX IF NOT EXISTS event_volunteer_audit_event_idx
    ON event_volunteer_audit_record (event_id, occurred_at);
