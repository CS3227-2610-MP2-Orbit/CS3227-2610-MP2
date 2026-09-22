CREATE TABLE IF NOT EXISTS organizer_event (
    id UUID PRIMARY KEY,
    club_id VARCHAR(100) NOT NULL,
    organizer_id VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL CHECK (version >= 0),
    CHECK (starts_at < ends_at)
);

CREATE INDEX IF NOT EXISTS organizer_event_club_start_idx
    ON organizer_event (club_id, starts_at);

CREATE TABLE IF NOT EXISTS organizer_event_audit_record (
    id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    actor_id VARCHAR(100) NOT NULL,
    action VARCHAR(60) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id UUID NOT NULL,
    resulting_version BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS organizer_event_audit_entity_idx
    ON organizer_event_audit_record (entity_type, entity_id, occurred_at);
