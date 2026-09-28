# 028 — Close the publish vs venue-release race

Date: 2026-09-28
Contributor: Joseph
Branch and starting revision: `bug-publish-event` at `54bed84`
Agent/tool: Cursor
Skills used: `.agents/skills/test-driven-implementation/SKILL.md` (`6c488f2`)

## Objective

Stop a draft from becoming `PUBLISHED` after its confirmed booking has been released in a concurrent organizer release.

## Original prompts (verbatim)

> can u fix this bug now?
>
> An event can be published while its booking is being released at the same moment,
> leaving a published event with no booking.

The same request was sent again after a compacted session.

## Response summary

`EventService.publishEvent` still does an early booking check, but persisting `PUBLISHED` now locks the `organizer_event` row (`SELECT … FOR UPDATE`, same lock as `JdbcVenueRelease`) and re-checks the confirmed active booking on that connection before the status update. If the booking is gone, publish throws `ValidationException`, rolls back, and the event stays `DRAFT`.

## Assumptions and design decisions

- Assignment/team rule (already implemented as a pre-check): publish needs a confirmed booking at an active venue matching the event times. The missing piece was atomicity with organizer release, not a new product rule.
- The service-layer check stays for in-memory unit tests and for the existing user-facing messages. The JDBC persist path is what closes the TOCTOU window.
- Release already locked the draft first, then cancelled the booking. Publish now uses the same event-row lock order, then a non-locking re-read of the booking predicate shared with registration.
- The integration test is a sequential sabotage (check returns true, then `releaseApprovedBooking` commits, then persist). It proves the re-check; it is not a two-thread scheduler test.
- `publishUpdate_auditFailure_rollsBackStatus` now seeds a matching booking first, because a publish persist without a booking is rejected before the audit insert.

## Files changed

- `src/test/java/seedu/eventmanager/event/EventPublishIntegrationTest.java`
- `src/main/java/seedu/eventmanager/event/JdbcEventRepository.java`
- `src/main/java/seedu/eventmanager/storage/JdbcEventBookingCheck.java`
- `src/main/java/seedu/eventmanager/storage/JdbcVenueRelease.java`
- `docs/DeveloperGuide.md`
- `docs/UserGuide.md`
- `logs/joseph/028-publish-release-race.md`

## Commands actually executed

From `/Users/josephkwok/Desktop/Website/cs3227/MP2`, with `EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://localhost:5432/mp2_review_2609` and `EVENT_MANAGER_TEST_DB_USER=josephkwok` (password unset; local peer auth). No secrets printed.

1. Red: `./gradlew test --tests 'seedu.eventmanager.event.EventPublishIntegrationTest.publish_bookingReleasedAfterCheck_refusesAndLeavesDraft' --rerun-tasks` — exit 1
2. Green (focused): `./gradlew test --tests 'seedu.eventmanager.event.EventPublishIntegrationTest' --tests 'seedu.eventmanager.event.EventServiceTest' checkstyleMain --rerun-tasks` — exit 0
3. `JAVA_TOOL_OPTIONS=-Duser.timezone=UTC ./gradlew test --tests 'seedu.eventmanager.e2e.CrossRoleWorkflowIntegrationTest' --rerun-tasks` — exit 0
4. `JAVA_TOOL_OPTIONS=-Duser.timezone=UTC ./gradlew test --rerun-tasks` — exit 0
5. `git status` / `git diff --stat` after the change

## Actual verification results

- Red: `AssertionFailedError` at `EventPublishIntegrationTest.java:227` (`assertThrows(ValidationException)`). Publish succeeded after the booking was released, so the event would have been `PUBLISHED` with no confirmed booking.
- Green: `EventPublishIntegrationTest` tests=14 failures=0 errors=0 skipped=0; `EventServiceTest` tests=33 failures=0 errors=0 skipped=0; `checkstyleMain` passed.
- `CrossRoleWorkflowIntegrationTest` tests=10 failures=0 errors=0 skipped=0.
- Full suite: tests=329 failures=0 errors=0 skipped=5 (same `PostgreSqlVenueAdministratorIntegrationTest` skips as in log 027 when `DATABASE_INTEGRATION_TESTS` is unset).
- Desktop UI was not run. Two-thread concurrent publish/release was not run.

## Problems, corrections, and skill revisions

The first `StrReplace` for the `Connection` import in `JdbcEventBookingCheck` omitted `path` and was retried. No skill file was changed.

## Outcome and limitations

The TOCTOU between the service booking check and the status update is closed on the JDBC persist path. A sequential sabotage test covers that window. True concurrent thread interleaving and JavaFX publish/release clicks were not executed. Work is uncommitted.

Suggested commit message:

```
Refuse publish when the venue booking is released concurrently
```

## AI-generated mini reflection

The defect was a transaction boundary, not a missing validation message. The strongest outcome is a failing integration test that published a booking-less event, then a lock-and-re-check that made the same test refuse and leave a draft. The main limitation is that the test is sequential sabotage rather than two overlapping transactions. A useful next step is a two-connection integration that starts release after publish has taken `FOR UPDATE`.

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
