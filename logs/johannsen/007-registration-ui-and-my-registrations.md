# 007 — Register/cancel and My Registrations (#29)

Date: 27 September 2026
Contributor: Johannsen
Branch and starting revision: `attendee`, `ef3b63c`
Agent/tool: Codex desktop, single agent

## Objective

Implement issue #29 using the existing registration backend, independently of
browse PR #25. No check-in, notification inbox, publishing or teammate UI changes.

## Original prompt

The request's text is preserved below; pasted rich-text emphasis/HTML entities
and indentation are normalized for readability (not a byte-for-byte transcript).

> # Task: Issue #29 — Attendee register/cancel and My Registrations
>
> ## Role
> You are the Attendee developer on the MP2 campus event manager (Java 25, JavaFX,
> PostgreSQL). Work on the `attendee` branch. Follow AGENTS.md guardrails.
>
> ## Goal (from issue #29)
> Let attendees register, cancel and re-register for events, and view their
> bookings in My Registrations.
>
> ## Current state (verify before editing)
> - Backend exists: `RegistrationService.register / cancel / myRegistrations`
>   (session-token based, expected-version concurrency, audit + outbox in one
>   transaction). Do not rewrite it; reuse it.
> - Browse details (#28) already show own status, remaining seats and
>   eligibility messages. `AttendeeBrowseView` has a placeholder:
>   "Register/cancel controls will be added in the next feature."
> - `AttendeeBrowseController` owns background tasks and stale-result handling.
>
> ## Business rules (accepted policies in docs/AttendeePlan.md)
> - P2: An authenticated ATTENDEE registers only themself, strictly before start,
>   while capacity remains. A future PUBLISHED event, a matching CONFIRMED booking
>   and an ACTIVE venue are required. No waitlist.
> - P3: The owner may cancel a CONFIRMED registration strictly before start.
>   Cancelling at or after start, or cancelling a CHECKED_IN registration, is
>   rejected. Re-registration is allowed if the attendee is currently eligible and a
>   seat remains.
> - P6: Cancelled registrations stay visible in My Registrations.
> - Rules are enforced in the service; the UI only reflects them.
>
> ## Requirements
> 1. Add Register and Cancel actions to the event details panel. Pass the current
>    own-registration version as expectedVersion (-1 when no record exists).
> 2. Show a clear success or rejection message for every service outcome, including
>    full, closed, stale version and expired session. Refresh details afterwards.
> 3. Prevent double submission while a command is running. Run commands off the FX
>    thread through the controller.
> 4. Add a My Registrations screen listing the attendee's own registrations
>    (upcoming, past and cancelled) with event title, SGT start time, venue and
>    status. If this needs a new read projection, reuse RegistrationReadSql and
>    existing patterns; do not add tables or edit applied migrations.
> 5. Allow cancelling from My Registrations with the same rules as the details panel.
>
> This is a seperate PR from #25

## Decisions and scope

- The user had deleted `AttendeePlan.md`; the policies in this request are the
  authority. No standalone plan was recreated or committed.
- Inspection confirmed PR #25 is still OPEN, `attendee` → `main`. Pushing this
  branch to its upstream would add #29 to #25. Asked whether to publish a stacked
  `attendee-registration-ui` → `attendee` PR or keep changes local until #25 merges.
  No answer received at the time of this record; no commit/push/new PR performed.
- Reused `RegistrationService` unchanged. Own details now carry the database
  version; CANCELLED is an existing record, never the absent-record sentinel.
- One controller command gate covers both screens; controls/navigation disable
  during a write. Reads remain cancellable and stale responses are discarded.
  Closing the app does not promise rollback of a command already sent to PostgreSQL.
- `MyRegistrationsService` calls the existing owner-only read and batch-enriches
  its results, then revalidates the session. It does not use the future-only catalogue.
  Cancellation availability is advisory; the command service rechecks all rules.
- Venue projection uses current CONFIRMED/AT_RISK booking, otherwise latest historic
  booking, with deterministic tie-breaking. It is not a historical venue snapshot.
- No schema/migration/dependency changes. Unrelated AgenticSE/evaluation work and
  private demo data were preserved. Verification used separate disposable databases.

## Skills and observable influence

Read repository requirements-and-acceptance, test-driven-implementation,
security-and-rbac, database-migration-and-integrity, desktop-ui-polish and
code-review-and-verification skills. Also read installed
supabase-postgres-best-practices and its short-transactions reference for PostgreSQL
read-query work (no hosted Supabase involved).

The skills led to tests before behavior, explicit owner/session revalidation,
batch metadata reads without holding command locks, one role sidebar, and separate
claims for PostgreSQL integration versus synthetic JavaFX interaction evidence.
No skill files were changed; student review is not inferred.

## Files changed

- `attendee`: versioned details/snapshot/service; new `MyRegistration`,
  `MyRegistrationsService`, `RegistrationEventInfoRepository`.
- `storage`: details version selection, `RegistrationReadSql` display-booking
  helper, new `JdbcRegistrationEventInfoRepository`.
- `ui`: Attendee controller/view, session-bound composition in
  `EventManagerApplication`, new `AttendeeRegistrationActions`,
  `MyRegistrationsView`, `RegistrationFeedback`.
- Tests: details fixtures/version coverage, reusable registration DB fixture,
  new owner-projection service/integration tests, feedback tests and JavaFX
  command smoke; existing browse smoke extended without removing assertions.
- `build.gradle`: opt-in registration UI smoke task only, no new dependencies.
- `docs/UserGuide.md`, `docs/DeveloperGuide.md`, this interaction record.

## Commands actually executed and results

Working directory: `CS3227-2610-MP2` under the shared school workspace.

Read-only inspection included `git status --short`, `git branch --show-current`,
`git diff --check`, source/skill reads and:

```sh
gh pr view 25 --json state,headRefName,baseRefName,mergedAt
```

Result: OPEN, head `attendee`, base `main`, no merge. `git diff --check` passed.

### Red/green evidence

```sh
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_attendee_test_johannsen EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests '*AttendeeEventDetailsIntegrationTest.carriesOwnVersionIncludingCancelledInsteadOfTreatingItAsAbsent'
```

Exit 1: with an API-only sentinel scaffold, cancelled own version expected 7,
actual -1. Added version to the SQL/snapshot/model, then the same test passed in:

```sh
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_attendee_test_johannsen EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests '*AttendeeEventDetailsIntegrationTest.carriesOwnVersionIncludingCancelledInsteadOfTreatingItAsAbsent' --tests '*MyRegistrationsServiceTest'
```

Overall exit 1: version test passed; four new MyRegistrations tests failed against
an empty-list scaffold (missing rows and missing session rejection). Implemented
enrichment/guard logic and ran:

```sh
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_attendee_test_johannsen EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests '*MyRegistrationsServiceTest' --tests '*MyRegistrationsIntegrationTest.listsOnlyOwnerIncludingPastUnpublishedCancelledAndCompletedVenue'
```

After the fixture/compiler corrections below: service tests passed; integration
failed because the repository scaffold returned no event metadata. Implemented
the batch query, then:

```sh
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_attendee_test_johannsen EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests 'seedu.eventmanager.attendee.*' --tests 'seedu.eventmanager.registration.*'
./gradlew attendeeUiSmoke
```

Focused unit/PostgreSQL suite: exit 0. Browse UI smoke initially exited 1 at
`registration actions must replace the placeholder`, before adding controls.
After wiring, the same smoke passed. New command smoke also passed:

```sh
./gradlew attendeeRegistrationUiSmoke
./gradlew attendeeUiSmoke attendeeRegistrationUiSmoke
```

### Full verification

Created a dedicated verification database (not the populated interactive demo):

```sh
/Applications/Postgres.app/Contents/Versions/17/bin/createdb -h 127.0.0.1 -p 55448 -U mp2_bootstrap -O mp2_demo_app mp2_registration_ui_verify_20260927
JAVA_TOOL_OPTIONS=-Duser.timezone=UTC DATABASE_URL=jdbc:postgresql://127.0.0.1:55448/mp2_registration_ui_verify_20260927 DATABASE_USER=mp2_demo_app DATABASE_PASSWORD='' DATABASE_INTEGRATION_TESTS=true EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_registration_ui_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew build --rerun-tasks
```

Both exited 0. Full build rerun after additional owner/venue regression tests:
**216 tests, 0 failures, 0 errors, 0 skipped**, counted from JUnit XML.
UTC is the documented workaround for pre-existing venue timestamp-offset equality
tests; app SGT display behavior is unchanged. Compiler unchecked-operation,
Gradle deprecation and JavaFX unnamed-module warnings remain.

UI smoke uses actual JavaFX controls with synthetic callbacks, not a real DB or
login E2E. It verifies displayed versions (-1, 0, 1, 2), duplicate suppression,
off-FX execution, both cancellation entry points, past/cancelled rows, empty list,
full/closed/stale/expired feedback and refresh. Separate real PostgreSQL tests
verify owner-only results, one record after re-registration and three atomic
audit/outbox effects. Existing backend concurrency/rollback tests remain unchanged.

Inspected snapshots in `build/attendee-smoke/` at 1280px and 1000px. One sidebar,
width-filling content, wrapped labels and visible My Registrations actions; browse
details scroll at smaller sizes. Corrected the active navigation highlight and
cleared old command feedback when changing pages/selected events.

## Corrections and limitations

- Initial new integration test referred to nonexistent `CONFIG`; corrected to
  fixture `configuration`. This compilation error was not claimed as TDD red.
- Repeated synthetic venue name/location violated a fixture uniqueness constraint;
  removed an unnecessary second booking and later added a named-venue helper for
  multi-booking coverage. Fixture failure was not claimed as product red.
- Moving shared test helpers exposed an existing subclass `long count` signature;
  aligned the common helper. No existing test assertions were weakened.
- No new lint/security tooling exists in the inspected configuration; compilation,
  tests and diff review were performed. No external teammate review or remote CI
  run for these unpushed changes is claimed.
- No publishing UI, normal check-in, notification inbox or attendance-history
  feature added. Fresh databases still need eligible published events to register.
- Full login-to-database desktop E2E and human acceptance review are not performed.
- Branch publication remains pending the separate-PR choice; #25 is unchanged.

Suggested commit message: `feat(attendee): register, cancel and view my registrations`

## AI-generated mini reflection

The key integration risk was preserving the displayed registration version,
especially after cancellation. Small failing tests caught that gap, while the
shared command controller kept both UI entry points consistent. UI and database
evidence are complementary, not interchangeable; a human desktop walkthrough
and teammate review remain useful before merge.

## Follow-up — filters, sorting and registration event-details tab

Original prompt (verbatim):

> Looks good, I want a way to sort my registration, looking at past events, cancelled events, ongoing events and all. Pressing it should also open up a tab where we see the event details as well.

Continued locally on `attendee`, preserving the uncommitted #29 work and unrelated
skill-evaluation edits. This follow-up did not resolve the separate-PR publication
choice, so no remote changes were made.

Assumptions announced before implementation: Upcoming is before start; Ongoing
includes start and excludes end; Past is from end onward. Cancelled bookings are
in Cancelled and All only. Added Upcoming plus earliest/latest start sorting as
part of organizing the list. These are presentation categories, not new states
or changes to cancellation/check-in policy.

Acceptance: every category has deterministic boundaries; All retains all rows;
sorting is stable and does not mutate the source list; selecting a past/cancelled
booking opens full event details; filters survive refresh and stale personal
details clear on filter changes/loading/failure.

Implementation: extended existing owner-only metadata projection with end time,
club, description and event status in its existing batch query. Added pure
`RegistrationListQuery`, filter/sort controls and reusable Bookings/Event details
tabs. No new database access from UI, no new route exposing arbitrary event IDs,
no migrations and no changes to RegistrationService or teammate screens.
Reads still go through the existing owner/session guards.

Skills used again: requirements-and-acceptance, test-driven-implementation,
desktop-ui-polish, security-and-rbac, database-migration-and-integrity,
code-review-and-verification, and PostgreSQL best-practices for the narrow read
projection extension. They shaped boundary tests, reuse of authenticated data,
and single-chrome/wrapping layout. No skill revisions were needed.

Commands and observed results (same repository working directory):

```sh
./gradlew test --tests '*RegistrationListQueryTest'
./gradlew test --tests '*RegistrationListQueryTest' attendeeRegistrationUiSmoke
./gradlew attendeeRegistrationUiSmoke
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_attendee_test_johannsen EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests '*RegistrationListQueryTest' --tests '*MyRegistrationsServiceTest' --tests '*MyRegistrationsIntegrationTest' attendeeRegistrationUiSmoke
```

First command exit 1: three assertion failures against an unfiltered/unsorted API
scaffold. Second: query tests green, UI intentionally red at
`registration details tab required`. After implementation, both subsequent
commands exited 0. The smoke now also verifies all five filters, both sort orders,
opening past/cancelled details, full SGT/club/description fields, refresh retaining
filters, no-match state, and clearing details on failed reads. Existing command
assertions remain; fixture selections now use event IDs rather than list indices
because ordering is intentionally configurable.

Inspected `registration-filters-1000.png` and `registration-details-1000.png`
under ignored `build/attendee-smoke/`: readable wrapped labels, growing content,
clear filter/sort and cancel actions, and a single reusable details tab. The role
sidebar remains the only role chrome. These screenshots use synthetic data.
Re-ran the exact full-build command in the Full verification section above against
the same disposable verification database: exit 0, **219 tests, 0 failures,
0 errors, 0 skipped**. The documented UTC test workaround remains in use.
Re-ran `./gradlew attendeeUiSmoke attendeeRegistrationUiSmoke`: both passed, exit 0.
`git diff --check` and `git diff --exit-code -- src/main/java/seedu/eventmanager/registration/RegistrationService.java src/main/resources/db`
both exited 0. The latter confirms command rules and migrations remain unchanged.
Also inspected the 1000px full-role snapshot with sidebar and event details:
controls fit, text wraps, and the detail content scrolls without adding a second
role shell. No human review or remote CI is implied by these local checks.

Limitations: event metadata remains a current snapshot, not historical archive;
time filters recompute on interaction/refresh rather than on a timer. No live
login-to-PostgreSQL UI E2E or student approval is claimed.

## Follow-up — side-by-side bookings and details

Original prompt (verbatim):

> for my registration, for bookings, i want it the same as browsing events when it comes to seeing the events details a tab side by side and not a new tab.

The user supplied a Browse events screenshot showing a left event list and right
details card. The screenshot was a layout reference; its demo event content was
not treated as new business requirements. This clarification supersedes the
earlier separate-tab UI, whose implementation/test evidence above is historical.

Used desktop-ui-polish, test-driven-implementation and code-review-and-verification
skills. Matched Browse's horizontal SplitPane, 42% initial list share, minimum
pane widths, scrollable/wrapped details and existing shared sidebar. No new
navigation chrome, commands, schema or teammate UI changes. Filters, sorting,
owner-only projection and cancellation callbacks are retained. Selection updates
details beside the list; filtering/loading/errors clear the old details to an
explicit selection prompt. Removed tab-only click/Enter handlers since ordinary
list selection now keeps the adjacent pane current.

Changed only `MyRegistrationsView`, its existing JavaFX smoke, User/Developer
Guides and this log in this follow-up. Existing uncommitted work was preserved.

Verification commands (same repository working directory):

```sh
./gradlew attendeeRegistrationUiSmoke
./gradlew classes attendeeUiSmoke attendeeRegistrationUiSmoke
./gradlew attendeeRegistrationUiSmoke
git diff --check
```

First smoke run exited 1 at the new assertion
`bookings and details must share a side-by-side split pane`, before changing
production UI. After replacing the tabs, compilation and both UI smokes passed.
The updated smoke checks the list is left of the detail pane in scene coordinates,
no TabPane exists, all filters still select the right event, cancellation still
uses the displayed record, and refresh/failure clears old private details.

Inspected 1000px full-role and cancelled-registration snapshots in ignored
`build/attendee-smoke/`: list and details stay visible together, controls fit,
labels wrap, details scroll, one sidebar and shared colors remain. A synthetic
fixture screenshot initially captured stale ScrollPane rendering despite passing
node assertions; the test now waits for two JavaFX rendering pulses before that
capture, and the resulting screenshot was inspected. This is synthetic UI
evidence, not login-to-database E2E.

The exact full-build command from the Full verification section was rerun using
the same disposable PostgreSQL database and documented UTC test workaround.
Result: exit 0, **219 tests, 0 failures, 0 errors, 0 skipped** from JUnit XML.
Final whitespace check passed; the source/documentation search found no remaining
TabPane implementation or current separate-tab instructions in the changed UI/guides.
No commit or push was performed.

## Publication request

Original prompt (verbatim):

> looks good, please push it as a new pr

Read AGENTS.md and code-review-and-verification before preparing the scoped commit.
`git fetch origin` succeeded; `git rev-list --left-right --count HEAD...origin/attendee`
returned `0 0`. PR #25 remains OPEN with head `attendee` and base `main`.
No local/remote `attendee-registration-ui` branch or existing PR for it was found.

To honor the separate-PR request, publish the feature from the new
`attendee-registration-ui` branch targeting `attendee` (stacked on #25).
After #25 merges, retarget this PR to main before merging the registration work.
Do not push this feature into #25's remote head.

Only the attendee feature, its tests/build task, User/Developer Guides and this
record are in publication scope. Unrelated AgenticSE/grader/evaluation edits and
private demo data remain excluded. `git diff --check` passed; the existing JUnit
report still records the preceding verified full build: 219 tests, no failures,
errors or skips. These report counts are prior executed verification, not a claim
that tests were rerun merely to create the PR. Student review remains unfilled.

## Student review (unfilled)

- [ ] I confirmed that the original prompt text is accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
