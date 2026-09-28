# 013 — Explain check-in availability (#38)

Date: 28 September 2026 (SGT)
Contributor: Johannsen
Branch and starting revision: attendee-check-in-availability, 509801f
Agent/tool: Codex
Skills used: .agents/skills/test-driven-implementation/SKILL.md,
.agents/skills/code-review-and-verification/SKILL.md,
.agents/skills/desktop-ui-polish/SKILL.md, read at starting revision 509801f.
No skill source changes.

## Objective

Expose the existing CheckInPolicy result in attendee read models and explain it
in Browse details and My Registrations. Preserve the command's authority,
policy order, timing, permissions and existing tests. Do not commit, push or
create a PR.

## Original prompts (verbatim)

Pasted formatting and HTML entities normalized; wording retained.

> # Task: Issue #38 — Attendee explain check-in availability
>
> ## Role
> You are the Attendee developer on the MP2 campus event manager (Java 25, JavaFX,
> PostgreSQL). Start a new branch from the latest Attendee branch (verify which one
> has #31 first). Follow AGENTS.md guardrails.
>
> ## Goal (from issue #38)
> Explain check-in availability in event details and My Registrations, including
> when check-in opens, when it is closed, or when the venue is unavailable,
> instead of only hiding the button.
> Scope: Attendee UI/read model. Reflect the existing check-in rules; keep the
> service authoritative and do not change timing or permissions.
>
> ## Current state (verify before editing)
> - `CheckInPolicy.evaluate` (pure, shared by the command and previews) returns:
>   AVAILABLE, NOT_REGISTERED, CANCELLED, ALREADY_CHECKED_IN, TOO_EARLY, CLOSED
>   or VENUE_UNAVAILABLE. Order: registration status, then published/ended
>   (CLOSED), then before start (TOO_EARLY), then booking/venue.
> - Read models expose only `boolean canCheckIn` (`AttendeeEventDetails`,
>   `MyRegistration`). The UI hides the button unless canCheckIn is true and
>   `startsAt <= now < endsAt` (`AttendeeBrowseView:315`, `MyRegistrationsView:65`).
> - `RegistrationFeedback` already has messages for the command errors
>   CHECK_IN_TOO_EARLY, CHECK_IN_CLOSED and CHECK_IN_VENUE_UNAVAILABLE.
>
> ## Fixed rules (do not change)
> - Check-in is open only while startsAt <= now < endsAt.
> - Requires own CONFIRMED registration, active attendee and session, a PUBLISHED
>   event, a matching CONFIRMED booking and an ACTIVE venue.
> - `RegistrationService.checkIn` stays authoritative. The preview is advisory only.
>
> ## Out of scope
> Changing timing, eligibility or permissions, changing CheckInPolicy order,
> notifications, the club work in #37, and new dependencies.
>
> ## Acceptance criteria
> - For every CheckInPolicy.Result, both screens show the matching explanation, with
>   SGT times at the boundaries (just before start, at start, just before end, at end),
>   using a controlled clock.
> - The preview result equals what `RegistrationService.checkIn` would return for the
>   same state (same policy, tested at both levels).
> - A stale AVAILABLE screen still gets the service rejection message on click.
> - Existing check-in and registration tests pass unchanged.
>
> ## Process
> - Skills: test-driven-implementation, code-review-and-verification, desktop-ui-polish.
> - TDD with red results recorded first. Use a controlled clock for the boundaries,
>   and the existing JavaFX smoke harness for real UI evidence.
>
> ## Verification and reporting
> - Run the focused tests and then the full `./gradlew build`, and report the exact
>   commands, exit codes and test counts. Label evidence as unit, database or real UI.
> - Create logs/johannsen/013-explain-check-in-availability.md from
>   logs/templates/interaction.md.
>   Leave the student-review boxes unfilled.
> - Update the UserGuide (the check-in messages) only for implemented behaviour.
> - Do not commit, push or open a PR until I say so.

## Response summary

Created a new branch from fetched origin/attendee-club-filter (509801f). Git
ancestry confirms #31 (c08a06b) is included, alongside later #32/#37 work.
Preserved unrelated AgenticSE/grader/evaluation changes and untracked review
log 011.

Both services now preserve the full CheckInPolicy.Result instead of reducing it
to a boolean. New result-aware constructors derive canCheckIn consistently.
Existing boolean-only constructors remain source-compatible so pre-existing
tests/callers are unchanged; these cannot supply a reason, so the formatter
gives a neutral refresh message rather than inventing one. All production
detail/registration reads use the non-null result-aware constructors.

Both UI panes use one shared presentation formatter. Every explanation includes
the SGT window, inclusive start/exclusive end, and a snapshot/refresh reminder.
The formatter mirrors the existing button's display-time guard for a stale
AVAILABLE snapshot but never promotes stale unavailability to authorization.
No live timer or new business rule was added.

## Assumptions and design decisions

The request's fixed rules are team requirements, not newly inferred policy.
RegistrationService and CheckInPolicy are untouched. Previews still use the
same policy with a controlled/injected clock; command errors still use the
unchanged RegistrationFeedback.

The existing public-catalogue boundary was retained and explained in commentary:
fresh Browse details do not expose ended/unpublished events. CLOSED is tested
on the already-open detail renderer and the unrestricted owner-only bookings
view, without changing catalogue visibility. Likewise, a never-registered
event has no My Registrations row. Its NOT_REGISTERED renderer case uses a
synthetic row only to verify exhaustive message presentation; real reads are
tested to return an empty owner list.

Acceptance coverage:

| Criterion | Evidence |
| --- | --- |
| All seven explanations, SGT boundaries on both panes | Formatter unit test and real JavaFX smoke |
| Same policy result as command | 11 unit scenarios and 11 real PostgreSQL scenarios |
| Just before start, at start, just before end, at end | Controlled nanosecond clocks in unit/DB/UI tests |
| Correct status/time/venue precedence | Cancelled/checked-in ended cases, unpublished-before-start, inactive venue before start/at end |
| Stale AVAILABLE still rejected | Real command service invoked from both real JavaFX controls after advancing clock to end, zero audits |
| Existing tests unchanged | No tracked test-file edits; full 279-test suite and original check-in smoke pass |

The added database fixtures exercise existing adapters/commands; no production
SQL, schema, migration, locking or session code changed. The database clock
still governs live sessions as before; the controlled clock governs check-in
window decisions. Prior review's session-race defects are out of scope.

## Files changed

Modified:
- build.gradle: new opt-in attendeeCheckInAvailabilityUiSmoke task, using the
  existing JavaExec/JavaFX smoke pattern; no new dependency.
- docs/UserGuide.md: implemented explanations, SGT window and snapshot limits.
- src/main/java/seedu/eventmanager/attendee/AttendeeEventDetails.java
- src/main/java/seedu/eventmanager/attendee/AttendeeEventDetailsService.java
- src/main/java/seedu/eventmanager/attendee/MyRegistration.java
- src/main/java/seedu/eventmanager/attendee/MyRegistrationsService.java
- src/main/java/seedu/eventmanager/ui/AttendeeBrowseView.java
- src/main/java/seedu/eventmanager/ui/MyRegistrationsView.java

New:
- src/main/java/seedu/eventmanager/ui/CheckInAvailabilityText.java
- src/test/java/seedu/eventmanager/ui/CheckInAvailabilityTextTest.java
- src/test/java/seedu/eventmanager/ui/CheckInAvailabilitySmoke.java
- src/test/java/seedu/eventmanager/registration/CheckInPreviewParityTest.java
- src/test/java/seedu/eventmanager/registration/CheckInPreviewIntegrationTest.java
- logs/johannsen/013-explain-check-in-availability.md

No edits to existing test files, CheckInPolicy, RegistrationService,
RegistrationFeedback, repositories, migrations, club workflows or notifications.
No pre-existing unrelated file changes are part of #38.

## Commands actually executed

Working directory throughout:
`/Users/johannsenlum/Documents/School/Y4S1/CS3227 Agentic Software Engineering/CS3227-2610-MP2`.

Branch verification (exit 0):
```sh
git fetch origin
git branch -vv
git merge-base --is-ancestor origin/attendee-self-check-in origin/attendee-club-filter
git log -5 --oneline origin/attendee-club-filter
git switch --no-track -c attendee-check-in-availability origin/attendee-club-filter
```

Read AGENTS.md, the three required skills, log template, existing policy/services/
views, shared shell, UserGuide and test/build patterns using cat, sed and rg.
Two guessed test filenames (CheckInPolicyTest.java and CheckInServiceTest.java)
were absent; rg located actual coverage in RegistrationServiceTest and
CheckInIntegrationTest. All new files/edits used apply_patch.

### Unit red/green

Added minimal DTO result-accessor and empty formatter scaffolding so tests compile.
No policy-result propagation or message behavior was implemented at the red step.

```sh
./gradlew test --tests '*CheckInPreviewParityTest' --tests '*CheckInAvailabilityTextTest' --rerun-tasks
```

First run: **exit 1, 13 tests, 13 assertion failures**. Eleven preview scenarios
expected the explicit policy result but received null; two formatter tests
expected explanations but received empty text. These were intended behavioral
failures, not missing dependencies or compilation errors.

Implemented result propagation/shared messages and reran the same command:
**exit 0, 13 tests passed**.

### Real UI red

Added the new JavaFX harness/task before adding explanation labels:
```sh
./gradlew attendeeCheckInAvailabilityUiSmoke
```

**Exit 1** at "visible check-in explanation on #attendee-check-in-availability".
This is the intended absent-label failure. The harness uses real controls,
controlled time and the real RegistrationService, with synthetic reads/storage.

### Focused green: unit, PostgreSQL and UI

After UI implementation, added a third formatter boundary guard case and
PostgreSQL parity scenarios:
```sh
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests '*CheckInPreview*' --tests '*CheckInAvailabilityTextTest' attendeeCheckInAvailabilityUiSmoke --rerun-tasks
```

**Exit 0**, BUILD SUCCESSFUL in 8s. XML: **25 tests, 0 failures/errors/skips**:
14 unit invocations (11 parity + 3 formatter), 11 real PostgreSQL invocations.
Real JavaFX smoke PASS separately from that JUnit count.

### Broad build and UI run, including an unrelated smoke failure

```sh
JAVA_TOOL_OPTIONS=-Duser.timezone=UTC DATABASE_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 DATABASE_USER=mp2_demo_app DATABASE_PASSWORD='' DATABASE_INTEGRATION_TESTS=true EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew build --rerun-tasks attendeeCheckInAvailabilityUiSmoke attendeeHistoryUiSmoke attendeeCheckInUiSmoke attendeeUiSmoke attendeeRegistrationUiSmoke attendeeInboxUiSmoke
```

Build/test/check tasks passed: **279 tests, 0 failures/errors/skips**.
Availability, History, Check-in, Browse and Registration UI tasks passed.
The final unchanged Inbox UI task failed at "Read initially has no messages",
so the **combined command exited 1**. This is not recorded as an all-green
combined run. No Inbox source or test edit was made.

Inspected the existing Inbox smoke's initial filter interaction and view loading/
filter code. A timing/initialization issue was suspected but not established.
An isolated diagnostic rerun, with no assertion change:
```sh
JAVA_TOOL_OPTIONS=-Duser.timezone=UTC ./gradlew attendeeInboxUiSmoke
```
**Exit 0**, Inbox UI smoke PASS. Cause remains unconfirmed; no fix is claimed.

### UI evidence correction

The first Browse screenshot was rendered using a synthetic direct detail callback
with an empty event list, an unrealistic fixture presentation. Updated only the
new harness to populate/select the event and exercise normal Browse loading;
ended-event renderer cases remain explicit fixtures to preserve catalogue rules.

```sh
./gradlew attendeeCheckInAvailabilityUiSmoke
```
**Exit 0**, new smoke PASS after the fixture correction.

### Separate final full build and required classes check

```sh
JAVA_TOOL_OPTIONS=-Duser.timezone=UTC DATABASE_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 DATABASE_USER=mp2_demo_app DATABASE_PASSWORD='' DATABASE_INTEGRATION_TESTS=true EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew build --rerun-tasks
./gradlew classes
```

Full build **exit 0**, BUILD SUCCESSFUL in 32s.
Fresh XML totals: **279 tests, 0 failures, 0 errors, 0 skipped**.
Classes **exit 0**, up to date.

## Actual verification results

- Unit: 14 new passing cases, plus existing suite.
- Database: 11 new passing real PostgreSQL cases in disposable per-test schemas;
  previews produce no audit/outbox writes, rejected commands preserve status,
  accepted commands produce one audit and no notification.
- Real UI: both actual panes, all seven message results, controlled boundary
  instants, wrapping SGT windows, existing button rules and stale service
  rejection. Synthetic storage/read callbacks; not real-login/database UI E2E.
- Broader: 279 JUnit tests pass; all six smoke tasks have passing executions,
  but the combined run encountered the intermittent Inbox failure noted above.

Screenshots inspected using view_image:
- build/attendee-smoke/check-in-availability-browse-1280.png (re-inspected after fixture correction)
- build/attendee-smoke/check-in-availability-registrations-1000.png

Desktop-polish before/after checklist:
1. Dual chrome/width: preserved single sidebar and resizable side-by-side panes.
2. Labels: previously no reason; now wrapping explanation with SGT start/end.
3. Primary actions: existing blue Check in and refresh controls unchanged.
4. Shared tokens: same page/card/nav colors; no new role styling.
5. Non-goals: no check-in window, permission, club, notification or Organizer changes.

## Problems, corrections, and skill revisions

TDD recorded real failing assertions before implementing the read-model/messages
and labels. No existing assertion was weakened. Review distinguished the
green build from the failing combined smoke invocation. UI-polish inspection
caught and corrected the synthetic screenshot setup, not a product defect.

The UI skill's historical note about no JavaFX interaction framework is no longer
a reason to claim no UI evidence: this checkout has custom real-control smoke
harnesses, and the user explicitly requested their use. No framework/dependency
was added and no skill file was rewritten.

No security scan, performance benchmark, cross-platform run or remote CI.
Existing unchecked compilation, JavaFX unnamed-module and Gradle warnings remain.
No interactive demo data was changed. Existing unrelated working-tree changes
were preserved.

## Outcome and limitations

#38 implemented within Attendee UI/read models on attendee-check-in-availability.
No commits, pushes, PRs or issue changes made. All student-review fields remain
unfilled. Existing tests are unchanged; command/policy and all timing/permission
rules remain authoritative. Fresh Browse visibility and missing-owner-row limits
are stated explicitly, rather than changing them to manufacture UI cases.

Suggested commit message: `feat(attendee): explain check-in availability (#38)`.

## AI-generated mini reflection

Keeping the actual policy result in the read model makes explanations consistent
without moving authorization into JavaFX. Controlled clocks and real database
parity tests establish the boundaries; real UI checks show the explanation and
command rejection together. The intermittent unrelated Inbox smoke failure
limits the claim to recorded passing executions, not a wholly green combined run.

## Publishing follow-up

Original user request:

> pr it please.

This authorizes committing, pushing and opening a PR, not merging. Applied
`code-review-and-verification` for the publishing scope and final checks. The
earlier no-publication outcome above describes the implementation phase.

`git fetch origin` exited 0; `git rev-list --left-right --count
HEAD...origin/attendee-club-filter` returned `0 0`. GitHub's open PR list confirms
#39 still targets attendee-attendance-history. This change will therefore target
attendee-club-filter, keeping the #38 diff separate from #37 and the earlier stack.

Final pre-publication verification:

```sh
JAVA_TOOL_OPTIONS=-Duser.timezone=UTC DATABASE_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 DATABASE_USER=mp2_demo_app DATABASE_PASSWORD='' DATABASE_INTEGRATION_TESTS=true EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew build --rerun-tasks attendeeCheckInAvailabilityUiSmoke
```

Exit 0: 279 JUnit tests, 0 failures/errors/skips, plus the real JavaFX availability
smoke PASS (synthetic reads/store, not database UI E2E). The earlier unrelated
Inbox smoke failure and unchanged successful isolated rerun remain disclosed;
this final command did not rerun Inbox. `git diff --check` exited 0.

Source review found no actionable publishing blocker: no changed command/policy,
applied migrations, dependencies, or existing tests. Unrelated AgenticSE/evaluation
work and untracked log 011 are excluded. Only the 14 #38 implementation,
verification, UserGuide and log files are selected. Remote CI is not yet verified.
Student-review fields remain unfilled.

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
