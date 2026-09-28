# Event Venue Manager Developer Guide

---

## Acknowledgements

Event Venue Manager is a CS3227 MP2 team project (Club Organizer, Venue Administrator, Attendee).

Libraries and resources used include:

1. [JavaFX](https://openjfx.io/)
2. [JUnit 5](https://junit.org/junit5/)
3. [PostgreSQL](https://www.postgresql.org/) / JDBC
4. [Flyway](https://flywaydb.org/)
5. [dotenv-java](https://github.com/cdimascio/dotenv-java) (local `.env` loading)
6. Gradle Wrapper for builds

Agentic SE process notes and skills live under [`AGENTS.md`](../AGENTS.md) and [Agentic SE](AgenticSE.md). Interaction-log format adapts Johannsen’s MP1 development-record practice; **no MP1 application code** was reused.

The Attendee catalogue was developed with Codex using the repository's TDD,
review and desktop UI skills. The bundled PostgreSQL best-practices skill informed
the query/index review; no additional database library or external SQL sample was
copied into the implementation.

---

## Setting up, getting started

1. Install **JDK 25** and a local **PostgreSQL** instance.
2. Clone the repository and create a project-root `.env` (see [User Guide — Getting Started](UserGuide.md#getting-started)).
3. From the project root:

   ```sh
   ./gradlew test
   ./gradlew run
   ```

4. Prefer feature branches; keep role-specific UI separate while sharing persistence and auth boundaries.

> **Note:** Club Organizer and Venue Administrator share one database when `DATABASE_*` / `EVENT_MANAGER_DB_*` point at the same URL. That is configuration compatibility, not unified authentication.

---

## Design

### Architecture

```mermaid
flowchart TB
  Main[Main / EventManagerApplication]
  OrgUI[OrganizerEventView]
  AdminUI[VenueAdministratorFxApplication]
  OrgSvc[EventService / OrganizerVenueRequestService]
  AdminSvc[VenueAdministratorService]
  Repos[EventRepository / VenueRequestRepository / VenueRepository]
  DB[(PostgreSQL)]

  Main --> OrgUI
  Main --> AdminUI
  OrgUI --> OrgSvc
  AdminUI --> AdminSvc
  OrgSvc --> Repos
  AdminSvc --> Repos
  Repos --> DB
```

**Main components**

| Component | Responsibility |
| --- | --- |
| **UI** | JavaFX role workspaces (`ui` package). Presentation only; no business-rule ownership. |
| **Application / service** | Workflows such as `EventService`, `OrganizerVenueRequestService`, `VenueAdministratorService`. |
| **Domain** | Types under `event`, `venue`, `common` (e.g. `Event`, `VenueRequest`, `Actor`). |
| **Storage** | JDBC repositories, Flyway migrations, organizer SQL migrations, `.env` bootstrap. |

**How components interact (venue request happy path)**

```mermaid
sequenceDiagram
  participant Org as OrganizerEventView
  participant OVS as OrganizerVenueRequestService
  participant ES as EventService
  participant VR as VenueRequestRepository
  participant Admin as VenueRequestManagementView
  participant VAS as VenueAdministratorService

  Org->>OVS: submit(actor, eventId, venueId)
  OVS->>ES: getEvent(actor, eventId)
  OVS->>VR: save(SUBMITTED VenueRequest)
  Admin->>VR: findSubmitted()
  Admin->>VAS: approve(actor, requestId)
  VAS->>VR: save(APPROVED) + booking
```

### UI component

**Location:** `seedu.eventmanager.ui`

* `EventManagerApplication` — home screen and role routing.
* `OrganizerEventView` — create/edit events and **Request venue**.
* `VenueAdministratorFxApplication` — login, dashboard, venues, requests, users/access.
* `VenueAdministratorDashboardController` + `VenueAdministratorApiClient` — presentation state without duplicating Admin business rules.

The Organizer and Admin shells share visual tokens (sidebar `#172033`, page `#f7f9fc`, primary actions `#2563eb`).

### Application / logic component

**Organizer**

* `ClubService` — create/list clubs for the signed-in `CLUB_ORGANIZER` account (single owner, names unique ignoring case) and build its `OrganizerIdentity` from the clubs it owns in `organizer_club`.
* `EventService` — create/list/get/edit draft events; club ownership checks; optimistic versioning.
* `OrganizerVenueRequestService` — builds Jordan’s `VenueRequest` as `SUBMITTED` (UTC window, attendance = capacity).
* `OrganizerIds` — transitional `String` → `UUID` mapping for `organizer_id`.

**Venue Administrator**

* `VenueAdministratorService` — approve/reject with authorization, conflict check on approve, audit + notification outbox.
* `AuthorizationService` / `JdbcAuthorizationService` — role + `venue_administrator_venues` scope.

### Model / domain component

* `event.Event`, `EventDetails`, `EventStatus`, `OrganizerIdentity`
* `venue.Venue`, `VenueRequest`, `VenueRequestStatus`, `VenueRequestValidator`
* `common.Actor`, `Role`, shared exceptions

Domain types do not depend on JavaFX.

### Storage component

* Organizer tables via `DatabaseMigration` / `db/organizer` resources (outside Flyway version stream).
* Venue Admin schema via Flyway under `src/main/resources/db/migration` (`baselineOnMigrate` at `0`).
* JDBC repositories: `JdbcEventRepository`, `JdbcVenueRequestRepository`, `JdbcVenueRepository`, etc.
* Config: `DatabaseBootstrap` reads `DATABASE_*` then falls back to `EVENT_MANAGER_DB_*`.

### Common classes

Shared utilities and cross-cutting types live under `seedu.eventmanager.common` (exceptions, roles, actors, logging helpers).

---

## Implementation

### Create and edit draft events

**Problem:** Organizers need to persist draft events with ownership and validation.

**Approach:**

1. UI collects form fields → `EventFormParser` → `EventDetails`.
2. `EventService.createEvent` / `editEvent` validates title, interval, capacity, and club ownership.
3. `EventRepository` persists event + business audit in one transaction.

**Key classes:** `EventService`, `JdbcEventRepository`, `OrganizerEventView`.

### Request venues (Organizer → Admin pipeline)

**Problem:** Organizer events must enter the Admin approve/reject queue.

**Approach:**

1. `OrganizerVenueRequestService.submit` loads the owned event, requires an ACTIVE venue, rejects duplicate open requests.
2. Persists `VenueRequest` with `status = SUBMITTED` through `VenueRequestRepository`.
3. Admin UI loads pending rows via `findSubmitted()`; `VenueAdministratorService.approve` / `reject` decides them.
4. Creating a venue (or **Claim access**) grants `venue_administrator_venues` so Approve is authorized.
5. Navigating to **Venue requests** calls `controller.load()` so the list is not stale.

**Key classes:** `OrganizerVenueRequestService`, `OrganizerIds`, `JdbcVenueRequestRepository`, `VenueAdministratorService`, `VenueAdministratorFxApplication`, `VenueManagementView`.

**Design notes / merge debt:**

* Organizer identity remains env-based until shared users auth.
* Conflict detection remains on Admin approve (not on Organizer submit).
* No supersede/withdraw in v1.

### Attendee catalogue and personalized event details

`EventCatalogueService` exposes a public read-only projection of the canonical
Organizer events. Its `EventCatalogueRepository` boundary has a JDBC adapter in
`storage`; it does not bypass organizer ownership checks for writes or introduce
an attendee event table. SQL restricts reads to published events; the service
also enforces future-start visibility for both listing and direct-ID details.
`CatalogueEvent` omits organizer identity and internal version/state fields.

`CatalogueQuery` combines literal, case-insensitive title/description search,
exact club ID and inclusive Singapore-calendar start dates. An injected clock
defines "upcoming". `AttendeeBrowseController` uses cancellable JavaFX Tasks on virtual
threads and ignores superseded results. Database/configuration work is off the
UI thread; Home/app shutdown cancels pending reads. Failures use safe messages and
structured failure-type logging, not raw JDBC messages or business audit writes.

`AttendeeEventDetailsService` authenticates a live attendee session before and
after a read, derives the owner ID internally and returns an immutable private
projection separate from `CatalogueEvent`. The JDBC adapter reads canonical
event, current booking, venue, occupied seats and only the caller's registration
in one statement snapshot, with no write locks. Unique booking/registration keys
and a separate aggregate prevent join multiplication. A service clock rechecks
visibility after the read; query timeout is 15 seconds.

`RegistrationEligibilityPolicy`, `AttendeeSessionGuard` and package-private
`RegistrationReadSql` share rules with commands without making previews execute
registration or acquire command locks. Occupancy includes CONFIRMED/CHECKED_IN
records even for inactive users; Joseph's roster is not a capacity counter.
Remaining seats are clamped at zero for display. Eligibility is advisory and
separate from own status; existing command locks, version checks, post-lock time
validation and transactional audit/outbox remain authoritative.

The Attendee route lazily runs `InboxDatabaseMigration`, which first delegates to
the existing `RegistrationDatabaseMigration` bootstrap, in background tasks,
independently of Organizer navigation. Successful service initialization is reused
and failure can be retried. No fixture insertion or publication operation is added. `EventService` still cannot publish
events, so new drafts do not appear. Register/cancel UI uses existing session-token commands.
See the [Attendee User Guide](UserGuide.md#attendee-browse-and-search-events)
for the implemented workflow and current limitations.

Focused verification:

```sh
./gradlew test --tests 'seedu.eventmanager.attendee.*'
./gradlew test --tests 'seedu.eventmanager.registration.*'
./gradlew attendeeUiSmoke
./gradlew attendeeRegistrationUiSmoke
./gradlew attendeeInboxUiSmoke
```

The PostgreSQL catalogue/detail/registration tests require `EVENT_MANAGER_TEST_DB_URL`,
`EVENT_MANAGER_TEST_DB_USER` and optionally `EVENT_MANAGER_TEST_DB_PASSWORD`.
Use a disposable test database, never the application database. Each new catalogue
test creates and drops its own randomized schema; other existing integration
tests may truncate their test tables. CI explicitly supplies the database settings
for the Attendee test step. Without them, these database tests are skipped, not
verified. Details integration tests live in the registration test package to reuse
its isolated-schema fixtures; the CI registration step includes them. Catalogue
and detail/policy service tests also run without PostgreSQL.

`attendeeUiSmoke` is opt-in and requires a graphical desktop. It opens the actual
JavaFX browse view with synthetic repositories, checks venue/seats/own status,
refresh, full state, expired session, delayed stale responses, clearing on Home,
search/empty/validation/error/retry behavior and saves snapshots under
`build/attendee-smoke/`. This is real UI interaction with fixture data, not
database-connected or cross-role E2E coverage. It is not run in headless CI.

### Attendee persistent notifications inbox (#30)

`InboxNotificationDelivery` adapts the existing `NotificationDelivery` contract to
`JdbcInboxRepository`. It accepts only `REGISTRATION_CONFIRMED`,
`REGISTRATION_CANCELLED` and `EVENT_ANNOUNCEMENT`; an explicitly routed unsupported
type fails with `UNSUPPORTED_NOTIFICATION`. Payload/recipient failures use a safe
`INVALID_NOTIFICATION` message, without exposing the payload or SQL exception.

The Attendee-owned Flyway stream `db/attendee_inbox` uses
`attendee_inbox_schema_history`. Its V1 creates `attendee_notification_inbox` and
an owner/newest-first index; no applied migration is edited. `notification_id` is
the primary key and references the outbox. Delivery uses `ON CONFLICT DO NOTHING`,
so retrying after a lost `markSent` response cannot duplicate an entry or reset
read status. Outbox cleanup must respect this FK; no cleanup job is added here.

The adapter resolves IDs from the canonical outbox row and verifies the recipient
has the ATTENDEE role. Registration references must belong to that recipient.
Deactivated attendees can still receive durable entries, but cannot read/mark
them until they have an active valid session again. The inbox stores IDs, original
notification type/time and read time, not copied message bodies. Event and
announcement IDs deliberately have no deletion-cascading FK. A single left-joined
read resolves current titles/start times and announcement text; removed sources
show neutral fallbacks. Registration notification kind remains historical even
if the registration has since changed. This is a notification inbox, not an audit
snapshot or proof of current registration status.

`InboxService` derives the owner from the live token through the existing
`AttendeeSessionGuard`, checks again after reads/before committing marks, and
never accepts an attendee ID. Mark-one filters by owner and notification ID;
unknown and other-owned IDs return the same safe error. Mark-all affects only
that owner. Marking is transactional and repeatable without changing an existing
read timestamp. `InboxSnapshot` derives the unread count from the same immutable
message list, avoiding inconsistent count/list queries.

The application starts one daemon `InboxDispatcher` after successful background
Attendee initialization. It runs the existing outbox worker in batches of up to
25 every two seconds, continues across Home navigation, and shuts down with the
app. `JdbcNotificationOutboxRepository` has an optional event-type scope: this
worker claims only the three supported attendee types. Its original constructor
still claims all types. Venue notifications are left untouched for their own
delivery route, not marked sent or silently discarded by the inbox adapter.
The existing claim lease/retry behavior is unchanged: five-minute claim expiry
also delays a failed attempt; the fifth failed attempt becomes FAILED.

`InboxActions` binds the session once at composition; `InboxController` executes
reads/marks off the FX thread, prevents duplicate marks and ignores stale/closed
callbacks. `InboxView` lives in the existing sidebar shell. Badge refresh happens
at initialization, opening Notifications, manual refresh and after marks—not via
UI polling. Its All/Unread/Read filter operates only on the loaded owner snapshot,
preserves newest-first order and remains selected through refresh/mark operations.
The unread count and mark-all command always cover the whole owner inbox; filtering
does not change service authorization or mark scope. Loading/failure discards the
cached snapshot and disables filtering, so it cannot restore stale personal rows.
Loading/failure displays an unknown count, rather than stale personal
data or a false zero. No email, push delivery, check-in, producer rewrite or
teammate business-rule changes are included. Announcement enqueue remains the
existing best-effort operation outside its save transaction; this feature cannot
recover announcements that were never queued.

`InboxIntegrationTest` uses isolated PostgreSQL schemas and real sessions/outbox
delivery to cover persistence, retry/concurrent idempotence, ownership, invalid
sessions, source changes/deletion, payload rejection, type-scoped routing and
transaction rollback. Its late-revocation case injects a session resolver; it is
not proof of clock-driven expiry during a database transaction. Unit tests cover
the service/adapter and dispatcher lifecycle. `attendeeInboxUiSmoke` opens actual
JavaFX controls with synthetic callbacks, exercises read/unread, badge, duplicate
guard, errors, expired sessions, stale reads and Home, and saves 1280/1000-wide
snapshots under `build/attendee-smoke/`. It is opt-in on a graphical desktop, not
a real-login/database-connected end-to-end test.

### Attendee registration commands and My Registrations (#29)

The composition root binds `RegistrationService.register`, `cancel` and
`myRegistrations` to the current session token. Views never supply an attendee ID.
`AttendeeEventDetails` includes the own record's version (including CANCELLED),
or -1 only when absent. Controls submit that displayed version unchanged.
`AttendeeBrowseController` shares one in-flight command gate across both screens,
runs callbacks on virtual threads, ignores stale read results and refreshes the
active screen after every command outcome. Navigation is disabled during writes;
app shutdown detaches UI callbacks without pretending to roll back a database commit.
`RegistrationFeedback` allowlists every existing service rejection code and treats
unknown failures as uncertain outcomes, without exposing raw exceptions.

`MyRegistrationsService` enriches `RegistrationService.myRegistrations` in one
batch metadata query; it does not filter through the future-only public catalogue.
It revalidates the session after enrichment and rejects non-owner records.
`JdbcRegistrationEventInfoRepository` uses a bound UUID array and a lateral join
from `RegistrationReadSql` that selects at most one venue booking per event:
current CONFIRMED/AT_RISK first, otherwise latest `confirmed_at` with booking ID
as tie-breaker. Thus completed/cancelled historic bookings remain displayable.
This is current event metadata, not a historical registration-time snapshot.
The same owner projection carries end time, club, description and event status
for the My Registrations detail pane, without broadening public-catalogue visibility.
`RegistrationListQuery` is a pure presentation filter/sort over these authorized
snapshots, with explicit current-time input: upcoming before start, ongoing
start-inclusive/end-exclusive, past from end, and cancelled in its own bucket
(also included in All). Earliest/latest start order uses event ID to break ties.
`MyRegistrationsView` uses the Browse-style horizontal split: bookings on the left,
scrollable event details on the right, updated on selection. Changing filters,
refreshing or failing a read clears the old details. Filter/sort choices persist
across refresh; time groups are recomputed on filter/sort/refresh, not on a timer.
The command service remains authoritative for ownership, time, capacity, versions,
and atomic state/audit/outbox writes. No tables, migrations or new dependencies.

`MyRegistrationsIntegrationTest` verifies real PostgreSQL owner-only enrichment,
past/cancelled visibility, historic/current venue selection and a real
register→cancel→re-register workflow with one row and three audit/outbox effects.
`attendeeRegistrationUiSmoke` checks actual JavaFX controls with synthetic callbacks:
exact submitted versions, double-click suppression, background execution, both
cancel entry points, rejection feedback, refresh, expiry, empty states and layouts
at 1280/1000px. It is not login-to-database E2E and requires a graphical desktop.

Historical verification before this main sync: on the inspected local SGT environment, the two
existing `PostgreSqlVenueAdministratorIntegrationTest` cases fail because record
equality distinguishes `+08:00` from equivalent UTC offsets returned by JDBC.
At that revision, the full suite passed with `JAVA_TOOL_OPTIONS=-Duser.timezone=UTC`; this is a
diagnostic environment setting, not a fix for those tests or a global app change.
The catalogue's Instant/SGT conversion tests pass in the normal local environment.
The initial catalogue fetches upcoming published events then filters text/club/date
in memory. Large-data pagination and a publication-specific index need a measured,
coordinated follow-up; no existing migration was modified or performance claim made.

### Team ownership

The shared registration contract is documented in the
[registration README](../src/main/java/seedu/eventmanager/registration/README.md).
The shared EventRegistrations/RegisteredAttendee contract comes unchanged from
Joseph's feature-view-registration branch; Johannsen supplies its JDBC adapter.
Use RegistrationDatabaseMigration for explicit startup, then
RegistrationServiceFactory for authenticated commands. The factory shares one
JdbcDatabase across state, audit and notification-outbox writes. Organizer startup
now wires the reader into volunteers, registration overview and announcements; no registration UI is added by
this backend slice. Confirmed booking plus active venue and PUBLISHED future
event are required for registration.

| Role | Owns |
| --- | --- |
| Club Organizer (Joseph) | Events, volunteers/announcements (as scheduled), Organizer→venue submit |
| Venue Administrator (Jordan) | Venues, availability, request decide, bookings, Admin UI |
| Attendee (Johannsen) | Catalogue/details, registration backend, register/cancel/My Registrations and persistent Notifications UI; check-in remains planned |

---

## Documentation, logging, testing, configuration

### Documentation

Update [UserGuide.md](UserGuide.md) and this Developer Guide when behaviour changes. Keep Agentic SE notes in [AgenticSE.md](AgenticSE.md).

### Logging and interaction records

* Business audit records (e.g. venue decisions) are distinct from diagnostic logs.
* Meaningful agent tasks are recorded under `logs/<contributor>/` using [logs/templates/interaction.md](../logs/templates/interaction.md).
* Summaries: [logs/prompts_summary.md](../logs/prompts_summary.md).

### Testing

| Level | Examples | What it proves |
| --- | --- | --- |
| Unit | `EventServiceTest`, `OrganizerVenueRequestServiceTest`, `OrganizerIdsTest`, `VenueRequestValidatorTest` | Domain rules with fakes |
| In-process pipeline / E2E (no JavaFX) | `OrganizerToAdminVenuePipelineE2ETest`, `VenueAdministratorWorkflowE2ETest` | Submit → pending → approve wiring |
| Optional Postgres | `PostgreSqlVenueAdministratorIntegrationTest` (env `DATABASE_INTEGRATION_TESTS=true`) | Real JDBC for Admin persistence |
| Manual UI | See [Appendix: Instructions for manual testing](#appendix-instructions-for-manual-testing) | Desktop flows |

Run:

```sh
./gradlew test
```

Focused Organizer request tests:

```sh
./gradlew test --tests 'seedu.eventmanager.event.OrganizerVenueRequestServiceTest' --tests 'seedu.eventmanager.e2e.OrganizerToAdminVenuePipelineE2ETest'
```

### Configuration

| Variable | Purpose |
| --- | --- |
| `DATABASE_URL` / `EVENT_MANAGER_DB_URL` | JDBC URL |
| `DATABASE_USER` / `EVENT_MANAGER_DB_USER` | DB user |
| `DATABASE_PASSWORD` / `EVENT_MANAGER_DB_PASSWORD` | Optional password |
### Agentic SE workflow

Project instructions: [`AGENTS.md`](../AGENTS.md). Skills under `.agents/skills/`:

* requirements-and-acceptance
* test-driven-implementation
* code-review-and-verification
* desktop-ui-polish

See [Agentic SE](AgenticSE.md). Cursor project hooks (optional process guardrails): [hook-design.md](hook-design.md).

---

## Appendix: Requirements

### Product scope

**Target users:** Campus club organizers and venue administrators coordinating events and room bookings.

**Value:** One desktop app with role workspaces over a shared database so Organizer drafts can enter the Admin venue pipeline.

### User stories (selected)

| Priority | As a… | I want to… | So that… |
| --- | --- | --- | --- |
| High | Club Organizer | create and edit draft events | I can plan club activities |
| High | Club Organizer | request a venue for an event | Admin can approve a booking |
| High | Venue Administrator | see submitted requests | I can approve or reject them |
| Medium | Venue Administrator | manage venues and access | I only decide venues I control |
| Low | Club Organizer | create my own clubs in the UI | only my account manages my clubs' events *(create/list implemented; rename/delete not)* |

### Non-functional requirements (selected)

* Desktop JavaFX on JDK 25.
* PostgreSQL persistence for events and venue requests.
* Authorization for Admin decisions enforced in the service layer, not only UI visibility.
* Do not log passwords, tokens, or unnecessary personal data.

---

## Appendix: Planned Enhancements

* Organizer supersede/withdraw of open requests.
* Club rename/delete and multi-organizer clubs.
* Attendee check-in and attendance history.
* Delivery routes for non-attendee outbox events and email (attendee in-app delivery is implemented).
* Broader Postgres integration tests for the Organizer submit path.

---

## Appendix: Instructions for manual testing

### Launch

1. Ensure Postgres is running and `.env` is set.
2. `./gradlew run`
3. Confirm the shared login routes an ATTENDEE account to the catalogue and retains
   Organizer/Admin role routing. Attendee Home returns to the login screen.

### Organizer create/edit event

1. Open **Club Organizer**.
2. **Events** → **+ New event** → fill required fields → **Save event**.
3. Select the event, change the title, **Save event**.
4. **Revert changes** after editing without saving — last saved draft returns.

### Venue request pipeline

1. **Venue Administrator** → log in → **Venues** → **Create venue** (or **Claim access** on an existing venue).
2. Home → **Club Organizer** → create/select an event → **Request venue** → submit.
3. Home → **Venue Administrator** → **Venue requests** → confirm the row appears → **Approve** (or **Reject** with reason).
4. Confirm the request leaves the pending list.

### Typical failure checks

| Test | Expected |
| --- | --- |
| Submit with no ACTIVE venues | Cannot choose a venue / clear guidance |
| Submit twice for same event | Duplicate open-request error |
| Approve without venue access | Forbidden until **Claim access** |
| Different DB URLs per role | Admin does not see Organizer requests |

---

## Appendix: Current implementation status

**Implemented (selected):** shared role-based login; Attendee catalogue, registration backend, register/cancel, My Registrations and persistent Notifications inbox; Organizer account-owned clubs, draft events, venue requests, volunteers, registration overview and announcements; Admin venues, user management and request approve/reject; Flyway + JDBC persistence; unit and PostgreSQL integration tests.

**Not yet implemented (selected):** Attendee check-in and attendance history; club rename/delete; Organizer supersede/withdraw; email notification delivery; production deployment tooling.
