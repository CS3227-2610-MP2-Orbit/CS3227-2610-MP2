# 011 — Registration backend and Joseph's shared reader

Date: 26 September 2026
Contributor: Johannsen
Branch: attendee-registration-handoff, based on catalogue commit c5b9c06
Agent: Codex desktop, single agent

## Original request and policy replies

> can we push this as a pr then also do the thing that joseph's need such that he can continue to work as well

User repeated Joseph's README during implementation; confirmed it is the intended
contract. Earlier clarified contract is preserved in log 009.

Asked whether registration needs PUBLISHED + matching CONFIRMED booking + ACTIVE venue.
The user first asked:

> do we have a confirm active button?

Explained that venue ACTIVE and booking CONFIRMED are existing states, not one
button; Venue Admin approval creates the booking, but publication remains absent.
On the focused follow-up, the user answered:

> Yes, require all three states (recommended)

## Skills and scope

Read requirements-and-acceptance, test-driven-implementation, security-and-rbac,
database-migration-and-integrity and code-review-and-verification. Read installed
PostgreSQL skill and foreign-key-index, short-transaction and deadlock-order
references. Single agent; no delegation.

Publish catalogue first (PR #25); follow-up supplies a real registration backend
and Organizer reader. Joseph retains Organizer wiring ownership. No registration
UI, QR, normal check-in command, history UI or inbox delivery added. Confirmed and
checked-in status support in the schema/reader is not a check-in feature claim.

## Implementation and boundaries

- EventRegistrations/RegisteredAttendee copied unchanged from Joseph's branch.
- JdbcEventRegistrations selects only user_id and username. Filters to active
  ATTENDEE accounts with CONFIRMED/CHECKED_IN registrations. Unique event/account
  key prevents duplicate lifecycle rows; re-registration reuses the row.
- Separate registration migration stream/history table runs after existing
  shared/Organizer bootstraps. Canonical user/event FKs, status/timestamp checks,
  version constraint and owner index. No applied teammate migration edited.
- RegistrationService accepts a live session token, never arbitrary attendee IDs.
  Owner-scoped list includes ongoing/past/cancelled records. Role/session checks,
  eligibility, capacity, cancellation cutoff and expected-version rules enforced
  in service, not UI.
- Require future PUBLISHED event, matching CONFIRMED booking and ACTIVE venue.
  Event row locked for capacity decisions, then account and booking/venue guards.
  Start time checked again after possible waits.
- CONFIRMED/CHECKED_IN rows consume capacity, including inactive users. Inactive
  accounts disappear from Joseph's list but are not silently cancelled or deleted.
- Register/cancel state, business audit and outbox effects share one JdbcDatabase
  transaction. Failures roll back; lifecycle version distinguishes a new
  re-registration from a retry. No delivered-notification claim.
- Explicit factory/bootstrap and constructor instructions in RegistrationHandoff.
  Joseph's shared contract stays independent of registration storage details.

## Files changed

Registration package: shared interface/record, own Registration record,
RegistrationEvent snapshot, RegistrationStore boundary, service and factory.
Storage: JdbcEventRegistrations, JdbcRegistrationStore, RegistrationDatabaseMigration.
Schema: db/registration/V1__event_registrations.sql.
Tests: isolated-schema base, roster integration tests, service unit tests, and
workflow integration tests. CI adds a PostgreSQL-enabled registration test step.
Docs: registration README, RegistrationHandoff, Developer Guide notes,
and this log. No Organizer/Admin code or their existing tests changed.

## TDD evidence and commands

All commands run from the repository root. Disposable Postgres endpoint:
127.0.0.1:55448/mp2_attendee_test_johannsen; never the interactive demo DB.

1. Added shared API/migration/test scaffolding; empty JdbcEventRegistrations.
   Ran:
   `EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_attendee_test_johannsen EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests 'seedu.eventmanager.registration.JdbcEventRegistrationsIntegrationTest' --rerun-tasks`.
   Exit 1: all four tests failed on intended expected-registry assertions, not
   setup or compilation. Implemented the narrow projection; repeated focused
   test command without --rerun-tasks, exit 0, all four passed.
2. Added seven service tests against a compile-ready empty command scaffold.
   `./gradlew test --tests 'seedu.eventmanager.registration.RegistrationServiceTest'`:
   exit 1, seven assertion failures (missing returned registration or required
   rejection). Implemented rules; same command passed all seven.
3. Added real JDBC workflow tests: lifecycle/persistence, live session rejection,
   eligibility, two-connection last-seat/duplicate races and audit/outbox rollback.
   Same DB environment plus `./gradlew test --tests 'seedu.eventmanager.registration.*' --rerun-tasks`:
   exit 0, 18 tests, no skips. These extended integration checks were added after
   service implementation, not claimed as a separate red/green cycle.
4. Review identified that waiting for a booking lock could cross event start.
   Added `waitingForBookingLockCannotAdmitRegistrationAfterStart` with a advancing
   controlled clock; focused command failed on missing rejection (exit 1).
   Rechecked time after booking/capacity reads. Added cancellation-outbox rollback
   regression. Re-ran full registration package with DB environment: exit 0,
   20 tests with no skips/failures.
5. Full verification:
   `JAVA_TOOL_OPTIONS=-Duser.timezone=UTC DATABASE_URL=jdbc:postgresql://127.0.0.1:55448/mp2_attendee_test_johannsen DATABASE_USER=mp2_demo_app DATABASE_PASSWORD='' DATABASE_INTEGRATION_TESTS=true EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_attendee_test_johannsen EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew build --rerun-tasks`.
   Exit 0. XML report totals: 117 tests, 0 skipped, 0 failures, 0 errors.
   UTC is the documented workaround for pre-existing Venue offset-equality tests;
   new registration package passed independently in the normal local timezone.

6. Split the unpublished handoff branch from the catalogue so Joseph can use it
   independently: committed backend changes, then
   `git rebase --onto origin/main attendee attendee-registration-handoff`.
   Applied cleanly on main 4987149, leaving catalogue commits on attendee.
   Re-ran the same full UTC/Postgres build command: **108 tests, zero skipped,
   failures or errors**. Difference from 117 is the nine catalogue tests absent
   on this independent branch, not removed/disabled tests.
7. Compared Git blob hashes for EventRegistrations and RegisteredAttendee against
   origin/feature-view-registration: both pairs identical. Checked diff against
   main: no Organizer/Admin implementation, UI, shared migration or existing test
   modifications. `git diff --check` passed.

Catalogue PR #25 was created and attached to the task; GitHub Validate passed.
The handoff PR targets main directly; no dependency on catalogue merge order.

## Review, limitations and corrections

- Real concurrency checks use two separate service/database instances and
  synchronized starts, not a sequential fake to claim capacity safety.
- Failure injection uses temporary constraints inside each owned test schema;
  cancellation failure preserves the previous seat, audit and notification.
- Historical migrations and schema are isolated per test and dropped afterwards.
- Reader contract is internal: existing Organizer authorization remains required.
  Direct database access is privileged under the existing desktop trust model.
- No new dependency, security scanner or GUI test framework. Existing compiler
  and Gradle warnings remain. This backend does not demonstrate user-visible
  registration screens, email/inbox delivery or event publishing.
- Review corrected a post-lock timing bug using a failing regression; no
  unrelated teammate code was changed to make tests pass.
- Agent-evaluation edits remain in the named retained stash, excluded from both
  feature PRs. Machine-local demo credentials/data remain Git-ignored.

## AI-generated mini reflection

The shared contract can now be consumed without tying Joseph's features to the
registration schema. TDD exposed missing behaviour and a genuine timing edge;
real database tests supplied concurrency and rollback evidence. The remaining
application work is Attendee UI plus team publication/lifecycle coordination.

## Student review

- [ ] I verified prompts, policy replies, scope and commands.
- [ ] I verified changed files and red/green evidence.
- [ ] I verified test results and limitations.
- [ ] I added omitted mistakes or disagreements.

Reviewed by:
Review date:
