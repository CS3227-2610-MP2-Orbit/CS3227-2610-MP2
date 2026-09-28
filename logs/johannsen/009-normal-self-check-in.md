# 009 — Normal attendee self-check-in (#31)

Date: 27 September 2026
Contributor: Johannsen
Branch and starting revision: `attendee-notifications-inbox`, `ab90328`; local
`attendee` fast-forwarded to that committed baseline for the requested work.
Agent/tool: Codex desktop, single agent
Skill used: `.agents/skills/requirements-and-acceptance/SKILL.md` (unchanged)

## Objective

Implement normal self-check-in through the existing registration workflow, with
live-session ownership, timing/version checks, one atomic audit transition and
buttons on the attendee screens. No QR codes or teammate workflow changes.

## Original prompt

The request text is preserved with rich-text emphasis/HTML entities and wrapping
normalized for readability, not as a byte-for-byte transcript.

> **Issue #31: Normal self-check-in**
>
> # Task: Issue #31 — Attendee normal self-check-in
>
> ## Role
> You are the Attendee developer on the MP2 campus event manager (Java 25, JavaFX,
> PostgreSQL). Work on the `attendee` branch. Follow AGENTS.md guardrails.
>
> ## Goal (from issue #31)
> Let attendees check into their registered events using a normal check-in button,
> with permission and timing checks and no duplicate check-ins. No QR codes.
>
> ## Current state (verify before editing)
> - `event_registration` already supports `CHECKED_IN` with `checked_in_at`
>   (a CHECK constraint ties the two together). No command sets it yet.
> - `RegistrationService` handles register/cancel using the live session token,
>   expected-version concurrency, lock order (event → account → booking/venue →
>   registration), and audit + outbox in one transaction. Cancelling a CHECKED_IN
>   registration is already rejected.
> - `RegistrationEvent` exposes `startsAt` and `endsAt`.
> - #29 (My Registrations, `AttendeeRegistrationActions`) and #30 (inbox; the
>   dispatcher claims only REGISTRATION_CONFIRMED, REGISTRATION_CANCELLED and
>   EVENT_ANNOUNCEMENT) are implemented.
> - Joseph's `EventRegistrations` already returns CHECKED_IN attendees.
>
> ## Requirements
> 1. Add a `checkIn(sessionToken, eventId, expectedVersion)` command to the
>    existing registration workflow. Reuse the session guard, lock order, version
>    check and transaction. Do not create a parallel service.
> 2. A repeated or concurrent check-in results in exactly one transition and one
>    `checked_in_at`, with no duplicate audit or outbox effects. A stale version
>    is rejected.
> 3. Record a business audit entry (REGISTRATION_CHECKED_IN) in the same
>    transaction. Do not write it to diagnostic logs.
> 4. UI: show a Check in button on the event details and My Registrations screens
>    only when it is plausibly allowed. The service re-checks every rule, and a
>    stale button must not bypass it. Show a clear message for every outcome
>    (too early, closed, not registered, cancelled, already checked in, expired
>    session).

## Inspection and settled scope

- The actual table is `event_registrations` (plural). Its existing migration
  already ties CHECKED_IN to a non-null timestamp; no migration is currently
  indicated. Existing storage saves timestamps and version.
- `RegistrationService` is the command boundary; event locking serializes its
  writers, with account and booking/venue locks available. No parallel service
  is needed. Existing register/cancel retry semantics accept one-version-behind
  retries; the new check-in requirement explicitly rejects stale versions, so
  those existing commands must not be silently changed to match check-in.
- Required transition is own CONFIRMED → CHECKED_IN with one timestamp/version
  increment and one REGISTRATION_CHECKED_IN business audit in the transaction.
  Rejection leaves state/audit/outbox unchanged. No new check-in notification was
  requested; proposed implementation is audit-only, leaving inbox routing alone.
- `EventCatalogueService` and `AttendeeEventDetailsService` currently restrict
  Browse/details to published events strictly before start. My Registrations
  already lists ongoing bookings with side-by-side details.
- Issue #31's live body repeats the goal but does not settle timing or venue-state
  policy. Log 003 labels earlier policies as proposals, not accepted check-in rules.

## Unresolved policy / implementation pause

The requirements skill and AGENTS.md prohibit silently inventing check-in timing
or permissions. Ask the user to confirm the following proposed rules:

1. Check-in opens exactly at event start (inclusive) and closes at end (exclusive).
   No early or late window.
2. Require the event to remain PUBLISHED with a matching CONFIRMED booking at an
   ACTIVE venue, in addition to active attendee/session and own confirmed record.
3. Include ongoing published events in Browse/details so a during-event button
   is reachable there. Registration itself must still close at start.

No application code or tests were changed before this clarification. These are
proposals, not implemented or accepted behavior. No planned feature was added to
the User Guide and no standalone plan file was created.

## Acceptance criteria and intended verification

- CI-01: Given an authenticated active attendee and own eligible CONFIRMED record,
  check-in with current version records CHECKED_IN/time/version and one audit.
  Real PostgreSQL integration; timing/state eligibility awaits confirmation.
- CI-02: Given concurrent or repeated submissions, at most one transition/audit
  occurs; old versions reject without effects. Real concurrency and retry tests.
- CI-03: Given invalid/expired/wrong-role session, another person's or absent
  registration, cancelled record or stale version, command rejects without
  state/audit/outbox effects. Service and database tests.
- CI-04: Given time boundary or status/venue change while waiting for locks,
  recheck eligibility before writing. Exact boundaries await policy confirmation.
- CI-05: Given an eligible displayed record, both required UI entry points run
  the command off FX, pass its version, prevent double submission and refresh
  with safe feedback. UI previews do not replace service checks. JavaFX smoke.
- CI-06: Given audit/storage failure, transaction rolls back registration and
  timestamp. No new unsupported outbox type is enqueued. Database failure test.

## Commands and evidence

Working directory: `CS3227-2610-MP2`.

- `git status --short`, `git branch -vv`: inspected baseline and unrelated dirty
  skill/evaluation work, which was preserved.
- Read AGENTS.md and requirements, TDD, security/RBAC and desktop-UI skills.
  Only requirements analysis was applied in this phase.
- `gh issue view 31 --json title,body` — exit 0, goal-only issue body.
- Used `rg`/`cat` to inspect the registration service/store/JDBC implementation,
  event snapshot, registration migration, catalogue/details services, guides and
  log 003 for an already accepted timing policy; none was found.
- `git switch attendee && git merge --ff-only attendee-notifications-inbox` —
  exit 0. Local fast-forward only; no remote update or PR merge.

## Outcome and limitations

Partially ready, paused for timing/eligibility/UI visibility confirmation. No
new feature, executed tests or runtime verification is claimed for this phase.
No commit, push or remote PR mutation. Existing #30 code is the baseline, not new
work in this task. Continue here after the user's policy response.

## AI-generated mini reflection

Source inspection exposed a real interaction between a during-event check-in
window and the existing future-only catalogue. Clarifying that boundary first
avoids implementing an unreachable button or silently changing business policy.
The next step is to confirm the rules, then implement/test one shared command.

## Policy confirmation and implementation

Follow-up prompt (whitespace normalized):

> 1. yes
> 2. yes
> 3. yes
>
> ongoing events are shown but registration is closed, so no Register button appears on them. The service already rejects
>
> registration after start.

The user accepted all three proposals above. The pause is resolved; the remaining
record describes implementation on local `attendee` at baseline `ab90328`.

### Skills applied

Used the repository TDD, security/RBAC, database-integrity, desktop-UI and review
skills, all unchanged. Also read the installed
`supabase:supabase-postgres-best-practices` skill and its
`references/lock-short-transactions.md` for the read projection and transaction
boundary. No Supabase runtime/dependency was introduced. These skills led to a
failing command test first, shared deterministic eligibility, owner-derived
identity, database race/rollback checks and real JavaFX smoke verification.

### Implemented decisions

- Added `RegistrationService.checkIn` to the existing service, reusing the
  transaction, session guard, event/account locking helper, strict version helper,
  store and business audit service. Booking/venue locks precede registration
  read/update. Event locking serializes the supported registration writers.
- Command revalidates the session and samples time after lock acquisition. The
  shared pure `CheckInPolicy` requires own CONFIRMED, PUBLISHED, matching confirmed
  active booking and start-inclusive/end-exclusive timing. Capacity does not
  need to be checked again for an existing reserved seat.
- One transition increments version and sets checked-in time once; audit action
  is REGISTRATION_CHECKED_IN, persisted atomically. No check-in outbox type is
  produced. No diagnostic log substitutes for a business audit.
- A repeated old version is stale and rejected; a repeat with the latest version
  reports ALREADY_CHECKED_IN. Neither changes timestamp or effects. Existing
  register/cancel retry semantics are unchanged.
- Details/My Registrations use the same policy for `canCheckIn`. Batch metadata
  uses EXISTS and existing matching-booking/active-venue SQL predicates, separate
  from historical venue display. Legacy projection constructors default the new
  preview flag to false, rather than assuming a booking is eligible.
- Catalogue visibility now includes PUBLISHED events until exact end. Renamed
  the attendee-owned repository method to `findPublishedNotEnded` and updated
  all consumers/fixtures mechanically. Registration remains future-start only.
  Ongoing details have no Register/Re-register node; cancel remains disabled.
- Both check-in buttons use the existing controller's background task, displayed
  version, shared busy gate, allowlisted messages and refreshed details/list.
  Successful check-in is distinct from already-checked-in rejection.
- One sidebar and side-by-side My Registrations details are retained. Check in
  uses the existing blue primary-action style; hiding an unavailable action also
  removes its layout space. The UI clock can be injected for deterministic tests.
- No migration, new table, dependency, QR, location verification, check-in inbox
  notification, attendance-history screen or Organizer/Admin business-rule change.
  All pre-existing skill-evaluation edits remain untouched. No commit or push.

### Changed files in this phase

Under `src/main/java/seedu/eventmanager/`:

- New `registration/CheckInPolicy.java`; updated `registration/RegistrationService.java`.
- Attendee `AttendeeEventDetails`, `AttendeeEventDetailsService`,
  `EventCatalogueRepository`, `EventCatalogueService`, `MyRegistration`,
  `MyRegistrationsService`, `RegistrationEventInfoRepository` and package README.
- Storage `JdbcEventCatalogueRepository` and `JdbcRegistrationEventInfoRepository`.
- UI `AttendeeRegistrationActions`, `AttendeeBrowseController`, `AttendeeBrowseView`,
  `MyRegistrationsView`, `RegistrationFeedback` and `EventManagerApplication`.

Under `src/test/java/seedu/eventmanager/`:

- New `registration/CheckInIntegrationTest.java` and `ui/AttendeeCheckInSmoke.java`.
- Updated `registration/RegistrationServiceTest`, `MyRegistrationsIntegrationTest`,
  `AttendeeEventDetailsIntegrationTest`; attendee `EventCatalogueServiceTest`,
  `JdbcEventCatalogueRepositoryIntegrationTest`, `AttendeeEventDetailsServiceTest`;
  UI `RegistrationFeedbackTest`, `AttendeeBrowseSmoke`, `AttendeeRegistrationSmoke`,
  `AttendeeInboxSmoke` (callback/repository signature updates).

Also `build.gradle`, User/Developer guides and this log. No standalone plan file.

### Executed checks and corrections

All commands below ran in `CS3227-2610-MP2`:

1. `./gradlew test --tests '*RegistrationServiceTest.checkInAtStartRecordsOneTimestampAndAuditWithoutNotification'`
   — exit 1 with the intended missing-behavior failure: the API scaffold threw
   UnsupportedOperationException instead of returning CHECKED_IN.
2. `./gradlew test --tests '*RegistrationServiceTest'` — exit 0 after implementation.
3. Real PostgreSQL command checks — exit 0:

```sh
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_attendee_test_johannsen EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests '*CheckInIntegrationTest' --tests '*RegistrationServiceTest'
```

4. `./gradlew test --tests '*EventCatalogueServiceTest' --tests '*AttendeeEventDetailsServiceTest'`
   — exit 1, two intended failures: ongoing event excluded from list and exact-start
   details rejected. Changed visibility to end-exclusive, without opening registration.
5. Broader attendee/registration checks — exit 0 after read/preview changes:

```sh
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_attendee_test_johannsen EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests 'seedu.eventmanager.attendee.*' --tests 'seedu.eventmanager.registration.*'
```

6. Combined tests/UI command below — JUnit and new check-in smoke passed; command
   exited 1 in existing browse smoke. Its fixed September 25 fixtures disagreed
   with the newly introduced real-time UI cutoff, hiding its Register control.
   Corrected by injecting a shared fixed clock into synthetic browse/registration
   views; kept their action assertions. Production still uses the real clock.

```sh
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_attendee_test_johannsen EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests 'seedu.eventmanager.attendee.*' --tests 'seedu.eventmanager.registration.*' attendeeCheckInUiSmoke attendeeUiSmoke attendeeRegistrationUiSmoke attendeeInboxUiSmoke
```

7. `./gradlew attendeeCheckInUiSmoke attendeeUiSmoke attendeeRegistrationUiSmoke attendeeInboxUiSmoke`
   — exit 0, all four passed after the clock correction. Inspected
   `build/attendee-smoke/check-in-details-1280.png` and
   `check-in-registrations-1000.png`: no duplicate chrome, growing content,
   readable labels, visible blue check-in actions and usable scrollable details.
8. Full build initially failed one of 243 tests: `RegistrationFeedbackTest` still
   expected the old cancellation-specific missing-registration phrase. Updated
   that assertion to the new general "not registered" message and extended the
   same safe-text table with every new check-in rejection code. No business test
   assertion was removed or skipped.

Full build command (disposable DB and UTC setting match earlier verification):

```sh
JAVA_TOOL_OPTIONS=-Duser.timezone=UTC DATABASE_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 DATABASE_USER=mp2_demo_app DATABASE_PASSWORD='' DATABASE_INTEGRATION_TESTS=true EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew build --rerun-tasks
```

### Evidence scope and remaining limits

Database integration exercises exact start/end, last valid instant, expired/
revoked/inactive/wrong-role sessions, missing/other-owned/cancelled registrations,
stale versions, booking/venue changes, separate-connection concurrent requests,
one timestamp/audit, audit rollback and Joseph's unchanged CHECKED_IN roster.
Projection tests verify both previews and closed registration for ongoing events.
Unit tests inject lock-wait time advancement and post-lock session revocation;
they are not real SQL clock-expiry races. The shared session resolver retains
transaction-time expiry semantics. JavaFX smoke checks actual controls with
synthetic callbacks, not real-login/database-connected desktop E2E. Self-check-in
does not independently verify physical presence. No CodeQL/security scan or
remote CI run is claimed. Teammate review should focus on shared composition and
the existing lock/session assumptions before merging.

### Final verification and outcome

The full build rerun after the feedback-test correction exited 0. JUnit XML
totals: **243 tests, 0 failures, 0 errors, 0 skipped**. All four JavaFX smokes
passed as recorded above. `git diff --check` passed. Final review found no new
actionable issue within the inspected scope; no claim of exhaustive security or
real desktop/database E2E verification is made. Final status remained on
`attendee`, with implementation and this log uncommitted/unpushed, and unrelated
skill/evaluation edits preserved. No existing remote PR changed.

Suggested commit message: `feat(attendee): add normal self-check-in (#31)`.

AI-generated implementation reflection: the useful result is one shared command
and policy, with real concurrency/rollback evidence and both UI entry points.
The accepted ongoing-visibility change required updating old future-only tests,
while injectable clocks prevented dated UI fixtures from depending on wall time.
Manual student testing against the configured application database and teammate
review remain the next verification steps; this is not student approval.

## Follow-up: manually created events missing from Browse

Original request:

> i have created 2 events test and test2 but they are not showing up in my attendee view

Used `code-review-and-verification` for a read-only diagnosis; no application
code or database records changed. Inspected `git status --short`, current branch
(`attendee`), the local launcher with credential lines omitted, `EventService`,
`JdbcEventCatalogueRepository`, and `OrganizerEventView`. Existing implementation
and unrelated evaluation changes were preserved.

Read-only database check (exit 0):

```sh
/Applications/Postgres.app/Contents/Versions/17/bin/psql -X -h 127.0.0.1 -p 55448 -U mp2_demo_app -d mp2_demo_johannsen -v ON_ERROR_STOP=1 -c "SELECT title,status,ends_at > CURRENT_TIMESTAMP AS not_ended,status = 'PUBLISHED' AND ends_at > CURRENT_TIMESTAMP AS attendee_visible FROM organizer_event WHERE lower(trim(title)) IN ('testing','testing 2');"
```

Both rows are `DRAFT`, not ended, and not attendee-visible. An earlier exact
title lookup for `test`/`test2` returned zero rows; inspecting event titles revealed
the saved names `testing` and `testing 2`. Source confirms creation sets DRAFT;
Browse requires PUBLISHED and `ends_at > now`. No Organizer publish action was
found in this checkout; its UI explicitly says it does not auto-publish events.
The configured demo launcher targets this database; the running desktop process
was not independently inspected. No test suite or security scan was rerun for
this data/source diagnosis. Several exploratory searches used nonexistent guessed
paths or an unmatched shell glob and exited nonzero; file discovery corrected
those searches. The review skill did not require a policy correction.

Outcome: explain the draft/publication dependency, preserving the intended
Attendee visibility boundary. Publishing is Organizer-owned work; no draft was
exposed or manually published as part of this diagnosis.

## PR preparation (28 September 2026 SGT)

Original request:

> sounds good lets push this as a pr first

Used `code-review-and-verification` for pre-push scope and verification. Ran
`git fetch origin`, `git branch -vv`, `git status --short`, and
`gh pr list --state open --json number,title,headRefName,baseRefName,url`.
PRs #25, #33 and #34 remain open and stacked. Created the dedicated branch with
`git switch -c attendee-self-check-in`, based on the local #34 tip `ab90328`.
The intended PR base is `attendee-notifications-inbox`, avoiding unrelated earlier
features in this diff or updates to PR #25. Unrelated AgenticSE/grader/evaluation
edits remain unstaged. No dependency, migration or Organizer publication changes.

Fresh verification command (exit 0, BUILD SUCCESSFUL):

```sh
JAVA_TOOL_OPTIONS=-Duser.timezone=UTC DATABASE_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 DATABASE_USER=mp2_demo_app DATABASE_PASSWORD='' DATABASE_INTEGRATION_TESTS=true EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew build --rerun-tasks attendeeCheckInUiSmoke attendeeUiSmoke attendeeRegistrationUiSmoke attendeeInboxUiSmoke
```

JUnit XML totals: 243 tests, zero failures/errors/skips. All four JavaFX smoke
tasks reported PASS. `git diff --check` passed. This uses the disposable verification
database, not interactive demo data. UI smokes use synthetic callbacks; no real-login
desktop E2E or security scan is claimed. Existing compiler/JavaFX/Gradle warnings
remain. The skill prompted a fresh rerun and explicit stacked-PR scope; no product
correction was required in this publishing follow-up. The staged diff check then
caught extra trailing blank lines in the two new test files (previously untracked
and absent from the unstaged diff check); removed those whitespace-only lines
before committing. Publishing outcome will be
reported after GitHub confirms creation; student review remains unfilled.

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
