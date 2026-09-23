# Developer Guide

## Architecture

The project is organized by responsibility. Event, venue, attendee,
registration, notification, and volunteer packages contain domain features.
Shared concerns are separated into common, storage, service, and UI packages.

The Club Organizer event workflow keeps JavaFX and JDBC behind application and
domain boundaries: `EventService` validates commands and ownership,
`EventRepository` defines persistence plus business-audit writes, and
`OrganizerEventView` contains presentation logic only. It stores events in the
organizer-specific `organizer_event` tables. Organizer venue booking requests
go through `OrganizerVenueRequestService`, which writes Jordan's
`VenueRequest` (`SUBMITTED`) via `VenueRequestRepository` so the Venue
Administrator pending queue can decide them. Organizer string identities are
mapped to UUIDs by `OrganizerIds` (transitional until shared users auth).

The Venue Administrator backend is split into domain types under `venue`,
application orchestration under `service`, and PostgreSQL schema resources
under `src/main/resources/db/migration`.

The Venue Administrator presentation layer is framework-neutral. It consists
of `VenueAdministratorDashboardController`,
`VenueAdministratorDashboardState`, and `VenueAdministratorApiClient` under
`ui`. These classes model the dashboard sections and UI states without
duplicating backend validation or authorization. The current JavaFX layer adds
a local login view, dashboard shell, venue-request review screen, and venue
management screen on top of these boundaries.

`Main` launches the shared JavaFX home screen. That screen routes to the Club
Organizer and Venue Administrator workspaces, but it deliberately does not
join their independent business workflows. The organizer schema resource lives
outside Flyway's venue migration folder so the two modules do not define the
same Flyway migration version.

The Venue Administrator bootstrap accepts `DATABASE_*` settings first and
falls back to the Organizer's `EVENT_MANAGER_DB_*` settings. This is a local
configuration compatibility layer, not shared authentication. Flyway uses
`baselineOnMigrate` at version `0` so existing Organizer tables (created
outside Flyway) do not block Venue schema migrations on a shared database.

## Team ownership

- Club Organizer: events, volunteers, and announcements
- Venue Administrator: venue requests, availability, restrictions, conflicts,
  and utilization
- Attendee: event discovery, registration, notifications, check-in, and history

## Development workflow

Run the tests before submitting changes:

```powershell
.\gradlew.bat test
```

Update documentation, tests, and logs whenever behaviour or design changes.

## Current implementation status

Implemented:

- Venue request state types and validation
- Backend Venue Administrator approval and rejection orchestration
- Role enforcement through the authorization boundary
- PostgreSQL configuration through environment variables and local `.env`
- Flyway migration bootstrap
- PostgreSQL request and booking repositories
- JDBC transaction boundary with commit and rollback handling
- Durable PostgreSQL audit logging for approval and rejection decisions
- Transactional notification outbox with idempotency protection
- Notification and authorization service boundaries
- PostgreSQL schema migration for venues, availability, requests, bookings,
  audit logs, and notification outbox
- Venue Administrator dashboard presentation state and controller
- JavaFX Venue Administrator login and dashboard shell
- JavaFX venue request review with approval and rejection actions
- JavaFX venue listing backed by PostgreSQL
- Club Organizer create/edit draft events (`EventService` / `OrganizerEventView`)
- Club Organizer submit venue requests into `venue_requests` as `SUBMITTED`
  (`OrganizerVenueRequestService`) for Admin review

Not yet implemented:

- Authentication/session implementation and production object-scope authorization
- Notification delivery worker and retry processor
- HTTP routes or a concrete API client
- Venue create, edit, activate/deactivate, and delete workflows
- Availability and schedule management UI
- Users and access management UI
- Organizer map and venue discovery UI
- Database-backed integration tests against PostgreSQL
- Deployment configuration and production monitoring backend
- Unified shared authentication (Organizer env identity vs Admin local login)
- Organizer supersede/withdraw and submit-time conflict alternatives
- Clubs CRUD UI

Until a frontend runtime is added, `gradlew.bat run` starts the application
entry point and prints a readiness message; it does not open a dashboard.
### Agentic SE workflow

Project-wide agent instructions are in [`AGENTS.md`](../AGENTS.md). Four shared
SWE skills live under `.agents/skills/`: requirements and acceptance criteria,
test-driven implementation, code review and verification, and desktop UI polish.
These apply across all three roles. See [Agentic SE](AgenticSE.md) for invocation
examples, validation scenarios, and the distinction between structural and
behavioral validation.

Record meaningful interactions under `logs/<contributor>/` using the
[interaction template](../logs/templates/interaction.md). The student personally
reviews the evidence before checking the review boxes. Existing summaries remain
available in `logs/prompts_summary.md`.

## Acknowledgements

Reused ideas, code, documentation, libraries, and external resources will be
recorded here as the project develops.

The initial SWE skill structure was developed with Codex using its bundled
`skill-creator` guidance. The interaction-log format adapts Johannsen's MP1
development-record practice; no MP1 application code was reused for this change.
