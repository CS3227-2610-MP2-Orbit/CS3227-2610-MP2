CREATE TABLE IF NOT EXISTS users (
    user_id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role VARCHAR(40) NOT NULL CHECK (role IN ('CLUB_ORGANIZER', 'VENUE_ADMINISTRATOR', 'ATTENDEE')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS venue_administrator_venues (
    user_id UUID NOT NULL REFERENCES users(user_id),
    venue_id UUID NOT NULL REFERENCES venues(venue_id),
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, venue_id)
);

CREATE TABLE IF NOT EXISTS user_sessions (
    session_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_sessions_active
    ON user_sessions (token_hash, expires_at)
    WHERE revoked_at IS NULL;
