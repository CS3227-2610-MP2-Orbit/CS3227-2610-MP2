# 027 — Fix CI silent test skips and add linting

Date: 2026-09-28
Contributor: Joseph
Branch and starting revision: `main` at `5ce137d`
Agent/tool: Cursor
Skills used: none as a formal invocation. Build and CI configuration change; the repository test suite is the verification.

## Objective

Close two CI gaps: database-gated tests skipped while the step still passed, and no lint plugin behind the "Lint check" step.

## Original prompts (verbatim)

> can you fix this?

with a screenshot listing:

> Area: CI gap: some tests silently skip — From log 014: the cross-role and event-publish integration tests need EVENT_MANAGER_TEST_DB_URL, but the step that runs them doesn't set it. They skip, and the step still passes. A green run doesn't mean every database test ran.
>
> Area: CI gap: no linting — The "Lint check" step prints "No lint plugin is configured". There is no Checkstyle, Spotless or PMD, which matters for the 25% Code Quality score.

## Response summary

Set the `EVENT_MANAGER_TEST_DB_*` variables for the whole CI job, added a Gradle guard that turns a skipped test into a failure, added a CI step for the organizer packages that previously had no step, and configured Checkstyle for both source sets with the violations it found fixed.

## Assumptions and design decisions

- A skip is only safe if it is deliberate. `-PfailOnSkippedTests` is opt-in so a local run without a database still works, while CI always passes it.
- Checkstyle rules target defects and dead code, not formatting taste, so a violation is worth fixing.
- Test sources use a second config. The project's tests deliberately use `method_scenario_result` naming, wildcard imports, wide fixture lines, and `catch (Throwable)` in the manual JavaFX smoke harnesses. Enforcing those in a teammate's files would have meant rewriting about 240 lines of their tests for no defect found, so those rules apply to `src/main` only.
- The repeated inline `psql` drop/create blocks became `reset-ci-database.sh`.

## Files changed

- `.github/workflows/ci.yml`
- `build.gradle`
- `config/checkstyle/checkstyle.xml` (new)
- `config/checkstyle/checkstyle-test.xml` (new)
- `reset-ci-database.sh` (new, executable)
- Main sources for the 6 Checkstyle violations: `attendee/InboxDispatcher.java`, `storage/JdbcAuthorizationService.java`, `storage/JdbcInboxRepository.java`, `ui/AttendanceHistoryView.java`, `ui/RegistrationFeedback.java`, `ui/VenueAdministratorDashboardView.java`
- Unused imports in `MainTest.java`, `service/VenueAdministratorServiceFactoryTest.java`, `service/VenueAdministratorServiceTest.java`, `ui/AttendanceHistorySmoke.java`, `ui/AttendeeInboxSmoke.java`
- `logs/joseph/027-ci-skips-and-linting.md`

No application behaviour was changed. The `JdbcDatabase` `catch (RuntimeException | Error)` was kept because rolling back and rethrowing at a transaction boundary is correct; the rule was narrowed to ban `Throwable` only.

## Commands actually executed

From `/Users/josephkwok/Desktop/Website/cs3227/MP2`:

1. `./gradlew compileJava compileTestJava --no-daemon` — BUILD SUCCESSFUL.
2. Full suite with only `EVENT_MANAGER_TEST_DB_*` set — tests=328, skipped=5, failures=0. The 5 skips were `PostgreSqlVenueAdministratorIntegrationTest`.
3. Full suite with `DATABASE_INTEGRATION_TESTS=true` added — tests=328, **skipped=0**, failures=0. This confirmed the variable set that makes every test run.
4. `./gradlew checkstyleMain checkstyleTest --no-daemon` — first run: main 7 violations, test 338. After fixes and the test config: BUILD SUCCESSFUL.
5. `./gradlew test --rerun-tasks -PfailOnSkippedTests --no-daemon` (no database variables) — BUILD FAILED, listing every skipped test by name. This is the negative case the guard exists for.
6. Same command with the database variables set — BUILD SUCCESSFUL.
7. `./gradlew test --rerun-tasks -PfailOnSkippedTests --warning-mode all` — showed `afterTest(Closure)` is deprecated for Gradle 10; replaced with `addTestListener(TestListener)` and re-ran with no deprecation warning.
8. `./gradlew build -PfailOnSkippedTests --no-daemon` with the database variables — BUILD SUCCESSFUL.
9. `bash -n reset-ci-database.sh` and a `yaml.safe_load` parse of `ci.yml` — both OK.

The review database `mp2_review_2609` was used for these runs.

## Actual verification results

- The skip guard fails the build and names the skipped tests when the database variables are missing, and passes when they are set. Both directions were observed.
- Checkstyle passes on main and test after 6 main-source fixes and 7 unused-import removals.
- The full build passes with the guard enabled.
- Not run: the GitHub Actions workflow itself. The YAML parses and the commands were exercised locally, but the hosted run has not been observed.

## Problems, corrections, and skill revisions

- Expanding the `javafx.scene.control.*` import first missed `ListCell`, which broke compilation. Caught by the next Checkstyle run and fixed.
- The first skip guard used `afterTest(Closure)`, deprecated for Gradle 10. Replaced with `addTestListener`.
- The first test Checkstyle config produced 338 violations, almost all house style in a teammate's files. The config was narrowed rather than reformatting their tests.

## Outcome and limitations

Both CI gaps are closed in the working tree, uncommitted. The lint gate covers Checkstyle only; no PMD, SpotBugs or Spotless was added. The workflow has not been run on GitHub. Nothing was committed or pushed.

Suggested commit message: `ci: fail on skipped tests and add Checkstyle linting`
