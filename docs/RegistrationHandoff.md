# Registration handoff — Johannsen to Joseph

## Status and ownership

This is a backend handoff, not an announcement that the Attendee registration UI
is finished. It includes persistent register/cancel/list operations, audit/outbox
writes and the real read-only Organizer adapter. No Organizer or Venue feature
implementation is changed. Joseph owns application wiring for Volunteers, View
registrations and Announcements. Attendee buttons/screens, normal check-in,
attendance-history UI and notification inbox delivery remain follow-ups.

The shared interface and DTO are copied unchanged from Joseph's
`feature-view-registration` branch to keep the integration source-compatible.
Those classes need not be redesigned or duplicated when his PRs merge.

## Joseph's integration

At application startup, initialize once (not on the JavaFX UI thread):

```java
DatabaseConfiguration configuration = DatabaseBootstrap.configuration();
RegistrationDatabaseMigration.migrate(configuration);
EventRegistrations registrations =
        new JdbcEventRegistrations(new JdbcDatabase(configuration));
```

Imports: `EventRegistrations` is in `seedu.eventmanager.registration`; the other
types above are in `seedu.eventmanager.storage`. Supply `registrations` where
Organizer features currently receive `new NoEventRegistrations()`. Keep existing
Organizer ownership/authorization checks before calling it.

Bootstrap reuses shared Venue/Auth migrations and the existing Organizer
migration, then applies registration's independent Flyway stream at
`db/registration` using `registration_schema_history`. This avoids a V-number
collision with Jordan and ensures the canonical users/event tables exist before
foreign keys are created. Repeated bootstrap validates existing migrations and
preserves registration data. It does not insert registrations or published events.
It also runs existing upstream migrations, including upstream development-account
provisioning; this is not a new production-account policy.

## Exact roster semantics

| Concern | Behaviour |
| --- | --- |
| Identifier | Shared ATTENDEE `users.user_id` |
| Display name | Current `users.username`; no new profile field |
| Included status | CONFIRMED or CHECKED_IN |
| Excluded status | CANCELLED |
| Inactive account | Excluded from roster, record/history retained |
| Changed role | Non-ATTENDEE excluded |
| Uniqueness | One database row per event/account; re-registration updates it |
| Sorting | Not guaranteed; caller sorts |
| Exposed fields | ID and username only |
| Event lifecycle | No future-only event filter; checked-in registrants remain queryable after completion |

Deactivation/role changes do not implicitly cancel or free reserved capacity.
They can therefore produce a roster count smaller than occupied places. Do not
use this filtered list to calculate available seats; the write workflow counts
all CONFIRMED/CHECKED_IN registration rows. Account-lifecycle cancellation is a
separate policy, not silently introduced here.

## Attendee backend entry points

```java
RegistrationService service =
        RegistrationServiceFactory.create(configuration, Clock.systemUTC());

// Token from the existing shared login; do not accept a caller-supplied attendee ID.
Registration saved = service.register(sessionToken, eventId, -1);
List<Registration> mine = service.myRegistrations(sessionToken);
Registration cancelled = service.cancel(sessionToken, eventId, saved.version());
Registration again = service.register(sessionToken, eventId, cancelled.version());
```

`expectedVersion=-1` means no previous registration. Once a record exists, use
the version returned by the service. Exact retries return the existing target
state without new side effects; stale commands cannot undo a newer lifecycle.
The returned own-record DTO is separate from the two-field Organizer DTO.
`myRegistrations` includes ongoing/past and cancelled records, not just the
future-only catalogue.

The user's confirmed eligibility policy requires **all three**:

1. Event is PUBLISHED and strictly before its start.
2. A CONFIRMED booking exists with the same event start/end.
3. Its venue is ACTIVE.

The event must also have capacity. Only the authenticated account may register
or cancel itself. Cancellation is allowed strictly before start, not after check-in.
Re-registration is allowed under the same eligibility/capacity rules. There is
no waitlist or automatic event publication. Shared authentication is resolved on
every command, including expired/revoked/inactive-account rejection.

No check-in command is added here. CHECKED_IN is supported in schema/roster rules
for the later normal self-check-in slice; tests use explicit fixtures for it.

## Consistency and side effects

- Registration writes lock the canonical event row first, serialize capacity
  decisions and retain the lock through commit.
- Account and matching booking/venue rows are protected while eligibility is
  checked. The start-time cutoff is checked again after potentially waiting.
- The unique event/account key prevents duplicate records.
- State, `audit_logs` and `notification_outbox` are committed together using
  one shared `JdbcDatabase`. Required side-effect failures roll back the command.
- Notification payloads include registration ID and lifecycle version, so a
  retry deduplicates but a genuinely new registration gets its own notification.
- Outbox entries are not proof of delivered inbox messages; delivery is not in
  this handoff.

Use the factory rather than composing repositories with unrelated JdbcDatabase
instances. JdbcRegistrationStore is an internal transaction-aware adapter, not
an API for bypassing the service rules. Raw SQL/direct JDBC can bypass the Java
authorization model; this remains the team's local desktop trust model.

Future changes to published-event capacity/lifecycle or booking cancellation
must coordinate with registration invariants. This PR does not invent automatic
cancellation/refund behaviour or modify Joseph/Jordan's lifecycle code.

## Verification

Use a disposable database, never your interactive demo/application database:

```sh
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://localhost:5432/mp2_test \
EVENT_MANAGER_TEST_DB_USER=your_test_user \
./gradlew test --tests 'seedu.eventmanager.registration.*'
```

Set the test password through your environment if required. Integration tests
create/drop only a randomized schema they own. Without the test URL they skip;
skips do not prove persistence. CI explicitly supplies the PostgreSQL test settings.

Coverage includes roster filtering/deduplication, username updates, inactive and
reactivated accounts, schema repeatability, live sessions, capacity, cancellation
and stale retries, two-connection last-seat and duplicate races, and rollback on
audit/outbox failure. Unit tests use a controlled clock for timing boundaries.
Current command results and limits are in log 011.

## Remaining team handoff

- Joseph: replace the placeholder, retain authorization, and provide event publication.
- Johannsen: add Attendee registration UI and later normal check-in/history/inbox.
- Jordan/team: coordinate future account/booking lifecycle changes and shared security.

Until an actual publishing workflow exists, use clearly labelled synthetic events
for integration tests. Approval alone does not publish an event.
