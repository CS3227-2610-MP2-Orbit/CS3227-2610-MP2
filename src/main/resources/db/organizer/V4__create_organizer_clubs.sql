CREATE TABLE IF NOT EXISTS organizer_club (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL CHECK (length(btrim(name)) > 0),
    owner_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS organizer_club_name_ci_idx
    ON organizer_club (lower(name));

CREATE INDEX IF NOT EXISTS organizer_club_owner_idx
    ON organizer_club (owner_id);

CREATE TABLE IF NOT EXISTS organizer_club_audit_record (
    id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    actor_id UUID NOT NULL,
    action VARCHAR(60) NOT NULL,
    club_id UUID NOT NULL
);

CREATE INDEX IF NOT EXISTS organizer_club_audit_club_idx
    ON organizer_club_audit_record (club_id, occurred_at);
