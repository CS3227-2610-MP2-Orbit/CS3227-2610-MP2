# 023 — Whole-product cross-role integration tests

Date: 2026-09-28
Contributor: Joseph (Club Organizer)
Branch and starting revision: `main` at `28b5973`, on top of the uncommitted publish/release work from log 022
Agent/tool: Cursor agent
Skills used (paths and revision or change description):
- `.agents/skills/test-driven-implementation/SKILL.md` (working-tree revision): used for the second-request fix. Worked without correction to the skill; a fixture bug had to be fixed before the intended red result.

## Objective

Add one integration suite that exercises the Club Organizer, Venue Administrator and
Attendee workflows together against real PostgreSQL, and fix any defect it exposes
within Joseph's scope.

## Original prompts (verbatim)

> can you create integration testing, and test caess for the whole poroject combined

Decision form (after the suite exposed a defect). Question: "An event with an APPROVED venue booking can currently submit another venue request, and approving it crashes on the database's one-booking-per-event rule. How should this work?" Selected: `block_until_release` ("Organizer side refuses: 'This event already has an approved venue. Release it under Request venue first.' Fix in my OrganizerVenueRequestService only, and matches the Release venue flow.")

## Response summary

Added `CrossRoleWorkflowIntegrationTest`. It runs every migration stream (venue, organizer,
registration, inbox) into one randomized schema that is dropped afterwards, then wires the
real services and JDBC repositories for all three roles. Only the clocks are faked: one
before the event and one during it. The suite exposed a cross-role defect, which was
fixed on the Organizer side after the user's decision.

## Assumptions and design decisions

- Test boundary: real services, real JDBC, real PostgreSQL; no fakes or mocks. Not a JavaFX test.
- Accounts are synthetic, created through `JdbcLocalSessionService` with the password `synthetic-test-account`. No real credentials are used or printed.
- Team decision (user, via form): a new venue request is refused while the event's latest request is `APPROVED`; the organizer must release first. Implemented in `OrganizerVenueRequestService.submit` only; Jordan's admin classes are unchanged.
- "Latest request APPROVED" is used as the booked signal. In this checkout, only `JdbcVenueRelease` cancels bookings, and it also marks the request `WITHDRAWN`.
- Existing behaviour recorded, not changed: when a venue is deactivated after publishing, the event stays listed in the catalogue but registration is refused with `VENUE_NOT_CONFIRMED`.

## Files changed

- `src/test/java/seedu/eventmanager/e2e/CrossRoleWorkflowIntegrationTest.java` (new, 7 tests)
- `src/main/java/seedu/eventmanager/event/OrganizerVenueRequestService.java` (refuse submit while latest request is APPROVED)
- `src/test/java/seedu/eventmanager/event/OrganizerVenueRequestServiceTest.java` (2 tests; the fake's `findOpenByEventId` now reads the latest state per request)
- `docs/DeveloperGuide.md` (release section and testing section)
- `docs/UserGuide.md` (Request venue caution)
- `logs/joseph/023-cross-role-integration-tests.md` (this log)

## Commands actually executed

Working directory: `/Users/josephkwok/Desktop/Website/cs3227/MP2`. Database `mp2_publish_test_1790595909` is disposable; each test uses its own schema.

1. `./gradlew compileTestJava -q`: exit 0.
2. `EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://localhost:5432/mp2_publish_test_1790595909 EVENT_MANAGER_TEST_DB_USER=josephkwok ./gradlew test --tests 'seedu.eventmanager.e2e.CrossRoleWorkflowIntegrationTest'`: 6 tests, 1 failed. The release test asserted that a second request on an approved event is refused, but `submit` accepted it.
3. The same command with a temporary probe test (approve a second request at another venue): the probe failed as designed and showed `IllegalStateException` caused by `duplicate key value violates unique constraint "idx_one_active_booking_per_event"`. The probe was then replaced by a real test.
4. `./gradlew test --tests 'seedu.eventmanager.event.OrganizerVenueRequestServiceTest'`: 17 tests, 2 failed. One failure was a fixture bug: the fake still reported a decided request as open.
5. The same command after fixing the fixture: 17 tests, 1 failed. This was the intended red: `submit_whileLatestRequestApproved_rejectedUntilReleased`, where nothing was thrown.
6. The same command after the fix: BUILD SUCCESSFUL.
7. Command 2 again: BUILD SUCCESSFUL; XML reports `tests="7" skipped="0" failures="0" errors="0"`.
8. `JAVA_TOOL_OPTIONS=-Duser.timezone=UTC EVENT_MANAGER_TEST_DB_URL=... EVENT_MANAGER_TEST_DB_USER=josephkwok ./gradlew test --rerun-tasks`: the first attempt ran inside the sandbox, Gradle produced no output, and nothing ran (not counted). The rerun without the sandbox gave BUILD SUCCESSFUL in 49s; summed XML totals were tests=315, skipped=5, failures=0, errors=0.

## Actual verification results

- Cross-role suite, all 7 tests passing on real PostgreSQL:
  - full lifecycle across all three roles, including the outbox-to-inbox delivery, check-in, attendance history, the organizer audit trail and `audit_logs`;
  - release and re-request with a real admin re-approval;
  - admin rejection and `BOOKING_CONFLICT`;
  - cross-organizer access denial with no side effects;
  - capacity (`EVENT_FULL`) and cancellation;
  - venue deactivated after publishing;
  - second request refused while booked.
- Red/green for the fix: 1 intended failure, then passing.
- Full suite: 315 tests, 5 skipped (Jordan's `PostgreSqlVenueAdministratorIntegrationTest`, which is gated by a different variable), 0 failures.
- Not run: JavaFX/manual checks of the new refusal message on the Request venue screen.

## Problems, corrections, and skill revisions

- My first draft assumed that an approved event could not submit another request. The code allowed it. The real-database run exposed this, and a probe confirmed that the admin approval then crashes. The user chose the fix.
- A test fixture (the fake `findOpenByEventId` scanning every saved version) produced a false red. It was fixed before the intended red run.
- A full-suite run inside the sandbox silently did not execute. It was re-run without the sandbox; only that run's numbers are reported.

## Outcome and limitations

- The whole-product integration suite is present and passing on a disposable database. It is skipped when `EVENT_MANAGER_TEST_DB_URL` is unset.
- Defect fixed: an organizer can no longer create a second request that makes the admin's approval fail with a raw database error.
- Limitations:
  - Jordan's approve still surfaces the raw database error if the duplicate-booking state is reached some other way. Reported to the team rather than changed.
  - A venue deactivated after publishing leaves the event listed but not registerable.
  - No UI-level end-to-end test.
- No git operations were performed, at the user's instruction.

Suggested commit message: `Add cross-role integration suite; refuse venue request while booked`

## AI-generated mini reflection

The strongest result is that the combined real-database suite found a defect that the
per-role fakes could not show. The organizer and admin unit tests each passed on their own
while the two together failed. The main limitation is that the suite stops at the
service layer; a JavaFX end-to-end run is the useful next step.
