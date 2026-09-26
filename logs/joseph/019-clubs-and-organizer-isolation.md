# 019 — Create clubs and per-account Organizer data

Date: 2026-09-26
Contributor: Joseph
Branch and starting revision: `bug-club-organizer-authentication` at `6be8152`
Agent/tool: Cursor agent
Skills used (paths and revision or change description):
- `.agents/skills/requirements-and-acceptance/SKILL.md` — policy questions asked before implementation
- `.agents/skills/test-driven-implementation/SKILL.md` — red/green for `ClubService`
- `.agents/skills/desktop-ui-polish/SKILL.md` — Clubs screen in the existing Organizer shell

## Objective

Let Club Organizers create clubs in the UI, and make each Club Organizer account
see and manage only its own data.

## Original prompts (verbatim)

> i also want to allow the create club section UI section somewhere, so that club organizers can create clubs.
>
> additionally, I want to make it so that each club organizer account has access to their own data and stuff. can you help me implement this?

## Response summary

Added a `club` package (`Club`, `ClubAuditRecord`, `ClubRepository`,
`JdbcClubRepository`, `ClubService`), migration `db/organizer/V4__create_organizer_clubs.sql`,
and a **Clubs** sidebar screen. After login, `ClubService.identityFor(actor)` builds
the `OrganizerIdentity` from the account's `users.user_id` and the clubs it owns in
`organizer_club`, replacing `EVENT_MANAGER_ORGANIZER_ID` / `EVENT_MANAGER_CLUB_IDS`.
All Organizer workflows already enforce ownership through `EventService`, so events,
venue requests, and volunteers are now scoped to the signed-in account.

## Assumptions and design decisions

Decisions chosen by the user (structured question, this session):

- A club has a single owner: the organizer account that created it.
- Club names are unique across the system, ignoring case.
- `.env` demo clubs are retired; existing demo events stay in the database but are not visible to any account.
- Scope is create and list only (no rename/delete).

Agent decisions:

- Name 1–80 characters after trimming; non-`CLUB_ORGANIZER` roles are rejected in `ClubService`.
- `organizer_club.owner_id` has no foreign key to `users` because Organizer
  migrations run without Flyway in integration tests; ownership comes from the
  authenticated `Actor`.
- Migration numbered V4 to avoid clashing with `V3__create_event_announcements.sql`
  on `feature-announcements`.
- The sidebar no longer shows the raw account UUID; the Home button label is unchanged
  because returning Home does not revoke the session token.

## Files changed

- `src/main/java/seedu/eventmanager/club/` (new: `Club`, `ClubAuditRecord`, `ClubRepository`, `ClubService`, `JdbcClubRepository`)
- `src/main/resources/db/organizer/V4__create_organizer_clubs.sql` (new)
- `src/main/java/seedu/eventmanager/storage/DatabaseMigration.java`
- `src/main/java/seedu/eventmanager/ui/OrganizerEventView.java`
- `src/main/java/seedu/eventmanager/ui/EventManagerApplication.java`
- `src/test/java/seedu/eventmanager/club/ClubServiceTest.java` (new)
- `src/test/java/seedu/eventmanager/club/JdbcClubRepositoryIntegrationTest.java` (new)
- `docs/UserGuide.md`, `docs/DeveloperGuide.md`
- `logs/joseph/019-clubs-and-organizer-isolation.md` (this log)

## Commands actually executed

Working directory: repository root.

```bash
./gradlew test --tests 'seedu.eventmanager.club.ClubServiceTest' --no-daemon
# red: exit non-zero; 12/12 failed (5 assertion failures, 7 explicit "not implemented" stub errors)

./gradlew test --tests 'seedu.eventmanager.club.ClubServiceTest' --no-daemon
# green: BUILD SUCCESSFUL; 12 tests, 0 failures

createdb event_manager_test_clubs
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://localhost:5432/event_manager_test_clubs \
EVENT_MANAGER_TEST_DB_USER=josephkwok \
  ./gradlew classes test --rerun-tasks --no-daemon
# BUILD SUCCESSFUL; tests=124 skipped=2 failures=0 errors=0 (club integration 3/3)
dropdb event_manager_test_clubs
```

## Actual verification results

- Unit: 12 `ClubServiceTest` cases (create, trim, blank, length boundaries,
  case-insensitive duplicate across organizers, non-organizer roles, own-clubs
  listing, identity derivation).
- Integration (temporary PostgreSQL database): round trip with audit,
  database-level duplicate rollback, and two accounts isolated from each other's
  events through `EventService`.
- Integration tests were written after `JdbcClubRepository` (coverage, not red/green).
- No automated JavaFX tests; manual `./gradlew run` check: **not run** by the agent.

## Problems, corrections, and skill revisions

- A combined shell command containing a table-truncation keyword was blocked by a
  repository hook earlier in the session; searches used the Grep tool instead.
- The shared `event_manager_test` database contains announcement tables from another
  branch, so a fresh temporary database was used and then dropped.

## Outcome and limitations

Club creation and per-account Organizer data are implemented and tested at unit and
integration level. Limitations: no club rename/delete or multi-organizer clubs; events
created under former `.env` clubs are hidden; returning Home does not revoke the
session; `feature-view-registration` and `feature-announcements` do not include this
change yet. Not committed or pushed.

Suggested commit message:

Add Organizer clubs and scope Organizer data to the signed-in account.

## AI-generated mini reflection

The main outcome is that ownership now comes from the authenticated account and the
database instead of local settings, reusing the existing `EventService` checks. The
main limitation is the missing manual UI check and the hidden legacy demo events; a
useful next step is a manual two-account run to confirm isolation end to end.

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
