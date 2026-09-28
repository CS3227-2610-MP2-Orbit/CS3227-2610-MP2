# 012 — Attendee club names and dropdown filter (#37)

Date: 28 September 2026 (SGT)
Contributor: Johannsen
Branch: `attendee-club-filter`
Starting revision: `9bfc80f` from freshly fetched `origin/attendee-attendance-history`
Agent/tool: Codex
Skills: repository requirements-and-acceptance, test-driven-implementation,
code-review-and-verification; installed supabase-postgres-best-practices
(data-n-plus-one and query-missing-indexes references). No skill source changed.

## Objective

Implement only issue #37 in the Attendee UI/read models. Keep Organizer
workflows, UI, repository interface and applied migrations unchanged. No
commit, push or PR publication was requested in this turn.

## Original prompt

The wording below preserves the user request with pasted indentation normalized.

> # Task: Issue #37 — Attendee club names and dropdown filter
>
> ## Role
>
> You are the Attendee developer on the MP2 campus event manager (Java 25, JavaFX,
> PostgreSQL). Start a new branch from the latest Attendee branch (verify which one
> has #29–#32 first). Follow AGENTS.md guardrails.
>
> ## Goal (from issue #37)
>
> Show human-readable club names and replace the exact club ID field with a
> dropdown for filtering events.
> Scope: Attendee UI/read model. Reuse shared club data and agree any contract
> changes with Joseph; do not change Organizer workflows.
>
> ## Current state (verify before editing)
>
> - Clubs live in Joseph's `organizer_club` table (`id UUID`, `name`, unique
>   case-insensitive name). `organizer_event.club_id` is `VARCHAR(100)` holding
>   `organizer_club.id` as text. Joseph's Organizer UI already maps IDs to names.
> - Joseph's `ClubRepository` only has `findByOwner`, `existsByNameIgnoreCase` and
>   `create`. There is no read-only "all clubs" or "name by ID" lookup.
> - Attendee shows raw IDs: `AttendeeBrowseView` has a free-text "Exact club ID"
>   field and shows "Club: <id>". `CatalogueEvent`, `CatalogueQuery`,
>   `MyRegistration`, `AttendanceRecord` and `RegistrationEventInfoRepository`
>   all carry `clubId` only.
> - Filtering is an exact match on `clubId` in `EventCatalogueService`.
> - Attendee read SQL joins `organizer_club` directly
>
> ## Requirements
>
> 1. Show the club name (not the ID) wherever Attendee shows a club: catalogue
>    list, event details, My Registrations and Attendance History.
> 2. If a club ID has no matching club (for example, older env-configured IDs),
>    show a neutral fallback such as "Unknown club". Never fail the screen.
> 3. Replace the club text field with a dropdown that has an "All clubs" default,
>    sorted by name. Filtering still runs on the club ID internally (the filter
>    semantics are unchanged), and Clear resets it to "All clubs".
> 4. Resolve names in the read model or SQL, not with one query per row. No new
>    table, no edits to applied migrations, and no write locks.
> 5. Do not change Organizer workflows or Joseph's UI.

## Inspection, decisions and acceptance criteria

Fetched origin and inspected branch ancestry before editing. History commit
9bfc80f contains #29 (6189718), #30 (ab90328), #31 (c08a06b) and #32.
Created attendee-club-filter from that remote tip. Git initially inferred the
history branch as upstream; unset it to avoid accidentally pushing to the old
branch. Existing AgenticSE, grader/evaluation and untracked log 011 work was
preserved, not staged or overwritten.

Confirmed canonical schema in organizer V1/V4 and existing raw-ID projections.
The direct-join instruction was implemented in Attendee adapters; there is no
change to Joseph's ClubRepository, ClubService, UI or schema contract.
No teammate agreement was fabricated or external message sent.

User-agreed requirements, with observable verification:

| ID | Given / when / then | Evidence |
| --- | --- | --- |
| CLUB-01 | Given a matching shared club, when attendee reads an event, then its name appears in catalogue, details, bookings and history | PostgreSQL projection test and three JavaFX smoke checks |
| CLUB-02 | Given a non-UUID legacy or unmatched UUID ID, when read, then the record remains visible with Unknown club | PostgreSQL fallback test |
| CLUB-03 | Given shared clubs, when options load, then All clubs is first and names are case-insensitively sorted; selecting uses ID, not name; Clear resets | PostgreSQL options/filter assertions and Browse UI smoke |
| CLUB-04 | Given multiple reads, when names resolve, then each existing projection uses a left join, not per-row lookups; dropdown uses one query | SQL inspection; no audit/outbox writes in integration assertions |
| CLUB-05 | Given slow/failing club loads, when refreshed, then UI work stays off FX, selection survives failures, and superseded responses cannot overwrite new choices | Browse UI smoke |

Low-impact implementation choices: dropdown contains all shared clubs, including
clubs with no upcoming events. Search / Refresh applies the selected club and
reloads options. Clear selects All clubs. A selected ID removed from shared
data is retained as Unknown club until the user clears/changes it; silently
broadening an already-submitted filter is avoided. These are documented.

## Implementation and affected files

New:
- `attendee/CatalogueClub.java`: ID/name projection, shared neutral fallback
  and name comparator; no owner fields exposed.
- `storage/AttendeeClubSql.java`: shared left join casts club UUID to text,
  never the potentially non-UUID legacy event ID to UUID.
- `registration/AttendeeClubNamesIntegrationTest.java` in test sources:
  isolated PostgreSQL fixtures across attendee projections.
- This log.

Modified main sources (relative to src/main/java/seedu/eventmanager):
- attendee: CatalogueEvent, EventCatalogueRepository, EventCatalogueService,
  MyRegistration, MyRegistrationsService, AttendanceRecord,
  RegistrationEventInfoRepository.
- storage: JdbcEventCatalogueRepository, JdbcAttendeeEventDetailsRepository,
  JdbcRegistrationEventInfoRepository, JdbcAttendanceHistoryRepository.
- ui: AttendeeBrowseController, AttendeeBrowseView, MyRegistrationsView,
  AttendanceHistoryView.

The catalogue's internal Entry pairs the existing Organizer event with its
read-only club name; public CatalogueEvent still excludes Organizer identity.
Display records carry both stable clubId and resolved clubName. Existing
constructor shapes remain available with neutral fallback. Service authorization,
event visibility, registration/check-in rules and ordering remain unchanged.

Read adapters use the shared one-to-one club join and existing 15-second
statement timeouts, without write locks. Dropdown reads only IDs/names in one
additional background query per refresh, independent of result row count.
No table, migration, dependency or new Organizer workflow was introduced.

Modified tests:
- attendee/EventCatalogueServiceTest and JdbcEventCatalogueRepositoryIntegrationTest:
  adapt the internal repository entry contract, retaining existing assertions.
- ui/AttendeeBrowseSmoke: named options, ID filtering, Clear, named details,
  off-FX load, failure/retry/selection preservation and stale club response.
- ui/AttendeeRegistrationSmoke and AttendanceHistorySmoke: named detail assertions.
- ui/AttendeeCheckInSmoke and AttendeeInboxSmoke: adapt catalogue fixtures.

Updated only the relevant UserGuide and DeveloperGuide sections for dropdown,
fallback and read-model behavior. No standalone plan was added.

## Commands and red/green evidence

All commands ran in CS3227-2610-MP2. Inspection used rg/sed/cat and git status,
branch/log/diff; source edits used apply_patch.

Branch verification/creation (exit 0):
```sh
git fetch origin
git log --oneline -6 origin/attendee-attendance-history
git merge-base --is-ancestor origin/attendee-registration-ui origin/attendee-attendance-history
git merge-base --is-ancestor origin/attendee-notifications-inbox origin/attendee-attendance-history
git merge-base --is-ancestor origin/attendee-self-check-in origin/attendee-attendance-history
git switch -c attendee-club-filter origin/attendee-attendance-history
git branch --unset-upstream
```

Added minimal clubName API/fallback scaffolding before the real database test;
it did not implement lookup. Then ran:
```sh
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests '*AttendeeClubNamesIntegrationTest' --rerun-tasks
```
Exit 1: two tests, one assertion failure. Known club expected Campus Arts but
received Unknown club. Legacy fallback passed. This was an intended behavioral
red, not a compilation/setup error.

After the SQL/service implementation:
```sh
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests '*AttendeeClubNamesIntegrationTest' --tests '*EventCatalogue*' --tests '*MyRegistrations*' --tests '*AttendanceHistory*' --rerun-tasks
```
Exit 0, BUILD SUCCESSFUL.

Added Browse dropdown/name assertions before replacing the text field:
```sh
./gradlew attendeeUiSmoke
```
Exit 1: intended assertion "club filter must be a dropdown, not a raw ID field".

After UI implementation and adding the options integration case:
```sh
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests '*AttendeeClubNamesIntegrationTest' --tests '*EventCatalogue*' --tests '*MyRegistrations*' --tests '*AttendanceHistory*' attendeeUiSmoke attendeeRegistrationUiSmoke attendeeHistoryUiSmoke --rerun-tasks
```
Database tests and Browse smoke passed. Exit 1 from My Registrations smoke's old
"Club: test-club" assertion. Updated that fixture to provide Campus Technology
and assert the human-readable name and absence of its raw ID. This changes an
obsolete presentation expectation, not a weakened business-rule test.

Added failure/stale-club checks and named History assertions, then ran:
```sh
./gradlew attendeeUiSmoke attendeeRegistrationUiSmoke attendeeHistoryUiSmoke
```
Exit 0, all three PASS.

Full final regression:
```sh
JAVA_TOOL_OPTIONS=-Duser.timezone=UTC DATABASE_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 DATABASE_USER=mp2_demo_app DATABASE_PASSWORD='' DATABASE_INTEGRATION_TESTS=true EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_inbox_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew build --rerun-tasks attendeeHistoryUiSmoke attendeeCheckInUiSmoke attendeeUiSmoke attendeeRegistrationUiSmoke attendeeInboxUiSmoke
```
Exit 0, BUILD SUCCESSFUL in 32s. JUnit XML totals: **254 tests, 0 failures,
0 errors, 0 skipped**. All five actual JavaFX smoke tasks PASS.

Inspected generated browse-1000.png, attendance-history-1000.png and
registration-details-1000.png under ignored build/attendee-smoke: named club
labels readable, dropdown in the existing filter row, side-by-side details
preserved. Screenshots use synthetic callbacks, not real-login/database UI E2E.
Database tests use disposable per-test schemas; no interactive demo data edited.

## Review, outcome and limitations

Requirements skill kept ownership and filter semantics explicit. TDD established
separate observed database and UI reds. PostgreSQL guidance led to joined/batched
reads, avoiding N+1 lookups. Review prompted full regression, raw-ID display
search and screenshot inspection. No skill edits or delegated agents were used.

All five requested requirements implemented. No source changes under Organizer
event/club modules or applied migrations. Issue #38, session races, publication
and release blockers from log 011 remain out of scope and are not claimed fixed.
Existing JavaFX unnamed-module, unchecked-compilation and Gradle warnings remain.
No load/EXPLAIN benchmark, security scan, cross-platform runtime test or remote CI
was run. No commit, push, PR creation or issue closing was performed.

Suggested commit message: `feat(attendee): show club names and dropdown filter (#37)`.

## AI-generated mini reflection

Keeping the ID for filtering while enriching read projections with a name avoids
coupling attendee presentation to Organizer write APIs. The legacy-ID database
case and stale-response UI test provide stronger evidence than only testing a
happy-path dropdown. Whole-application real-login E2E remains separate work.

## Publishing follow-up (28 September 2026 SGT)

Original prompt:

> looks good, please pr it

Used code-review-and-verification for the publishing checks. Fetched origin and
queried open PRs: #36 is still open, so this feature's PR base remains
attendee-attendance-history, not main. Re-ran the exact full final regression
command above: exit 0, BUILD SUCCESSFUL in 34s, all five UI smoke checks PASS.
No application code was changed in this follow-up.

Publishing scope is the 28 #37 implementation/test/guide/log files listed above.
Existing AgenticSE/grader/evaluation edits and untracked review log 011 are
excluded. No changes to Organizer workflows, applied migrations or #38.
Student approval fields remain unfilled; the user's PR request is authorization
to publish, not evidence of personal verification of every log statement.

Published implementation commit `1baec54` with
`git commit -m 'feat(attendee): show club names and dropdown filter (#37)'`
and `git push -u origin attendee-club-filter` (both exit 0).
Created https://github.com/CS3227-2610-MP2-Orbit/CS3227-2610-MP2/pull/39 using
`gh pr create --repo CS3227-2610-MP2-Orbit/CS3227-2610-MP2 --base attendee-attendance-history --head attendee-club-filter --title 'feat(attendee): club names and dropdown filter' --body <summary, scope, verification, dependency and Closes #37> --milestone 'Core Workflow MVP'`
(exit 0; body content is recorded on the PR). Attached it to this chat.
The PR documents its dependency on #36. The fresh JUnit XML total is 254 tests,
zero failures/errors/skips. Publishing did not merge any PR or manually close
issue #37. Remote CI is separate from the passing local checks above.

## Student review

- [x] I confirmed that the original prompts are accurate.
- [x] I confirmed that the changed-file list is accurate.
- [x] I confirmed that recorded commands were actually executed.
- [x] I confirmed that verification results and limitations are accurate.
- [x] I added any mistakes or disagreements omitted by the AI.

Reviewed by: Johannsen Lum
Review date: 29 September 2026
