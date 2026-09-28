# 015 — Release workflow, monitoring and demo data

Date: 28 September 2026
Contributor: Johannsen
Branch and starting revision: `production-readiness`, created from `main` at `5ce137d`
Agent/tool: Claude Code (Claude Opus 5.5), single agent, no subagents
Skills used: `test-driven-implementation`, `observability-and-error-handling`,
`database-migration-and-integrity`, `security-and-rbac` (read before use; not changed).
`security-and-rbac` applied only to the bug-fix work that was later reverted.

## Objective

Close the production-readiness gaps from the MP2 submission review in one PR:
continuous delivery (release jars), monitoring, and robustness for public users,
including opt-in demo data. CI changes and the reviewed product bugs are out of scope
(owned by Joseph and Jordan).

## Original prompts (verbatim)

First prompt, in order. Two pasted blocks are summarised in brackets, not reproduced:

> Do up everything below in one pr, do not need to add more issues
>
> [pasted: the agent's "Production-level: CI/CD, automated testing, monitoring" table
> and the seven confirmed bugs from log 014]
>
> do not do CI, joseph is doing it. for the postgres
>
> [pasted: the Developer Guide "Setting up, getting started" section]
>
> , make it such that there is mockdata that the user can seed from

Answers to the agent's clarifying questions: release/CD workflow "Include in this PR";
capacity rule "Room caps attendance (Recommended)"; seed method "Opt-in Gradle task
(Recommended)"; teammate code "Fix all in this PR".

Follow-ups during the task:

> for  How should testers load the mock data, it should be updated in the Developer guide as well in this part

> actually dont need to fix the bugs, please revert.

> ill pass it over to joseph and jordan as it is their bugs

> please only do the CD, the monitoring and also the robustness for public user

> do i need to add a .env.example or something so the user can copy paste?

## Response summary

Added a per-OS runnable `releaseJar`, a tag-triggered `release.yml` that builds and
smoke-starts jars on Linux, Windows and macOS and publishes a GitHub Release, local
monitoring (rotating diagnostic log, uncaught-error logging, in-memory metrics,
database health check shown in the app), a clearer first-run error screen with
**Try again**, `.env.example`, and opt-in demo data (`./gradlew seedDemo` /
`--seed-demo`) created through the real services. Updated the User Guide,
Developer Guide and README. The seven bug fixes were implemented and then fully
reverted at the user's request.

## Assumptions and design decisions

- Team decision (user): Joseph owns CI; this PR adds only `release.yml` and does not
  touch `ci.yml`. Bugs 1–7 are handed to Joseph and Jordan and are not changed here.
- One jar per OS, not one universal jar: JavaFX natives are platform-specific and the
  Intel and Apple Silicon macOS natives share file names. No Intel macOS jar is built;
  the guides say so.
- No new Gradle plugin: `mergeServiceFiles` merges `META-INF/services` so Flyway keeps
  its PostgreSQL plugin in the fat jar.
- Demo data goes through `ClubService`, `EventService`, `OrganizerVenueRequestService`,
  `VenueAdministratorService`, `RegistrationService`, `VolunteerService` and
  `AnnouncementService` with fixed clocks, so seeded rows obey the same rules and
  create normal audit records. It is opt-in, never automatic, and idempotent
  (skips when `demo_organizer` exists). Event times are relative to the seeding time.
- Monitoring is local diagnostics for a desktop app, not hosted monitoring or alerting;
  the Developer Guide says this explicitly. Log events carry roles and exception types
  only, never passwords, tokens, usernames or connection strings. Library exception
  stack traces are logged as-is; the guide notes they can include a JDBC URL.
- `Monitoring.metrics()` replaces `NoopMetrics` as the default in the shared
  `VenueAdministratorServiceFactory`, so its existing counters become visible.
- `EVENT_MANAGER_LOG_DIR` is read from the process environment only, so it was removed
  from `.env.example` to avoid a misleading setting.
- Release assets include `env.example` because jar-only users never see the repository.

## Files changed

- `build.gradle` — `seedDemo`, `mergeServiceFiles`, `releaseJar`
- `.github/workflows/release.yml` (new)
- `.env.example` (new)
- `src/main/java/seedu/eventmanager/Main.java` — `VERSION`, `--seed-demo`
- `src/main/java/seedu/eventmanager/demo/DemoDataSeeder.java` (new)
- `src/main/java/seedu/eventmanager/common/DiagnosticLog.java`, `InMemoryMetrics.java`,
  `Monitoring.java` (new)
- `src/main/java/seedu/eventmanager/storage/DatabaseHealth.java` (new)
- `src/main/java/seedu/eventmanager/ui/EventManagerApplication.java` — log install,
  health check, status line, error screen with **Try again**, login/stop events
- `src/main/java/seedu/eventmanager/ui/HomeAuthenticationView.java` — `login_failed`
- `src/main/java/seedu/eventmanager/service/VenueAdministratorServiceFactory.java` —
  default metrics
- Tests (new): `demo/DemoDataSeederIntegrationTest`, `common/InMemoryMetricsTest`,
  `common/DiagnosticLogTest`, `storage/DatabaseHealthTest`
- `docs/UserGuide.md`, `docs/DeveloperGuide.md`, `README.md`
- This log

Not part of this change and left untouched: the pre-existing uncommitted edits to
`docs/AgenticSE.md`, `tools/graders/*`, `evals/`, `tools/evals/` and logs 011/014.

## Commands actually executed

Working directory: repository root. Test DB variables below are
`EVENT_MANAGER_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55448/mp2_attendee_test_johannsen
EVENT_MANAGER_TEST_DB_USER=mp2_demo_app EVENT_MANAGER_TEST_DB_PASSWORD=''` with
`JAVA_TOOL_OPTIONS=-Duser.timezone=UTC`.

```sh
git fetch origin --prune && git switch -c production-readiness
./gradlew test --tests 'seedu.eventmanager.e2e.CrossRoleWorkflowIntegrationTest'   # bug work, later reverted
./gradlew test --tests 'seedu.eventmanager.demo.*'
./gradlew test --tests 'seedu.eventmanager.common.*'
./gradlew test --tests 'seedu.eventmanager.storage.DatabaseHealthTest'
./gradlew releaseJar
actionlint .github/workflows/release.yml
DATABASE_URL=… DATABASE_INTEGRATION_TESTS=true <test DB vars> ./gradlew build releaseJar --rerun-tasks
java -jar build/release/EventVenueManager-0.1.0-macos-arm64.jar --version        # JDK 25
java -jar build/release/EventVenueManager-0.1.0-macos-arm64.jar --seed-demo      # twice, scratch DB
java -jar build/release/EventVenueManager-0.1.0-macos-arm64.jar                  # GUI, 10 s, with and without .env
```

The scratch database `mp2_release_smoke_20260928` was created with the local
`mp2_bootstrap` role after `mp2_demo_app` was refused `CREATE DATABASE`.

## Actual verification results

- Red: `InMemoryMetricsTest` (3) and `DiagnosticLogTest` (3) failed against stubs that
  threw `UnsupportedOperationException`; `DatabaseHealthTest` (2) failed the same way.
  Green: all 8 passed after implementation (exit 0).
- `DemoDataSeederIntegrationTest` (2) passed on an isolated schema. It was written after
  the seeder, so it is regression coverage, not a red/green cycle.
- Full build: `BUILD SUCCESSFUL`, 338 JUnit tests, 0 failures, 0 skipped (exit 0).
  This includes the PostgreSQL suites; the 338 are the 328 existing tests plus 10 new.
- `releaseJar` (macOS arm64 only, locally): manifest has `Main-Class` and
  `Enable-Native-Access`; merged Flyway service file lists the PostgreSQL plugin;
  jar contains the macOS JavaFX `.dylib` files.
- Jar on a fresh database: `--seed-demo` ran all migrations and loaded the data
  (exit 0); a second run printed "already present" and changed nothing (exit 0);
  7 events existed with the expected statuses.
- GUI launch of the jar (real JavaFX, about 10 s each, then killed):
  - Without `.env`, the log recorded `app_started` and `database_not_configured`, and a
    screenshot showed the new error screen with **Try again**.
  - With `.env`, the log recorded `database_health result=up`, and a screenshot showed
    the login screen with the status line.
  - `app_stopped` was not verified because the process was killed, not closed.
- `actionlint` on `release.yml`: exit 0 (after quoting a `sha256sum` glob).
- Not run: the release workflow itself on GitHub (it needs a pushed tag); Windows and
  Linux jars; logging in and using each role with the demo accounts through the UI.

## Problems, corrections, and skill revisions

- Bugs 1–7 were implemented test-first. Six red cross-role integration tests failed on
  their assertions; the fixes turned them green. The user then asked to revert, and
  every bug-fix file was restored with `git restore` or deleted. The demo seeder was
  changed back to the existing service constructors. None of that work remains.
- Changing `ActiveBooking` during the bug work broke `EventPublishIntegrationTest`;
  that change was reverted along with the fixes.
- `.env.example` first listed `EVENT_MANAGER_LOG_DIR`, which the app does not read
  from `.env`; the line was removed.
- The first DG monitoring text overclaimed that logs never contain connection strings;
  it was corrected to mention library stack traces.
- After PR #47 was opened, `main` gained PR #45 (Joseph's guide and README edits), and
  GitHub reported the PR as conflicting. `git merge origin/main` conflicted in
  `README.md`, `docs/DeveloperGuide.md` and `docs/UserGuide.md`. Each conflict was
  resolved by keeping Joseph's wording and adding this PR's `.env.example`, jar,
  demo-data and quick-start text, then removing a duplicate README paragraph.
  No code files conflicted, so the build was not re-run for the merge.
- No skill needed revision.

## Outcome and limitations

Release packaging, a release workflow, local monitoring, a clearer first-run screen
and demo data are implemented and verified locally as listed above. Not included:
CI changes, the reviewed bugs, the `master` branch, GitHub Pages, and an Intel macOS
jar. Publishing a release still needs someone to push a `v*` tag.

Suggested commit message: `feat: add release jars, local monitoring and demo data`

## AI-generated mini reflection

The task showed how shared-shell changes can raise production readiness without touching
role workflows. The strongest outcome is the real-jar evidence: a fresh database
migrated and seeded from the fat jar, which caught the Flyway service-file risk
before release. The main limitation is that the Windows and Linux jars and the
release workflow are unverified until a tag runs on GitHub. A useful next step is to
tag a release candidate and check each jar on its OS.

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
