# 006 — Attendee catalogue first slice

Date: 2026-09-25
Contributor: Johannsen
Branch and starting revision: `attendee`, `bfab6b8956a718930b4f60cf09e7eb16b21f38b3`
Agent/tool: Codex desktop, single agent
Skills used: repository `test-driven-implementation`, `code-review-and-verification`,
`desktop-ui-polish`; bundled `supabase-postgres-best-practices` for the SQL/index
review (not a Supabase service integration). Skills were read before use and not
changed. No subagents were used.

## Objective

Implement the first planned Attendee slice: navigation, read-only published-event
browsing/search and details, with tests and honest integration limitations.

## Original prompts (verbatim)

> go

## Response summary

Interpreted "go" as approval for the recommended defaults and to begin the first
slice, as announced before implementation. Added a public catalogue route,
application service/public DTO/read boundary, JDBC adapter, responsive asynchronous
JavaFX screen, six service tests, three real PostgreSQL tests and an opt-in JavaFX
smoke harness. Updated CI and guides. No registration, authentication redesign,
publication, venue integration or further feature slice was implemented.

## Assumptions and design decisions

- Browsing is public/read-only. Future personal actions require a resolved session;
  selecting the Attendee workspace is not authentication.
- Source is the existing Organizer table. Show only PUBLISHED events strictly
  after the injected current instant, both in listing and direct-ID details.
- Search is literal, case-insensitive title/description matching. Club filter is
  an exact case-sensitive ID. Date bounds are inclusive SGT start-calendar dates.
- Public DTO omits organizer identity and internal state/version. Capacity is
  clearly the configured limit, not remaining seats. No inferred venue details.
- Data/schema errors never make drafts visible. Browse performs no migration or
  seeding; a missing schema is reported safely and can be retried after setup.
- JDBC/configuration work runs outside the FX thread. Superseded tasks cannot
  replace current results, and Home/shutdown cancels work. No raw exception
  message, credentials, query text or business audit is emitted for catalogue reads.
- No new dependencies or schema changes. Existing indexes are not sufficient to
  claim large-data catalogue performance; pagination/index work is deferred and
  recorded, rather than modifying a teammate's applied migration.
- Previously uncommitted evaluation tools, plan and logs were preserved. Only the
  plan's progress section was updated to describe this slice.

## Files changed

- `src/main/java/seedu/eventmanager/attendee/`: new `CatalogueEvent`,
  `CatalogueQuery`, `EventCatalogueRepository`, `EventCatalogueService`; updated README.
- `src/main/java/seedu/eventmanager/storage/JdbcEventCatalogueRepository.java`.
- `src/main/java/seedu/eventmanager/ui/AttendeeBrowseView.java` and
  `EventManagerApplication.java`.
- `src/test/java/seedu/eventmanager/attendee/EventCatalogueServiceTest.java` and
  `JdbcEventCatalogueRepositoryIntegrationTest.java`.
- `src/test/java/seedu/eventmanager/ui/AttendeeBrowseSmoke.java`.
- `build.gradle`, `.github/workflows/ci.yml`, `docs/UserGuide.md`,
  `docs/DeveloperGuide.md`, `docs/AttendeePlan.md`, and this log.
- Generated ignored build reports and `build/attendee-smoke/*.png`; these are
  rebuildable local artifacts, not committed product screenshots.

## Commands actually executed

Repository working directory:
`/Users/johannsenlum/Documents/School/Y4S1/CS3227 Agentic Software Engineering/CS3227-2610-MP2`.
Source/instruction/build/CI inspection used `cat`, `rg`, `git status` and `git diff`.

```sh
./gradlew test --tests seedu.eventmanager.attendee.EventCatalogueServiceTest
mktemp -d /tmp/mp2-attendee-db.XXXXXX
initdb -D /tmp/mp2-attendee-db.TKAnNY/data -U mp2_eval --auth=trust --no-locale -E UTF8
pg_ctl -D /tmp/mp2-attendee-db.TKAnNY/data -l /tmp/mp2-attendee-db.TKAnNY/server.log -o '-h 127.0.0.1 -p 55447 -k /tmp/mp2-attendee-db.TKAnNY' -w start
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55447/postgres EVENT_MANAGER_TEST_DB_USER=mp2_eval ./gradlew test --tests 'seedu.eventmanager.attendee.*'
./gradlew attendeeUiSmoke
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55447/postgres EVENT_MANAGER_TEST_DB_USER=mp2_eval EVENT_MANAGER_TEST_DB_PASSWORD='' DATABASE_INTEGRATION_TESTS=true DATABASE_URL=jdbc:postgresql://127.0.0.1:55447/postgres DATABASE_USER=mp2_eval DATABASE_PASSWORD='' ./gradlew clean build
JAVA_TOOL_OPTIONS=-Duser.timezone=UTC EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55447/postgres EVENT_MANAGER_TEST_DB_USER=mp2_eval EVENT_MANAGER_TEST_DB_PASSWORD='' DATABASE_INTEGRATION_TESTS=true DATABASE_URL=jdbc:postgresql://127.0.0.1:55447/postgres DATABASE_USER=mp2_eval DATABASE_PASSWORD='' ./gradlew build --rerun-tasks
git diff --check
pg_ctl -D /tmp/mp2-attendee-db.TKAnNY/data -m fast -w stop
test -d /Users/johannsenlum/.Trash && test ! -e /Users/johannsenlum/.Trash/mp2-attendee-db.TKAnNY && mv /tmp/mp2-attendee-db.TKAnNY /Users/johannsenlum/.Trash/mp2-attendee-db.TKAnNY
```

The local PostgreSQL cluster was new and isolated, loopback-only on port 55447;
the existing server on port 5432 was not used or changed. New catalogue tests
create/drop only their own generated schemas; the full existing suite also writes
test fixtures, entirely inside this disposable cluster.
After verification, that exact agent-created cluster was stopped and its temporary
files moved to Trash (recoverable). An attempted recursive forced removal was
rejected by the execution tool and did not execute; the recoverable move was used
instead. No user database was removed. Build reports/screenshots remain in the
ignored `build/` directory.

## Actual verification results

- **Red:** six compiling service tests ran against minimal API scaffolding; five
  failed on missing listing/filter/validation/details behavior, exit 1. The hidden
  detail negative test already passed; no regression was manufactured.
- **Green:** same six service tests passed after implementation, exit 0.
- **Focused database run:** all nine Attendee tests passed, including three real
  JDBC cases, no skips. Checks cover SQL visibility, start boundary, deterministic
  order, literal search, combined filters, SGT dates, direct-ID lookup, refresh
  after visibility changes and absence of read-side audit writes.
- **Initial full build, default SGT timezone:** 73 tests, 71 passed, two existing
  Venue PostgreSQL cases failed, no skips, exit 1. `persistsRequestAndDetectsBookingConflict`
  expected `2026-09-27T16:41:25+08:00` but read the equivalent `2026-09-27T08:41:25Z`.
  `createsUpdatesAndDeletesAvailabilityRecord` failed record containment for the
  same offset-sensitive record-equality pattern. Those test/source files were
  untouched. No unrelated fix or assertion weakening was made.
- **Diagnostic full rebuild with UTC JVM timezone:** 73 tests passed, zero skips,
  failures or errors; build exit 0. This is environment-specific confirmation,
  not a claim the SGT failure was fixed. XML totals were inspected with `rg`/`awk`.
- **JavaFX smoke:** passed browse/details, text filtering, empty state, date
  validation, safe error/retry and Home callback. Real UI, in-memory synthetic data:
  not a live database-connected or cross-role UI test. Ran three times while
  regenerating screenshots after clean and correcting resize capture timing.
- **Visual inspection:** read snapshots with the image viewer. The first minimum
  size capture occurred before native resize completed; added an explicit wait
  for actual root dimensions, reran successfully and inspected the corrected
  1000-wide image. This was a test-harness correction, not a claimed app bug.
- Gradle reports existing deprecations/unchecked code; the classpath-based JavaFX
  smoke reports an unnamed-module warning but completes. No new library added.
- No CodeQL/security scanner or formatter is configured/run here. CI workflow
  updated locally only; remote Actions execution is not claimed.
- Final diff review and `git diff --check` completed without whitespace errors.
  Existing unrelated working-tree changes were preserved; no commit/push occurred.

## Review and desktop UI checklist

- Before: no Attendee route/screen. After: one dark role sidebar, edge-to-edge
  content, Home callback, no competing outer header.
- Content fills available width. Filters wrap at the minimum window width;
  labels retain full wording; details scroll vertically.
- Search/Refresh, Clear filters and Home have observed effects. Loading, empty,
  validation and safe retry states are explicit.
- Sidebar/page/cards/primary button match Venue's existing visual tokens.
- No club CRUD, publishing, venue approval, login bypass, seat count or working
  registration controls were invented. Future features are labelled unavailable.
- No new dependencies. Private Organizer fields are excluded from the public DTO.
  Publication/start visibility is rechecked in service details, not just UI selection.
- Human team review should confirm catalogue/publication contract compatibility.
  No messages or reviews were posted on the student's behalf.

## Problems, corrections, and skill revisions

TDD supplied actual red/green evidence rather than a compile failure. Review exposed
unrelated timezone-sensitive database tests; their failures are retained here.
The UI skill led to a single shell, wrapping filters and minimum-size inspection.
No skill revision was justified by this slice. The PostgreSQL skill prompted an
explicit performance/index limitation rather than an unmeasured performance claim.

## Outcome and limitations

First read-only slice is implemented locally. Current public fields are covered;
venue, available seats and own registration status in ATT-03 are not. ATT-15 checks
apply to this browse view only. Publication is still absent in Organizer, so fresh
data remains drafts and the catalogue can legitimately be empty. No demo records
were inserted into the application database. Future mutation policies/integration
contracts remain separate work. Nothing committed, pushed or published.

Suggested commit message: `feat(attendee): add read-only event catalogue and browse UI`

## AI-generated mini reflection

The skills helped turn a narrow feature slice into an observed test cycle and a
usable UI without inventing integration data. The most useful correction was
checking actual rendered dimensions instead of trusting the screenshot filename.
Full database testing also exposed a pre-existing timezone assumption that normal
skipped tests had not shown. Next: coordinate publication/booking/transaction
contracts, then implement authenticated registration with real concurrency tests.

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
