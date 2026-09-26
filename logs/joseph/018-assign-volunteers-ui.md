# 018 — Assign volunteers (Organizer desktop UI)

Date: 2026-09-25
Contributor: Joseph
Branch and starting revision: `feature-assign-volunteer` at `74d66ae`
Agent/tool: Cursor agent
Skills used (paths and revision or change description):
- `.agents/skills/desktop-ui-polish/SKILL.md` — Volunteers screen follows the existing Organizer sidebar/card shell (no new product policy)

## Objective

Add the Club Organizer desktop UI for the volunteer service implemented in log 016.

## Original prompts (verbatim)

> ok moving back to the feature-assign-volunteer, you implemented the logic but what about the UI and the front end?

## Response summary

Added a **Volunteers** sidebar screen to `OrganizerEventView`: an event list, the
assigned-volunteer list with **Remove selected**, and an assign form (registered
attendee picker, optional role limited to 60 characters, **Assign volunteer**),
plus success/error feedback. Wired `VolunteerService` into
`EventManagerApplication` using `NoEventRegistrations` and `JdbcVolunteerRepository`.

## Assumptions and design decisions

- Reuses the rules from log 016 (registered attendees only, optional role up to 60
  characters, duplicates rejected, ownership enforced in `VolunteerService`). The
  UI adds no new policy; errors from the service are shown as feedback.
- `VolunteerService.MAX_ROLE_LENGTH` was made public so the role field can limit input.
- With `NoEventRegistrations`, the picker is always empty; the UI shows an
  explanatory empty state and disables the assign controls.
- A volunteer whose registration no longer exists is shown as
  "Attendee no longer registered — role".
- Unresolved policy (unchanged from 016): cancellation effects, caps,
  notifications, attendee sign-up, volunteer QR check-in.

## Files changed

- `src/main/java/seedu/eventmanager/ui/OrganizerEventView.java`
- `src/main/java/seedu/eventmanager/ui/EventManagerApplication.java`
- `src/main/java/seedu/eventmanager/volunteer/VolunteerService.java` (`MAX_ROLE_LENGTH` public)
- `src/main/java/seedu/eventmanager/volunteer/README.md`
- `docs/UserGuide.md` (Feature Summary row and "Assigning volunteers" section)
- `logs/joseph/018-assign-volunteers-ui.md` (this log)

## Commands actually executed

Working directory: repository root.

```bash
./gradlew classes test --no-daemon
# exit 0; BUILD SUCCESSFUL; tests=109 skipped=10 failures=0 errors=0

EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://localhost:5432/event_manager_test \
EVENT_MANAGER_TEST_DB_USER=josephkwok \
  ./gradlew test --rerun-tasks --no-daemon
# exit 0; BUILD SUCCESSFUL; tests=109 skipped=2 failures=0 errors=0
```

## Actual verification results

- Compilation succeeded, and existing unit and Postgres integration tests pass
  against `event_manager_test`.
- No automated JavaFX UI tests exist; the new screen has **not** been exercised
  by an automated test.
- Manual `./gradlew run` check of the Volunteers screen: **not run** by the agent.

## Problems, corrections, and skill revisions

- None observed in this task.

## Outcome and limitations

The Volunteers screen compiles and is wired in. Assigning volunteers cannot be
demonstrated end to end until Attendee registration implements `EventRegistrations`.
This branch does not yet include main's request-venue fixes or the log 017 commit
from `feature-capcity`. Not committed or pushed.

Suggested commit message:

Add Organizer Volunteers screen for assigning registered attendees.

## AI-generated mini reflection

The task connected the tested volunteer service to the Organizer shell without
adding policy. Its main limitation is that the assign path shows only the empty
state until registrations exist and has no UI automation; the next step is a manual
run and, once Johannsen's registration lands, an end-to-end assign check.

## Follow-up: branch integration merges

Prompts (verbatim):

> ok, I will merge that. before i merge the feature-asign-volunteer branch. Can you merge the bug fixes of the authentication and the club UI to this branch as well. https://github.com/CS3227-2610-MP2-Orbit/CS3227-2610-MP2/tree/bug-club-organizer-authentication

> im waitgin for my teamate's approval to merge, but can u merge it with his PR now. Assuming i will merge it https://github.com/CS3227-2610-MP2-Orbit/CS3227-2610-MP2/pull/26

Decisions:

- `origin/bug-club-organizer-authentication` (b904e94, log 019) was a direct
  descendant of this branch, so it was fast-forwarded; no new commit.
- PR #26 (`origin/attendee-registration-handoff`, 263d833) was merged with
  `--no-ff`. The only conflict, `registration/README.md`, was resolved with
  #26's version, which records the agreed contract and replaces this branch's
  "proposed contract" text. #26 also adds a registration CI step.
- Merge commit 5544afe created locally with the user's authorization. Not pushed.
- `EventManagerApplication` still wires `NoEventRegistrations`; replacing it with
  `JdbcEventRegistrations` plus `RegistrationDatabaseMigration` was not done here.

Commands actually executed (temporary databases created with `createdb` and
dropped with `dropdb` afterwards):

```bash
git merge --ff-only origin/bug-club-organizer-authentication
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://localhost:5432/event_manager_test_ffmerge EVENT_MANAGER_TEST_DB_USER=josephkwok ./gradlew test --rerun
# BUILD SUCCESSFUL; tests=124 failures=0 errors=0 skipped=2
git merge --no-ff --no-commit origin/attendee-registration-handoff
# CONFLICT in src/main/java/seedu/eventmanager/registration/README.md
git checkout --theirs -- src/main/java/seedu/eventmanager/registration/README.md
EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://localhost:5432/event_manager_test_m26 EVENT_MANAGER_TEST_DB_USER=josephkwok ./gradlew classes test --rerun
# BUILD SUCCESSFUL; tests=144 failures=0 errors=0 skipped=2
git commit --no-edit
```

The two skipped tests are in `PostgreSqlVenueAdministratorIntegrationTest`
(Jordan's), which was not enabled by these environment variables. No manual UI run
was performed after either merge.

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
