CREATE TABLE event_registrations (
    registration_id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES organizer_event(id),
    attendee_id UUID NOT NULL REFERENCES users(user_id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('CONFIRMED', 'CANCELLED', 'CHECKED_IN')),
    registered_at TIMESTAMPTZ NOT NULL,
    cancelled_at TIMESTAMPTZ,
    checked_in_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    UNIQUE (event_id, attendee_id),
    CHECK ((status = 'CANCELLED') = (cancelled_at IS NOT NULL)),
    CHECK ((status = 'CHECKED_IN') = (checked_in_at IS NOT NULL))
);

CREATE INDEX event_registrations_owner_idx ON event_registrations (attendee_id, registered_at);
