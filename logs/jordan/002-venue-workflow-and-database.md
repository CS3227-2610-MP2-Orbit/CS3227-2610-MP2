# 002 — Venue workflow and PostgreSQL design

Date: September 2026  
Contributor: Jordan  
Branch: `branch-venue-admin`  
Agent/tool: Codex  
Skills used:
- `.agents/skills/requirements-and-acceptance/SKILL.md` — defined the venue-request state machine and validation rules before implementation.
- Architecture inspection and database design review — reused existing event and authentication entities instead of duplicating them.
- `.agents/skills/database-migration-and-integrity/SKILL.md` — added retrospectively to capture PostgreSQL migration, constraints, and transaction concerns.

## Objective

Design the Venue Administrator request and booking workflow, then define a
normalized PostgreSQL schema before writing migrations.

## Workflow decisions

The request lifecycle uses `DRAFT`, `SUBMITTED`, `INVALID`, `APPROVED`,
`REJECTED`, `CANCELLED`, and `WITHDRAWN`. Confirmed bookings use
`CONFIRMED`, `AT_RISK`, `CANCELLED`, and `COMPLETED`.

Approval requires validation, capacity checks, venue availability, restriction
checks, and conflict detection. Rejection requires a reason. State transitions
record the acting administrator, timestamp, reason, notification, and audit
event.

## Database decisions

The PostgreSQL design introduced normalized tables for:

- `venues`
- `venue_availability`
- `venue_requests`
- `venue_bookings`
- `audit_logs`

Booking intervals use timezone-aware timestamps and half-open interval
semantics. PostgreSQL GiST exclusion constraints prevent overlapping active
bookings, while a partial unique index prevents multiple active bookings for
one event.

## Verification and limitations

The schema was reviewed for foreign keys, interval checks, approval metadata,
indexes, and transactional conflict prevention. Concrete repositories and
database runtime wiring were implemented incrementally afterward.
