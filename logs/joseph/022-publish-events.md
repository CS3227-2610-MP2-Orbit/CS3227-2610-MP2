# 022 — Publish events for Attendee visibility

Date: 28 September 2026 (SGT)
Contributor: Joseph (Club Organizer)
Branch and starting revision: working tree on `main` at `28b5973` (after PR #41 merged); no branch, staging or commit by the agent
Agent/tool: Cursor agent
Skills used (paths and revision or change description): `.agents/skills/test-driven-implementation/SKILL.md` (unchanged); `code-review-and-verification` practices for the PR #41 review (skill file not re-read)

## Objective

Review Johannsen's completed Attendee work (PR #41) for clashes with the Organizer
side, then add the missing Organizer publish workflow so Attendees can browse,
register and check in to real events.

## Original prompts (verbatim)

1. > my friend just implemented the attendee side, however there is some logic clashing with my end. he sent me this prompt, can you have a read and create a plan?
   >
   > Problem: Every Organizer event stays a draft. createEvent always saves EventStatus.DRAFT
   >   (EventService.java:62), editEvent only works on drafts, and nothing ever sets PUBLISHED.
   >   There's no publish method, no publish audit action and no Publish button. Attendees only
   >   see PUBLISHED events, so in the real app no attendee can browse, register or check in. The
   >   Organizer registration list and announcements also stay empty.
   >
   >   Change needed (Organizer side only):
   >   1. Add EventService.publishEvent(actor, eventId, expectedVersion). Only the owning
   >      organizer can publish, only from draft to published, with the same version check
   >      editEvent uses.
   >   2. Add a PUBLISH_EVENT audit action, saved in the same transaction as the status change
   >      (same pattern as create/edit in JdbcEventRepository).
   >   3. In the Organizer UI, show each event's status and add a Publish button for owned
   >      drafts.
   >   4. Add tests: unit tests plus a real PostgreSQL test for persistence and rollback. Update
   >      the event README and guides where they say "no publish action".
   >
   >   Decisions for Joseph:
   >   - Must the event have a confirmed venue booking with matching times before it can be
   >     published? Attendee suggests yes; otherwise attendees see the event but can't register.
   >   - Can a published event still be edited? Changing its times breaks the booking match.
   >   - Is unpublishing allowed?
   >   - Can an event be published after its start time?
2. Decision form answers: booking required = yes; edit after publish = no; unpublish = not in this change; publish after start = no (future only).
3. > my friend created his attendee logic, its finished, based on the thing i just did and his code, implement the correct implementation and fix the bugs
4. > sorry, one more thing, my friend just pushed a new PR, this is the last last final push from him. can u have a look at this. Im going to undo the changes u just did. I just pulled it from the main again. Dont do any git version control for me, no commits, git add, or anything.
   >
   > https://github.com/CS3227-2610-MP2-Orbit/CS3227-2610-MP2/pull/41
5. Decision form answer: implement publishing + tests + docs on the current `main` working tree with no git operations.

## Response summary

Reviewed PR #41 as merged in `main`. No regression to Organizer code was found:
`EVENT_ANNOUNCEMENT` is one of the inbox worker's routed types (deleted
announcements display "Announcement removed."), `InboxDatabaseMigration` still
runs `RegistrationDatabaseMigration`, the default outbox constructor still claims
all types, and check-in only produces `CHECKED_IN` rows the Organizer roster
already reads. The blocking clash was the absence of any publish path. Implemented
`EventService.publishEvent`, a read-only booking check sharing the Attendee SQL
predicate, UI status and Publish button, tests and docs.

## Assumptions and design decisions

Team decisions (Joseph, via decision form):
- Publishing requires a CONFIRMED booking at an ACTIVE venue whose start/end equal the event's (same as `RegistrationReadSql`).
- Published events are not editable (`editEvent` already draft-only).
- Unpublishing is out of scope.
- Publishing requires `now < startsAt`.

Implementation decisions:
- Check order: exists, ownership, version, DRAFT, future start, booking check.
- `EventBookingCheck` port in `event`; `JdbcEventBookingCheck` in `storage` so it can reuse package-private `RegistrationReadSql`. Jordan's and Johannsen's code unchanged.
- Existing `EventService` constructors keep working; without a booking check, `publishEvent` fails closed (`IllegalStateException`).
- Reuses `JdbcEventRepository.update` (status, version check and audit in one transaction); `action` is `VARCHAR(60)` without a constraint, so no migration.
- UI: status line on every event card, Publish button only for drafts, confirmation dialog, read-only form for non-drafts. Request venue help text now points to publishing.

Unresolved / not implemented: behaviour when a booking is cancelled or venue deactivated after publishing (event stays PUBLISHED; Attendee rules refuse registration/check-in).

## Files changed

- `src/main/java/seedu/eventmanager/event/EventBookingCheck.java` (new)
- `src/main/java/seedu/eventmanager/event/EventAuditRecord.java`
- `src/main/java/seedu/eventmanager/event/EventService.java`
- `src/main/java/seedu/eventmanager/storage/JdbcEventBookingCheck.java` (new)
- `src/main/java/seedu/eventmanager/event/VenueRelease.java` (new, follow-up)
- `src/main/java/seedu/eventmanager/storage/JdbcVenueRelease.java` (new, follow-up)
- `src/main/java/seedu/eventmanager/event/OrganizerVenueRequestService.java` (follow-up)
- `src/test/java/seedu/eventmanager/event/OrganizerVenueRequestServiceTest.java` (follow-up)
- `src/main/java/seedu/eventmanager/ui/EventManagerApplication.java`
- `src/main/java/seedu/eventmanager/ui/OrganizerEventView.java`
- `src/main/java/seedu/eventmanager/event/README.md`
- `src/test/java/seedu/eventmanager/event/EventServiceTest.java`
- `src/test/java/seedu/eventmanager/event/EventPublishIntegrationTest.java` (new; extended in the follow-up)
- `docs/UserGuide.md`
- `docs/DeveloperGuide.md`
- `logs/joseph/022-publish-events.md` (this log)

## Commands actually executed

Working directory `/Users/josephkwok/Desktop/Website/cs3227/MP2`.

- `gh pr view 41 --json body,mergeable,baseRefName ...` and `git diff --stat origin/main...origin/attendee-club-filter` (review, before merge)
- `git status -sb && git log --oneline -5` (read-only; `main` at `28b5973`, clean)
- `git diff 11218b8 28b5973 -- .../JdbcNotificationOutboxRepository.java .../EventManagerApplication.java .../RegistrationService.java` (read-only review)
- Red: `./gradlew test --tests 'seedu.eventmanager.event.EventServiceTest'` — exit 1, 23 tests, 9 failed
- Green: same command — exit 0; report `tests="23" failures="0" errors="0"`
- `createdb mp2_publish_test_1790595909` (Postgres.app, disposable database)
- `EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://localhost:5432/mp2_publish_test_1790595909 EVENT_MANAGER_TEST_DB_USER=josephkwok ./gradlew test --tests 'seedu.eventmanager.event.EventPublishIntegrationTest'` — first exit 1 (missing `ValidationException` import, compile error), then exit 0, 4 tests, 0 failures
- `./gradlew compileJava compileTestJava test` — exit 0
- `JAVA_TOOL_OPTIONS=-Duser.timezone=UTC EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://localhost:5432/mp2_publish_test_1790595909 EVENT_MANAGER_TEST_DB_USER=josephkwok ./gradlew test --rerun-tasks` — exit 0; 292 tests, 5 skipped, 0 failures, 0 errors

## Actual verification results

- Red: the 9 new `EventServiceTest` publish/edit-after-publish tests failed against a stub `publishEvent` that threw `UnsupportedOperationException`; the 14 existing tests passed.
- Green: 23/23 `EventServiceTest` after implementation.
- Integration (`EventPublishIntegrationTest`, real Flyway Venue schema plus Organizer schema in a per-test schema dropped afterwards): publish persists `PUBLISHED`, version 1 and `PUBLISH_EVENT` audit, and the event becomes visible through `JdbcEventCatalogueRepository`; cancelled, time-mismatched, inactive-venue and other-event bookings are rejected; audit failure rolls back the status; a stale second publish is rejected. Written after the implementation, so this is coverage, not a red/green cycle.
- Full suite: 292 tests, 0 failures/errors. The 5 skips are all in `PostgreSqlVenueAdministratorIntegrationTest`, which uses a different enablement variable; they are pre-existing and unrelated.
- Not run: manual JavaFX check of the Publish button, confirmation and read-only form; real-login end-to-end Organizer → Admin approval → publish → Attendee browse.

## Problems, corrections, and skill revisions

- The first implementation attempt was on a new branch created from the PR #41 head; the user undid it, pulled `main` and asked for no git operations. The work was re-applied to the `main` working tree without branch/stage/commit.
- A first integration-test draft violated Jordan's schema (CANCELLED bookings need cancellation fields; one active booking per event). It was corrected before running by filling the fields and using separate event IDs.
- A compile error (missing import) in the integration test was fixed and rerun.
- No skill revision required.

## Outcome and limitations

Publishing is implemented in the working tree and not committed. Limitations:
events stay PUBLISHED if a booking is later cancelled or the venue deactivated;
editing draft times after approval blocks publishing until they match again; no
unpublish; UI verified by compilation only. The disposable database
`mp2_publish_test_1790595909` remains for the user to drop.

Suggested commit message: `feat(event): publish approved drafts for attendee visibility`

## Follow-up: approved event could not be published

Prompts (verbatim):

> it doens't work, there are bugs
>
> It is accepted but i cannot publish it

(Sent with two screenshots: "Welcome tea" 29 Sep 2026 showing `Venue request: APPROVED`, and the Events screen showing the refusal *Publishing needs a confirmed venue booking at an active venue matching the event times*.)

Decision form answer: block time changes after approval (title, description and capacity stay editable; errors name the booked times).

Diagnosis (read-only `psql -X -d event_manager` SELECTs on `organizer_event`,
`venue_requests`, `venue_bookings`, `organizer_event_audit_record`): the request
was submitted and approved at 19:52 for 28 Sep 18:00 → 29 Sep 20:00 SGT, and the
event was then edited eight times (19:53) to 29 Sep 18:00 → 20:00. The booking no
longer matched, so the refusal was the intended rule, but the organizer had no way
to recover: times could change silently after approval, Request venue keeps
Submit disabled while `APPROVED`, and the message did not name the booked times.

Changes:
- `EventBookingCheck` gains `ActiveBooking` and `findActiveBooking` (CONFIRMED/AT_RISK window plus venue ACTIVE flag); `JdbcEventBookingCheck` implements it.
- `EventService.editEvent` rejects time changes when a booking exists unless the new times equal the booked window (restoring drifted drafts stays possible). Without a booking check, the lock is skipped (only test-only constructors).
- `EventService.publishEvent` refusals now distinguish no booking, times differing from the booked window (named in SGT), inactive venue, and unconfirmed booking.
- Docs: User Guide cautions and publish messages, Developer Guide publish section, event README.

Commands and results:
- Red: `./gradlew test --tests 'seedu.eventmanager.event.EventServiceTest'` — exit 1, 28 tests, 3 failed (time-change lock, publish mismatch message, inactive venue message). The two other new tests (other fields editable, restoring booked times then publishing) passed before the change and are regression coverage.
- Green: same command — exit 0, 28/28.
- Red: `EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://localhost:5432/mp2_publish_test_1790595909 EVENT_MANAGER_TEST_DB_USER=josephkwok ./gradlew test --tests 'seedu.eventmanager.event.EventPublishIntegrationTest'` — exit 1, 6 tests, 2 failed against the empty `findActiveBooking` stub.
- Green: same command — exit 0, 6/6.
- `JAVA_TOOL_OPTIONS=-Duser.timezone=UTC EVENT_MANAGER_TEST_DB_URL=... EVENT_MANAGER_TEST_DB_USER=josephkwok ./gradlew test --rerun-tasks` — exit 0; 299 tests, 5 skipped (same `PostgreSqlVenueAdministratorIntegrationTest` skips), 0 failures.

Not run: manual JavaFX retest of the user's "Welcome tea" event after the fix.
Limitation: approved events cannot move to new times; that needs a re-request or
supersede flow in Jordan's venue workflow.

## Follow-up: re-requesting a venue after approval

Prompts (verbatim):

> ah so the logic now si that if i chagne the event date after i accept a venue, it will be invalid

> but if I change the event date and want to resubmit a venue request, it does not let me do it

Decision form answers: add a "Release venue" button on Request venue for approved, unpublished events (cancel booking, withdraw request, audit), then edit times and resubmit; releasing is for drafts only.

Changes:
- New Organizer port `event.VenueRelease` and `storage.JdbcVenueRelease`: one transaction locks the draft `organizer_event` row, cancels the CONFIRMED/AT_RISK booking with cancellation fields, sets the approved request to `WITHDRAWN`, and inserts `VENUE_BOOKING_RELEASED` (`CLUB_ORGANIZER`) into `audit_logs`. Jordan's classes are unchanged; his tables are written.
- `OrganizerVenueRequestService.releaseApprovedVenue` (ownership, DRAFT only, fails closed if unconfigured); new 5-arg constructor, old constructor kept.
- UI: **Release venue** button with confirmation on Request venue, shown only for APPROVED drafts; the edit-lock message now says to release the venue first.
- Wiring in `EventManagerApplication`; User Guide section "Releasing an approved venue", Developer Guide and event README.

Commands and results:
- Red: `./gradlew test --tests 'seedu.eventmanager.event.OrganizerVenueRequestServiceTest'` — exit 1, 15 tests, 5 failed (stub).
- Green: same command — exit 0, 15/15.
- Red: `EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://localhost:5432/mp2_publish_test_1790595909 EVENT_MANAGER_TEST_DB_USER=josephkwok ./gradlew test --tests 'seedu.eventmanager.event.EventPublishIntegrationTest'` — exit 1, 8 tests, 1 failed (release on a draft). The "published or unbooked changes nothing" test passed against the no-op stub; it guards against side effects.
- Green: same command — exit 0, 8/8.
- `JAVA_TOOL_OPTIONS=-Duser.timezone=UTC EVENT_MANAGER_TEST_DB_URL=... EVENT_MANAGER_TEST_DB_USER=josephkwok ./gradlew test --rerun-tasks` — exit 0; 306 tests, 5 skipped (same Venue Admin integration skips), 0 failures.

Not run: manual JavaFX check of Release venue, and the real Venue Administrator re-approval after release (the integration test inserts the second booking directly rather than calling `VenueAdministratorService`).
Limitation: a publish already past its booking check when a release commits can still complete; Jordan should be told that Organizer code now writes his venue tables.

## AI-generated mini reflection

The task showed that the clash came from a missing Organizer workflow rather than
defects in the Attendee code. The strongest outcome is that publishing reuses the
exact Attendee booking predicate, so the two sides cannot disagree about
eligibility. The main limitation is the lack of a manual UI or real-login
end-to-end check. A useful next step is running the full Organizer → Admin →
Publish → Attendee flow in the desktop app.

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
