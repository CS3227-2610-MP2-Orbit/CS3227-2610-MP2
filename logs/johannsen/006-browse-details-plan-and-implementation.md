# 006 — Browse/search event details: planning and implementation

Renumbered from combined log 016 on 27 September 2026. Former log numbers in
the historical prompts and phase records below are preserved as historical context.

Combined on 27 September 2026 at Johannsen's request. The planning phase below
preceded implementation authorization; its proposed/ready wording records that
earlier state, not unfinished work after the implementation phase. Original prompts,
acceptance criteria, commands, results and limitations are retained. Historical
mentions of removed logs refer to the pre-cleanup records, preserved in the local backup.

## Planning record (formerly log 016)

Date: 27 September 2026
Contributor: Johannsen
Branch: attendee, 95878ad
Agent: Codex desktop, single agent
Skill: `.agents/skills/requirements-and-acceptance/SKILL.md`

### Original prompt

>  okay great，lets do them 1 by 1. Planning out browse and search event details with good SWE principles, DRY, SOC

### Objective and scope

Plan issue #28, not implement it. Extend existing browse/details with venue,
remaining seats, own registration status and eligibility explanations. Preserve
the search/date/club semantics and future PUBLISHED visibility. Registration
buttons and My Registrations are #29, not this slice. No GitHub body expansion,
commit, push, database mutation or new application code authorized by planning.

### Evidence and commands

Read requirements skill completely, existing EventCatalogueRepository,
JdbcEventCatalogueRepository, CatalogueQuery, AttendeeBrowseView,
RegistrationStore/JdbcRegistrationStore, RegistrationService, JdbcEventRegistrations,
JdbcDatabase, VenueBookingRepository, ClubRepository and Actor. Inspected current
catalogue test names and existing source evidence from log 013.

Commands included `git status --short --branch`,
`gh issue view 28 --json title,body,milestone`, cat of the sources listed above and
`rg -n '@Test|void |public ' src/test/java/seedu/eventmanager/attendee/EventCatalogueServiceTest.java src/test/java/seedu/eventmanager/attendee/JdbcEventCatalogueRepositoryIntegrationTest.java`.
All completed successfully. No tests executed for this planning-only task.

### Proposed design

1. Keep EventCatalogueService responsible for public search and visibility.
2. Add an authenticated AttendeeEventDetailsService, depending on a small
   read-only repository interface. Derive attendee identity from the live session,
   never a UI-supplied user ID. Return an immutable AttendeeEventDetails value.
3. Details contain existing public fields, venue name/location and booking state,
   capacity/occupied/remaining seats, own status and typed eligibility reasons.
   They never contain other attendee identities, password hashes or organizer
   internal fields. Keep personal data separate from the public CatalogueEvent.
4. Implement JDBC reads over canonical tables, preferably one bounded joined
   snapshot with an independent count aggregate to avoid multiplying rows.
   No write locks or register calls for a preview. Keep SQL out of service/UI.
5. Share a small pure RegistrationEligibilityPolicy with the existing register
   command, extracting only existing rules. The command retains lock order,
   post-lock time recheck, version checks, authorization, idempotent retries and
   transactional state/audit/outbox. A details preview is never authorization
   or a seat reservation. Characterization tests must prove unchanged commands.
6. Reuse registration seat-count and booking-match definitions through a narrow
   shared persistence helper where appropriate, without making browse acquire
   command locks or altering Joseph's EventRegistrations interface.
7. View renders values and messages only; a small controller/presenter manages
   selection, background tasks, loading/error/retry and ignoring stale results.
   Preserve the existing SGT formatter and cancellation on Home/close. Avoid a
   generic UI framework or unnecessary base classes.

### Existing rules to preserve

- Visibility is upcoming PUBLISHED, even if registration is unavailable because
  of booking/venue/capacity. Full events do not disappear just because full.
- Registration requires future PUBLISHED, matching CONFIRMED booking (same event
  interval), ACTIVE venue and capacity. Missing/AT_RISK/cancelled booking is not
  silently treated as confirmed. UI states do not loosen command checks.
- Occupancy counts CONFIRMED and CHECKED_IN, including inactive accounts; CANCELLED
  does not count. Joseph's roster excludes inactive accounts and therefore is not
  a seat counter. Display remaining as max(0, capacity minus occupied), with no
  promise of a seat until a later registration succeeds.
- Show own registration separately from general availability; an already-registered
  attendee should see that fact even when the event is full. No new history access
  or after-start catalogue visibility in this slice.

### Scope of changes and database

Attendee service/read-model/repository interface, storage adapter, small shared
registration policy/query support, Attendee view/controller, application wiring,
tests and guides. Pass the current session and initialize existing registration
migrations when entering Attendee so it does not depend on visiting Organizer.
No expected new table/column or new migration; do not modify applied migrations.
Shared services keep their owners; do not edit Organizer/Admin screens.
Human-readable club labels could be a small follow-up, but club-picker redesign,
new filtering semantics, pagination and automatic refresh are not necessary for #28.

### Acceptance criteria and verification

| ID | Given / When / Then | Verification |
| --- | --- | --- |
| BD-01 | Given existing search inputs, searching preserves literal case-insensitive text, exact club and inclusive SGT date behavior, deterministic ordering and draft exclusion. | Existing unit + JDBC regression |
| BD-02 | Given published event/booking/venue combinations, opening details shows accurate venue data and distinguishes unavailable registration without hiding valid catalogue events. | Pure policy + real JDBC |
| BD-03 | Given confirmed, checked-in, cancelled and inactive-user records, details count seats by the command's rule without negative remaining display or join inflation. | JDBC integration |
| BD-04 | Given two attendees, details reveal only the caller's status; expired/invalid/wrong-role sessions reject personalized reads. | Service + authorization integration |
| BD-05 | Given capacity/status/time changes after a search, refreshed details reflect current data; started or unpublished direct lookups no longer expose the event. | Controlled clock + JDBC |
| BD-06 | Given slow/failing/out-of-order loads or logout/Home, UI remains responsive, clears stale personal state and does not render superseded results or raw SQL errors. | Controller + actual JavaFX smoke |
| BD-07 | Given a details read, no event/registration/audit/outbox writes occur; eligibility refactoring preserves all existing register/cancel/concurrency/rollback semantics. | Read-side-effect and registration regression tests |

### Sequence, readiness and boundaries

Implementation proposal: characterize existing behavior, test/implement read facts
and pure eligibility, integrate session and UI, run real PostgreSQL + JavaFX
verification, then scoped review/docs/log. Each stage uses actual red/green evidence
where changing behavior; do not invent failures or performance claims.

Ready to implement on this local baseline once authorized. Real organizer-to-public
catalogue demonstration still needs Joseph's publication workflow; synthetic demo
data supports development but is not that proof. No new check-in or venue-risk
policy is introduced here. Catalogue PR #25 remains a branch integration dependency.
The requirements skill influenced separation of proposal, current rules and
readiness; no skill files changed.

### AI-generated mini reflection

The main design risk is duplicating eligibility rules or incorrectly using the
Organizer roster as capacity. A pure shared policy and a narrowly scoped read
projection keep those concerns explicit without coupling UI to SQL or command locks.

## Implementation record (formerly log 017)

Date: 27 September 2026
Contributor: Johannsen
Branch and starting revision: attendee, 95878ad
Agent/tool: Codex desktop, single agent

### Original prompt (verbatim)

```text
looks good, lets do it as well as update the logs for 003



And changing 011-registration-backend-and-shared's logs.md



## Original request and policy replies

> can we push this as a pr then also do the thing that joseph's need such that he can continue to work as well

User repeated Joseph's README during implementation; confirmed it is the intended contract. Earlier clarified contract is preserved in log 009.

Asked whether registration needs PUBLISHED + matching CONFIRMED booking + ACTIVE venue. The user first asked:

> do we have a confirm active button?

Explained that venue ACTIVE and booking CONFIRMED are existing states, not one button; Venue Admin approval creates the booking, but publication remains absent. On the focused follow-up, the user answered:

> Yes, require all three states (recommended)



Please help me create a better "original request", same goes for my 003 as well
```

### Objective, skills and scope

Implement the accepted #28 plan in the planning record above. No registration buttons, My
Registrations, check-in, notification inbox, publication or other role UI changes.
Reword the request introductions in logs 003 and 011 without fabricating history:
add explicitly labelled retrospective paraphrases, preserve actual quotations,
policy replies and historical results. Student review boxes remain unfilled.

Read requirements-and-acceptance, test-driven-implementation,
code-review-and-verification, security-and-rbac, database-migration-and-integrity
and desktop-ui-polish in full. Read installed supabase-postgres-best-practices
and lock-short-transactions. The skills guided acceptance boundaries, observable
red/green evidence, narrow SQL/session reuse, non-locking read snapshots, UI
consistency and explicit verification limitations. No skill edits or delegation.

### Implementation and SWE decisions

- Public catalogue stays separate from the immutable personalized detail model.
  New AttendeeEventDetailsService derives the viewer from the live session,
  rechecks it after the read, and revalidates upcoming/PUBLISHED visibility.
  Public APIs do not accept a caller-selected attendee ID.
- New JDBC repository selects event, venue/current booking, occupied seats and
  own status in one statement snapshot with a 15-second timeout and no write locks.
  One-to-one joins use existing unique keys; the count is a separate aggregate.
  No new table, column, applied migration edit or dependency.
- RegistrationReadSql shares occupancy and exact booking-interval predicates;
  RegistrationEligibilityPolicy shares event/time and capacity rules;
  AttendeeSessionGuard shares existing session/role validation. Existing command
  lock order, state/version checks, retries and audit/outbox atomicity remain.
- Occupancy includes CONFIRMED/CHECKED_IN even for inactive accounts, excludes
  CANCELLED, and is not computed from Joseph's active-account roster. Remaining
  display is clamped at zero. Own status and general availability are separate.
- AttendeeBrowseController owns background tasks, cancellation and ignoring stale
  results. The view renders venue/location, booking/venue state, remaining seats,
  own status and eligibility; Refresh details reloads without losing selection.
  Loading/failure/Home clears prior personal details. No SQL/business eligibility
  logic is placed in JavaFX handlers.
- Application composition passes the session privately to the detail service and
  lazily initializes existing prerequisites on background reads. Initialization
  succeeds once per workspace and failed initialization can be retried. There is
  no dependency on visiting Organizer first, and no automatic fixture insertion.

### Files changed

New attendee detail model, repository interface and service; shared registration
policy/session guard; JDBC detail adapter and SQL predicates; Attendee controller;
service and isolated-schema integration tests. Updated registration command/store
to reuse equivalent rules, browse view/application wiring, JavaFX smoke, Attendee
README and User/Developer Guides/plan. Edited only request-introduction sections
of logs 003/011; added this log. Unrelated evaluation files remain unchanged.

### Test plan and actual red/green evidence

Test plan: service tests cover own-only projection, auth/role rejection, exact
start/draft/missing visibility, full/unconfirmed/inactive explanations and session
revocation during a read. PostgreSQL tests cover true occupancy (including
inactive users), different owners, booking/time/status changes, query side effects
and reads while another transaction holds event/booking locks. UI smoke tests
cover current details, refresh, error/redaction, expiry and late responses.

All commands below were run from CS3227-2610-MP2.

1. Added compile-ready empty detail service and AVAILABLE-only policy scaffold
   plus six behavioral tests. Ran:

   `./gradlew test --tests 'seedu.eventmanager.attendee.AttendeeEventDetailsServiceTest' --tests 'seedu.eventmanager.registration.RegistrationServiceTest'`

   Exit 1: 14 tests, six expected assertion failures (no returned details,
   missing auth/visibility rejection, wrong eligibility result). Eight existing
   registration unit tests passed. This was behavioral red, not compilation failure.

2. Implemented service and policy, extracted shared session guard and existing
   command predicates. Ran:

   `./gradlew test --tests 'seedu.eventmanager.attendee.*' --tests 'seedu.eventmanager.registration.RegistrationServiceTest'`

   Exit 0. Database catalogue cases were skipped because no database flags were
   supplied in this run; this step is unit evidence only.

3. Added empty JDBC adapter and five real isolated-schema integration tests. Ran:

   `EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_attendee_test_johannsen EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests 'seedu.eventmanager.registration.AttendeeEventDetailsIntegrationTest'`

   Exit 1: four tests rejected valid fixture events as unavailable because the
   empty repository returned no snapshot. The independent session-rejection test
   passed. Database setup/fixtures succeeded; this demonstrates missing read behavior.

4. Implemented the SQL snapshot and shared predicates. Ran:

   `EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_attendee_test_johannsen EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew test --tests 'seedu.eventmanager.attendee.*' --tests 'seedu.eventmanager.registration.*'`

   Exit 0, including existing concurrency/rollback registration tests.

5. Integrated controller/view/application composition. Ran
   `./gradlew classes testClasses` (exit 0), then `./gradlew attendeeUiSmoke`
   (exit 0). Smoke passed personalized venue/seats/status, refresh/full state,
   details failure/retry, session expiry, late response rejection, search filters,
   empty/validation/error states and Home during an outstanding read. Deliberate
   failure fixtures produce redacted warning logs. Inspected the generated
   `build/attendee-smoke/browse-1000.png` visually. Re-ran smoke after bounding its
   delayed-fixture wait and adding cleanup release; no application logic changed.

6. Created a fresh disposable verification database, never the interactive demo:

   `/Applications/Postgres.app/Contents/Versions/17/bin/createdb -h 127.0.0.1 -p 55448 -U mp2_bootstrap -O mp2_demo_app mp2_details_verify_20260927`

   `JAVA_TOOL_OPTIONS=-Duser.timezone=UTC DATABASE_URL=jdbc:postgresql://127.0.0.1:55448/mp2_details_verify_20260927 DATABASE_USER=mp2_demo_app DATABASE_PASSWORD='' DATABASE_INTEGRATION_TESTS=true EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_details_verify_20260927 EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD='' ./gradlew build --rerun-tasks`

   Exit 0. Parsed JUnit XML: **205 tests, zero failures, errors or skips**. UTC is
   the pre-existing documented Venue offset-equality workaround; the focused
   Attendee/registration database suite also passed without it.

7. `git diff --check` passed. Reviewed source/diff/status and compared the two
   existing grader files against preserved stash 390c047d6965fbbd7ba570631f91618d5a32f369:
   no differences, confirming they were not altered by this feature. No source
   changed outside the Attendee composition and narrow registration reuse listed.

### Review and limitations

All seven BD criteria in the planning record above have service/database/UI evidence appropriate to
their level. The direct-ID service rechecks visibility; SQL excludes drafts and
inactive viewers. Re-authentication prevents returning a revoked in-flight read.
Preview is not a seat reservation: commands always revalidate under locks.

UI before/after: retains one sidebar, width-filling content and shared colors;
new details wrap and scroll at compact size. Refresh details is explicit and
disabled without a selection; Home cancels/clears. No new register button,
Organizer controls, club-directory policy or authentication redesign.

JavaFX smoke uses synthetic repositories, not a live-database login-to-details
E2E. PostgreSQL service tests separately use real sessions/storage. No live demo
records were changed and the user's existing app window was not forcibly closed.
Restart it to load this implementation. Publication remains absent; fixtures do
not prove an Organizer publish workflow. Existing unchecked, Gradle deprecation
and JavaFX unnamed-module warnings remain. No configured security scanner or
formatter was run; compilation is the project's current static/type check.

### Outcome

Issue #28 implemented locally, not pushed, committed, closed or posted to GitHub.
The issue remains a simple goal as requested. Logs 003/011 now have clearer
request summaries without rewritten quotations or invented student approval.

### AI-generated mini reflection

The useful DRY boundary was eligibility/authentication and persistence predicates,
not using a write-oriented service to render a read preview. Real database tests
confirmed the inactive-account capacity distinction and non-locking reads; UI
tests supplied separate evidence for asynchronous presentation and stale-response
safety. Full cross-role publication remains outside this slice.

### Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:

## Log consolidation record

User request (verbatim):

> remove 3,4,7,8,9,10,12,13,14,15.
>
> 16 and 17 should be combined

Removed the ten specified logs from the working tree and combined the original
016/017 files here. Other log numbers and application code remain unchanged.
Updated the current Attendee plan and registration log references. No tests rerun
for this documentation-only cleanup; checked the archive contents, resulting
file list, merged source content and whitespace. No commit or push.

Recovery: all twelve original source files are archived at the Git-ignored
`.local-demo/log-backup.ljDMFf/logs-before-cleanup.tar.gz`. Archive was created
with explicit filenames and its contents verified with `tar -tzf` before removal.
The two source logs are preserved as separate phases above; no execution evidence
or student approval was invented.

### Subsequent renumbering

User request (verbatim):

> rename them to 003,004,005,006

Kept 001/002 and renumbered the remaining records in order: former 005 → 003,
006 → 004, 011 → 005, combined 016 → 006. Updated filenames, titles and current
documentation references, retaining verbatim prompts and historical command paths.
Verified the six-file inventory and whitespace. No application code, teammate logs,
test results or student review states changed; no commit or push.
