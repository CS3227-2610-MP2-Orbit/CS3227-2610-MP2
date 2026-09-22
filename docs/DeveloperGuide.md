# Developer Guide

## Architecture

The project is organized by responsibility. Event, venue, attendee,
registration, notification, and volunteer packages contain domain features.
Shared concerns are separated into common, storage, service, and UI packages.

The event workflow keeps JavaFX and JDBC behind application/domain boundaries:

- `EventService` validates commands and enforces club ownership.
- `EventRepository` defines persistence plus atomic business-audit writes.
- `JdbcEventRepository` stores events and audit records in one PostgreSQL
  transaction and uses the event version for optimistic concurrency control.
- `OrganizerEventView` is the JavaFX create/edit screen and does not contain
  authorization or persistence rules.
- `SingaporeDateTimes` converts date-picker and `HH:mm` values in
  `Asia/Singapore` to UTC `Instant` values at the UI boundary. Domain and
  PostgreSQL timestamps remain time-zone-neutral instants.

PostgreSQL connection settings come from `EVENT_MANAGER_DB_URL`,
`EVENT_MANAGER_DB_USER`, and `EVENT_MANAGER_DB_PASSWORD`. The temporary
development identity comes from `EVENT_MANAGER_ORGANIZER_ID` and
`EVENT_MANAGER_CLUB_IDS`; shared authentication must replace this adapter.

## Team ownership

- Club Organizer: events, volunteers, and announcements
- Venue Administrator: venue requests, availability, restrictions, conflicts,
  and utilization
- Attendee: event discovery, registration, notifications, check-in, and history

## Development workflow

Run the tests before submitting changes:

```sh
./gradlew test
```

On Windows:

```powershell
.\gradlew.bat test
```

The PostgreSQL repository integration test runs when an isolated test database
is supplied through `EVENT_MANAGER_TEST_DB_URL` and, if needed,
`EVENT_MANAGER_TEST_DB_USER` and `EVENT_MANAGER_TEST_DB_PASSWORD`. The test
truncates the organizer-event and organizer-event-audit tables, so never point it
at a shared or production database.

Update documentation, tests, and logs whenever behaviour or design changes.

### Agentic SE workflow

Project-wide agent instructions are in [`AGENTS.md`](../AGENTS.md). Three shared
SWE skills live under `.agents/skills/`: requirements and acceptance criteria,
test-driven implementation, and code review and verification. These apply across
all three roles. See [Agentic SE](AgenticSE.md) for invocation examples, validation
scenarios, and the distinction between structural and behavioral validation.

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
