# 024 — Delete draft events

Date: 2026-09-28
Contributor: Joseph (Club Organizer)
Branch and starting revision: `main` at `28b5973`, on top of the uncommitted work from logs 022 and 023
Agent/tool: Cursor agent
Skills used (paths and revision or change description):
- `.agents/skills/test-driven-implementation/SKILL.md` (working-tree revision): used without correction.

## Objective

Let club organizers delete events from the Events screen, and add and run tests for it.

## Original prompts (verbatim)

> laslty, can u also add a delete event button so club organizers can delete events. then create and run more tests on ti

Decision form (asked before implementing, because the deletion policy was unresolved):
- "Which events can an organizer delete?" Selected: `drafts_only` ("Drafts only — published events have attendees/registrations, so they can't be deleted").
- "How should deletion be stored?" Selected: `soft` ("Soft delete — mark the event DELETED, hide it everywhere, keep the audit trail with a DELETE_EVENT entry").
- "What if the draft has a submitted or approved venue request?" Selected: `auto_release` ("Deleting also withdraws the open request / releases the approved booking, in the same transaction").

## Response summary

Added a **Delete** button, shown only for drafts, with a confirmation dialog. It calls
`EventService.deleteEvent`, which soft-deletes the draft through a new `DraftEventDeletion`
port. The JDBC implementation marks the event `DELETED`, withdraws a `SUBMITTED` venue
request, releases an approved booking, and writes the audit records, all in one
transaction. Deleted events are treated as not found by `EventService` and hidden from its listings.

## Assumptions and design decisions

- Team decisions (user, via form): drafts only; soft delete; venue clean-up in the same transaction.
- Only `SUBMITTED` requests are withdrawn. The schema requires `submitted_at` for a `WITHDRAWN` row, and the organizer flow creates `SUBMITTED` requests directly, never `DRAFT`.
- The booking release reuses the SQL extracted from `JdbcVenueRelease` (`cancelActiveBooking`) with reason code `EVENT_DELETED`. Release behaviour is otherwise unchanged; the reason code is now passed in as a parameter.
- Hiding is enforced in `EventService` (`findExisting` and `listEvents`), not in the UI. Every Organizer workflow that goes through `getEvent` therefore refuses deleted events.
- No changes to Jordan's or Johannsen's classes. Their tables are written only through the release and withdraw SQL described above.

## Files changed

- `src/main/java/seedu/eventmanager/event/EventStatus.java` (`DELETED`)
- `src/main/java/seedu/eventmanager/event/EventAuditRecord.java` (`DELETE_EVENT`)
- `src/main/java/seedu/eventmanager/event/DraftEventDeletion.java` (new port)
- `src/main/java/seedu/eventmanager/event/EventService.java` (6-argument constructor, `deleteEvent`, deleted events hidden)
- `src/main/java/seedu/eventmanager/storage/JdbcDraftEventDeletion.java` (new)
- `src/main/java/seedu/eventmanager/storage/JdbcVenueRelease.java` (extracted `cancelActiveBooking` helper)
- `src/main/java/seedu/eventmanager/ui/OrganizerEventView.java` (Delete button, confirmation, `Deleted` status label)
- `src/main/java/seedu/eventmanager/ui/EventManagerApplication.java` (wiring)
- `src/main/java/seedu/eventmanager/event/README.md`, `docs/UserGuide.md`, `docs/DeveloperGuide.md`
- Tests:
  - `src/test/java/seedu/eventmanager/event/EventServiceTest.java` (+5)
  - `src/test/java/seedu/eventmanager/event/EventPublishIntegrationTest.java` (+5)
  - `src/test/java/seedu/eventmanager/e2e/CrossRoleWorkflowIntegrationTest.java` (+3)

## Commands actually executed

Working directory: `/Users/josephkwok/Desktop/Website/cs3227/MP2`. The test database `mp2_publish_test_1790595909` is disposable, and each test uses its own schema.

1. `./gradlew test --tests 'seedu.eventmanager.event.EventServiceTest'`: first attempt was a compile failure. The UI status `switch` did not cover `DELETED`; I added a `Deleted` label.
2. The same command: 33 tests, 5 failed. This was the intended red: all five delete tests failed with `UnsupportedOperationException` from the scaffold stub.
3. The same command after implementing: BUILD SUCCESSFUL.
4. `EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://localhost:5432/mp2_publish_test_1790595909 EVENT_MANAGER_TEST_DB_USER=josephkwok ./gradlew test --tests 'seedu.eventmanager.event.EventPublishIntegrationTest'`: BUILD SUCCESSFUL; XML reports `tests="13" skipped="0" failures="0" errors="0"`.
5. The same variables with `--tests 'seedu.eventmanager.e2e.CrossRoleWorkflowIntegrationTest'`: BUILD SUCCESSFUL; `tests="10" skipped="0" failures="0" errors="0"`.
6. `JAVA_TOOL_OPTIONS=-Duser.timezone=UTC EVENT_MANAGER_TEST_DB_URL=... EVENT_MANAGER_TEST_DB_USER=josephkwok ./gradlew test --rerun-tasks`, run without the sandbox: BUILD SUCCESSFUL in 53s. Summed XML totals: tests=328, skipped=5, failures=0, errors=0.

## Actual verification results

- **Unit (fakes):**
  - an owned draft is soft-deleted with a `DELETE_EVENT` audit record and is then not found by list, get, edit, publish or a second delete;
  - a published event is refused;
  - an unowned event is refused;
  - an unknown event, a stale version and a negative version are refused;
  - a missing port fails closed.
- **PostgreSQL:**
  - the row is marked `DELETED` with the audit record and is not in the catalogue;
  - an approved booking is released (reason `EVENT_DELETED`) and its request withdrawn;
  - a submitted request is withdrawn and audited;
  - a published event is refused by both the service and the SQL guard;
  - an audit failure or stale version rolls back the event and venue changes.
- **Cross-role (real admin and attendee services):**
  - after a deletion, the freed slot is approved for another club, which then publishes;
  - the admin gets `INVALID_STATE` when approving a withdrawn request;
  - a published event cannot be deleted and still accepts registrations.
- **Full suite:** 328 tests, 5 skipped (Jordan's separately gated PostgreSQL tests), 0 failures.
- **Not run:** manual JavaFX check of the Delete button. The app running in the terminal was started before this change and needs a restart.

## Problems, corrections, and skill revisions

- The new enum value broke an exhaustive `switch` in the UI. It was caught at compile time and fixed before the red run.

## Outcome and limitations

- Organizers can delete drafts; published events cannot be deleted, as decided.
- Deletion is permanent from the UI; there is no restore.
- Jordan's and Johannsen's screens don't read `organizer_event.status` for drafts. If a future feature lists events by status, it must exclude `DELETED`.
- No git operations were performed, at the user's instruction.

Suggested commit message: `Add soft delete for draft events with venue clean-up`

## AI-generated mini reflection

The strongest result is that the delete behaviour was checked at every layer: the rule in
unit tests, one-transaction rollback in PostgreSQL, and the effect on the Venue Administrator
queue and slot availability in the cross-role suite. The main limitation is the lack of a
recorded manual UI check of the button; clicking through it in the running app is the next step.
