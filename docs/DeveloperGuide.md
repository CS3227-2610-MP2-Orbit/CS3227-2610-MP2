# Developer Guide

## Architecture

The project is organized by responsibility. Event, venue, attendee,
registration, notification, and volunteer packages contain domain features.
Shared concerns are separated into common, storage, service, and UI packages.

The current implementation is a Java 25 Gradle skeleton. The Venue
Administrator backend is split into domain types under `venue`, application
orchestration under `service`, and PostgreSQL schema resources under
`src/main/resources/db/migration`.

The Venue Administrator presentation layer is framework-neutral. It consists
of `VenueAdministratorDashboardController`,
`VenueAdministratorDashboardState`, and `VenueAdministratorApiClient` under
`ui`. These classes model the dashboard sections and UI states without
duplicating backend validation or authorization. A concrete JavaFX or web view
still needs to be connected to this layer.

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

Not yet implemented:

- Authentication/session implementation and production object-scope authorization
- Notification delivery worker and retry processor
- HTTP routes or a concrete API client
- Rendered JavaFX or web views
- Database-backed integration tests against PostgreSQL
- Runtime startup wiring from `Main` to a user-facing application
- Deployment configuration and production monitoring backend

Until a frontend runtime is added, `gradlew.bat run` starts the application
entry point and prints a readiness message; it does not open a dashboard.

## Acknowledgements

Reused ideas, code, documentation, libraries, and external resources will be
recorded here as the project develops.
