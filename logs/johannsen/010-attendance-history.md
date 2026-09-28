# 010 — Attendee attendance history (#32)

Date: 28 September 2026 (SGT)
Contributor: Johannsen
Branch and starting revision: `attendee`, fast-forwarded locally from `ab90328` to `c08a06b` (#31)
Agent/tool: Codex
Skills: repository requirements-and-acceptance, test-driven-implementation, and
code-review-and-verification at `c08a06b`; installed
supabase-postgres-best-practices (query-missing-indexes and data-n-plus-one references).

## Objective

Implement a separate owner-only attendance view backed by actual #31 check-ins.
Preserve unrelated AgenticSE, grader and evaluation changes. No commits/pushes/PR
updates authorized by this task. The previous task's publish authorization was
already fulfilled by PR #35.

## Original prompt

The request below preserves its wording; pasted indentation is normalized.

> **Issue #32: Attendance history**
>
> # Task: Issue #32 — Attendee attendance history
>
> ## Role
> You are the Attendee developer on the MP2 campus event manager (Java 25, JavaFX,
> PostgreSQL). Work on the `attendee` branch. Follow AGENTS.md guardrails.
>
> ## Goal (from issue #32)
> Let attendees view their own attendance history with event details and
> check-in times.
>
> ## Current state (verify before editing)
> - Attendance is recorded by #31 as `event_registration.status = 'CHECKED_IN'`
>   with `checked_in_at`. Confirm #31 is implemented in this checkout first. If
>   it is not, stop and tell me.
> - My Registrations (#29) already lists upcoming, past and cancelled
>   registrations. History is a separate, narrower view, not a copy of it.
> - Existing read patterns: `RegistrationReadSql`, `JdbcAttendeeEventDetailsRepository`
>   (single-statement snapshot, statement timeout, no write locks) and
>   `AttendeeBrowseController` (background reads, stale-result handling).

## Sources, decisions and acceptance criteria

Verified `RegistrationService.checkIn` and existing #31 tests at `c08a06b` before
editing. The actual table is `event_registrations` (plural); its migration ties
CHECKED_IN to non-null `checked_in_at` and prevents duplicate event/owner rows.
Read `RegistrationReadSql`, `JdbcAttendeeEventDetailsRepository`,
`AttendeeSessionGuard`, Browse/Inbox controllers and application composition.

Required by the user: own attendance only, actual check-in times, separate from
My Registrations, JavaFX/PostgreSQL and existing read/concurrency conventions.
Implementation defaults stated to the user before building: newest check-in first,
SGT display, side-by-side details, no commands or new tables. History includes an
ongoing event once checked in and retains ended/no-longer-published events. It is
not filtered by current registration eligibility. Event/venue details are current
metadata, not frozen check-in-time snapshots; the UI explains this explicitly.
No assignment requirements beyond the user-provided task are inferred.

Readiness: ready; no new permission or check-in policy decision is required.

| Criterion | Given / When / Then | Verification |
| --- | --- | --- |
| HIST-01 | Given a live ATTENDEE session, when history loads, only that owner's CHECKED_IN rows and stored timestamps appear; confirmed/cancelled/other-owner rows do not. | PostgreSQL integration |
| HIST-02 | Given completed/ongoing checked-in events and changed/missing venues, history remains available, newest check-in first, with at most one row per registration. | PostgreSQL integration |
| HIST-03 | Given invalid, expired, revoked, inactive or wrong-role sessions, reads reject; a session changed/revoked during a read cannot return private results. | Unit + PostgreSQL integration |
| HIST-04 | Given loaded history, selecting a row shows SGT check-in/event times and details beside the list, with no mutation controls. | Real JavaFX smoke with synthetic callbacks |
| HIST-05 | Given loading, empty or failed reads, the UI distinguishes each state, clears stale private details and supports refresh; late reads cannot overwrite newer results or survive navigation/Home. | JavaFX smoke |
| HIST-06 | Given history reads, registration/audit/outbox state is not mutated. | Read-only SQL inspection + integration effect counts |

## Implementation and changed files

- `attendee/AttendanceRecord.java`: immutable display projection, no account secrets or command versions.
- `attendee/AttendanceHistoryRepository.java`: internal owner-filtered read boundary.
- `attendee/AttendanceHistoryService.java`: live session-derived owner, post-read revalidation, immutable list.
- `storage/JdbcAttendanceHistoryRepository.java`: one statement, bound owner, active attendee join, CHECKED_IN filter, 15-second timeout, no write locks; reuses `RegistrationReadSql.displayBooking` and the existing owner index.
- `ui/AttendanceHistoryController.java`: virtual-thread reads with cancellation/latest-task guards, safe failures and navigation invalidation.
- `ui/AttendanceHistoryView.java`: read-only split layout, refresh, empty/error states, current-metadata disclosure.
- `ui/AttendeeBrowseView.java`: history sidebar entry, navigation lifecycle and close handling. Existing constructor overloads preserve older synthetic smoke fixtures; the production constructor supplies real history.
- `ui/EventManagerApplication.java`: session-bound history wiring using the same database/session services.
- `attendee/AttendanceHistoryServiceTest.java`, `registration/AttendanceHistoryIntegrationTest.java`, `ui/AttendanceHistorySmoke.java`: new verification files under `src/test/java/seedu/eventmanager`.
- `build.gradle`: opt-in `attendeeHistoryUiSmoke`.
- `docs/UserGuide.md`, `docs/DeveloperGuide.md`, attendee README and this log: implemented behavior and verification limits.

Production package paths above are under `src/main/java/seedu/eventmanager`.
No applied migrations, dependency versions, Organizer/Admin workflows or
registration/check-in command semantics were changed.

## Commands actually executed and results

Working directory: repository root `CS3227-2610-MP2`.

1. `git status --short` and `git branch -vv`: confirmed dirty unrelated evaluation
   files and #31 on `attendee-self-check-in`.
2. `git switch attendee && git merge --ff-only attendee-self-check-in`: exit 0,
   local fast-forward only; remote branches untouched.
3. Added minimal throwing API scaffolding and tests, then ran:

```sh
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests '*AttendanceHistoryServiceTest.resolvesOwnerAndReturnsImmutableHistoryIncludingEndedEvents' --tests '*AttendanceHistoryIntegrationTest.listsOnlyOwnCheckInsNewestFirstIncludingCompletedAndOngoingEvents'
```

Exit 1: two intended failures, both UnsupportedOperationException from the
unimplemented service; compilation and database fixture setup succeeded. This
proves the public behavior was missing, not an isolated SQL-mutation test.

4. Implemented the read path and ran:

```sh
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests '*AttendanceHistory*Test'
```

First run: exit 1, seven passed, one fixture failure. Setting a venue booking to
CANCELLED requires cancellation time, actor and reason. Added those synthetic
fixture fields (did not weaken the database constraint), reran the same command:
exit 0, all eight tests passed.

5. `./gradlew attendeeHistoryUiSmoke`: exit 0. Tests real controls with synthetic
callbacks, SGT details, no commands, empty/error/expiry, background work, stale
refresh/navigation/Home. Produces ignored screenshots in `build/attendee-smoke`.
6. Full regression command:

```sh
JAVA_TOOL_OPTIONS=-Duser.timezone=UTC DATABASE_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 DATABASE_USER=mp2_demo_app DATABASE_PASSWORD='' DATABASE_INTEGRATION_TESTS=true EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew build --rerun-tasks attendeeHistoryUiSmoke attendeeCheckInUiSmoke attendeeUiSmoke attendeeRegistrationUiSmoke attendeeInboxUiSmoke
```

Exit 0, BUILD SUCCESSFUL. JUnit XML totals: **251 tests, 0 failures, 0 errors,
0 skipped**. All five UI smoke tasks passed. UTC is for the existing venue
timestamp-comparison tests, not a change to SGT application display.

7. Inspected the 1000px screenshot: readable labels, shared single sidebar,
growing split layout, blue Refresh and no mutation controls. The 1280px snapshot
was captured before list cells laid out; added explicit CSS/layout in the smoke
screenshot helper and reran `./gradlew attendeeHistoryUiSmoke`: exit 0, PASS.
Re-inspected the 1280px screenshot: list labels and details both readable.
No product code needed for this screenshot correction.
8. `git diff --check`: passed for tracked edits; a separate no-index diff check
is used for new files before completion. Searched guides for obsolete planned-history
claims (no matches). One exploratory shell glob for booking migrations and a
guessed enum filename were absent; inspecting actual migration sources resolved
the fixture issue. No command outputs containing real credentials were retained.

Final whitespace verification: `git diff --check` passed. Ran
`git diff --no-index --check /dev/null <file>` for each of the nine new Java files
and this log: no whitespace diagnostics. No-index returns 1 for a new-file
difference even without diagnostics; an initial shell loop stopped at that code,
so all files were subsequently checked individually. Final branch is `attendee`;
all feature changes remain uncommitted, with unrelated edits preserved.

## Skill effects, outcome and limitations

Requirements skill kept history separate from registration and exposed metadata
snapshot assumptions. TDD produced an observed missing-behavior failure before
implementation and caught a fixture mistake. PostgreSQL guidance kept metadata
in one joined query using existing predicates/indexes rather than N+1 reads.
Review skill prompted full regression and screenshot inspection. No skill source
was edited; unrelated pending skill/evaluation work is preserved.

This is a read-only view of persisted self-check-ins, not proof of physical
presence. History has no pagination or immutable event snapshots. Database
tests use isolated schemas in the disposable verification database, not the
interactive demo database. Real-login/database-connected desktop E2E, dedicated
security scans and remote CI were not run. Existing JavaFX unnamed-module,
unchecked-compilation and Gradle deprecation warnings remain. No commit, push
or PR update was performed. Teammate review of shared composition and manual
testing with an actual checked-in attendee remain useful next checks.

Suggested commit message: `feat(attendee): add attendance history (#32)`.

## AI-generated mini reflection

The narrow read model reuses canonical attendance and venue data without adding
a second registration workflow. The strongest evidence combines real PostgreSQL
owner/status checks with actual JavaFX navigation tests. The main remaining
verification limit is the separation between database integration and synthetic
UI callbacks, rather than an end-to-end login session in the running app.

## Publishing follow-up (28 September 2026 SGT)

Original request:

> please create a pr for this as well

This authorizes publishing the completed feature. Used code-review-and-verification
for a focused pre-push check. `git status --short`, `git branch -vv`, and
`gh pr list --state open --json number,title,headRefName,baseRefName,url` confirmed
#35 remains open. `git fetch origin` succeeded; `git switch -c
attendee-attendance-history` created a dedicated branch from `c08a06b`. The PR
base is `attendee-self-check-in` to keep earlier stacked features out of this diff.
Unrelated AgenticSE/grader/evaluation edits are excluded and preserved locally.

Fresh command:

```sh
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests '*AttendanceHistory*Test' --rerun-tasks attendeeHistoryUiSmoke
```

Exit 0: history unit/integration tests and UI smoke passed. The earlier 251-test
full-build/five-smoke evidence remains from implementation; it is not presented
as a new full-suite run here. Reviewed session scope, SQL read boundary and
controller stale-result handling; no product correction was needed. The skill
prompted the focused rerun and explicit dependency/base documentation. No remote
CI or security scan result is claimed. PR creation outcome will be reported once
GitHub confirms it; no student approval is inferred.

## Student review

- [x] I confirmed that the original prompts are accurate.
- [x] I confirmed that the changed-file list is accurate.
- [x] I confirmed that recorded commands were actually executed.
- [x] I confirmed that verification results and limitations are accurate.
- [x] I added any mistakes or disagreements omitted by the AI.

Reviewed by: Johannsen Lum
Review date: 29 September 2026
