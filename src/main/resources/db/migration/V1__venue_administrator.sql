CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE IF NOT EXISTS venues (
    venue_id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    location VARCHAR(255) NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    description TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (name, location)
);

CREATE TABLE IF NOT EXISTS venue_availability (
    availability_id UUID PRIMARY KEY,
    venue_id UUID NOT NULL REFERENCES venues(venue_id),
    availability_type VARCHAR(20) NOT NULL CHECK (availability_type IN ('OPEN', 'BLOCKED')),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    reason VARCHAR(255),
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL,
    CHECK (ends_at > starts_at),
    CHECK (availability_type = 'OPEN' OR reason IS NOT NULL)
);

CREATE TABLE IF NOT EXISTS venue_requests (
    request_id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    venue_id UUID NOT NULL REFERENCES venues(venue_id),
    organizer_id UUID NOT NULL,
    requested_starts_at TIMESTAMPTZ NOT NULL,
    requested_ends_at TIMESTAMPTZ NOT NULL,
    expected_attendance INTEGER NOT NULL CHECK (expected_attendance > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'SUBMITTED', 'INVALID', 'APPROVED', 'REJECTED', 'CANCELLED', 'WITHDRAWN')),
    submitted_at TIMESTAMPTZ,
    decided_at TIMESTAMPTZ,
    decided_by UUID,
    decision_reason VARCHAR(50),
    decision_comment TEXT,
    supersedes_request_id UUID REFERENCES venue_requests(request_id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (requested_ends_at > requested_starts_at),
    CHECK (status = 'DRAFT' OR submitted_at IS NOT NULL),
    CHECK (status NOT IN ('APPROVED', 'REJECTED') OR (decided_at IS NOT NULL AND decided_by IS NOT NULL)),
    CHECK (status <> 'REJECTED' OR decision_reason IS NOT NULL)
);

CREATE TABLE IF NOT EXISTS venue_bookings (
    booking_id UUID PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE REFERENCES venue_requests(request_id),
    event_id UUID NOT NULL,
    venue_id UUID NOT NULL REFERENCES venues(venue_id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('CONFIRMED', 'AT_RISK', 'CANCELLED', 'COMPLETED')),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ NOT NULL,
    cancelled_at TIMESTAMPTZ,
    cancelled_by UUID,
    cancellation_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (ends_at > starts_at),
    CHECK (status <> 'CANCELLED' OR (cancelled_at IS NOT NULL AND cancelled_by IS NOT NULL AND cancellation_reason IS NOT NULL))
);

ALTER TABLE venue_bookings
    ADD CONSTRAINT venue_bookings_no_overlap
    EXCLUDE USING gist (
        venue_id WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&
    ) WHERE (status IN ('CONFIRMED', 'AT_RISK'));

CREATE INDEX IF NOT EXISTS idx_venue_requests_review
    ON venue_requests (status, submitted_at);
CREATE INDEX IF NOT EXISTS idx_venue_requests_venue_interval
    ON venue_requests (venue_id, status, requested_starts_at, requested_ends_at);
CREATE INDEX IF NOT EXISTS idx_venue_availability_interval
    ON venue_availability (venue_id, starts_at, ends_at);
CREATE INDEX IF NOT EXISTS idx_venue_bookings_calendar
    ON venue_bookings (venue_id, status, starts_at, ends_at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_one_active_booking_per_event
    ON venue_bookings (event_id)
    WHERE status IN ('CONFIRMED', 'AT_RISK');

CREATE TABLE IF NOT EXISTS audit_logs (
    audit_log_id UUID PRIMARY KEY,
    actor_id UUID,
    actor_role VARCHAR(40) NOT NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    previous_state VARCHAR(30),
    new_state VARCHAR(30),
    reason_code VARCHAR(50),
    details JSONB,
    correlation_id UUID,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_entity_time
    ON audit_logs (entity_type, entity_id, created_at);
