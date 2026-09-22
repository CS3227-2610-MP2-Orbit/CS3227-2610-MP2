# 006 — Club Organizer create and edit events

Date: 2026-09-22
Contributor: Joseph
Branch and starting revision: `joseph-edit-skills`, `068b3d3`
Agent/tool: Codex
Skills used: `.agents/skills/test-driven-implementation/SKILL.md` (read and applied; build setup required correction before the behavioral red run)

## Objective

Implement the first Club Organizer feature, create/edit events, through a
verified red/green/refactor cycle without changing the unrelated Agentic SE
graders.

## Original prompts (verbatim)

> can you implement the first feature. do not need to ask for recommendations, just work on it untilit is finisehd

> can you use the tree of thoughts to see different ways to fix this, and then choose the best option.
>
> The better design is:
>
> - Date picker
> - Start-time and end-time fields such as `18:00`
> - UI labelled `Singapore Time (SGT)`
> - Friendly display such as `1 Oct 2026, 6:00 PM`
> - Convert to UTC internally before storing it in PostgreSQL

## Response summary

Implemented organizer-owned event creation, listing, viewing, and draft editing.
The workflow validates event data, enforces club ownership, prevents stale
updates, and records event changes with sanitized business audit data. Added a
JavaFX create/edit screen, PostgreSQL repository and schema migration, safe
environment-based configuration, focused tests, documentation, and the missing
Gradle wrapper.
The related usability follow-up replaced raw UTC timestamp entry with JavaFX
date pickers and validated `HH:mm` Singapore-time fields while retaining UTC
storage.

## Assumptions and design decisions

- “First feature” means the first Joseph/Club Organizer item: create/edit events.
- New events are drafts. Publication and post-publication editing belong to a
  later workflow because venue approval does not yet exist.
- A valid draft has a non-blank title, a start before its end, and positive
  capacity. No future-only date rule was invented.
- The authenticated organizer identity is injected into `EventService`. The UI
  uses an environment-backed development adapter until shared authentication is
  implemented.
- Event and audit writes are one repository transaction. Audit records contain
  IDs, action, time, and version but no credentials or unnecessary personal data.
- PostgreSQL is the user-selected database. The official pgJDBC release page was
  checked before selecting driver `42.7.13`; JavaFX uses version 25 to match the
  project JDK.
- Four time-input designs were compared: raw UTC text, a third-party date-time
  picker, hour/minute spinners, and native JavaFX date pickers with `HH:mm`
  fields. Native controls plus validated time text were selected to avoid a new
  dependency while providing familiar input and minute-level precision.
- `Asia/Singapore` is the presentation zone. UI values are converted to
  `Instant` before entering the event service, so domain and database contracts
  remain unchanged.

## Files changed

- Build support: `build.gradle`, `gradlew`, `gradlew.bat`, and `gradle/wrapper/`.
- Event domain/application/persistence classes under
  `src/main/java/seedu/eventmanager/event/`.
- Shared exceptions and PostgreSQL configuration/migration support under
  `common/`, `storage/`, and `src/main/resources/db/migration/`.
- JavaFX application, organizer event view, and form parser under `ui/`; updated
  `Main.java`.
- Singapore date/time conversion utility and focused UI-boundary tests.
- Unit tests under `src/test/java/seedu/eventmanager/`.
- `README.md`, `docs/UserGuide.md`, `docs/DeveloperGuide.md`, and the event
  package README.
- `tools/graders/` was not changed.

## Commands actually executed

From `/Users/josephkwok/Desktop/Website/cs3227/MP2`:

1. `sed -n '1,320p' .agents/skills/test-driven-implementation/SKILL.md && git status --short --branch && git diff --stat && rg --files src test docs logs | sort` — exit 2 because the nonexistent top-level `test` path was passed to `rg`; skill, status, and existing files were still inspected.
2. `git diff -- && git status --porcelain=v1 && java -version && gradle --version` — exit 127; Java 25.0.3 was present and a system `gradle` command was absent.
3. Repository/cache inspection located Gradle 9.6.1 and JUnit artifacts under the user Gradle cache.
4. `/Users/josephkwok/.gradle/wrapper/dists/gradle-9.6.1-bin/4ticwg1pgcbps2hj28r8so764/gradle-9.6.1/bin/gradle test --no-daemon` — exit 1; the baseline could not start JUnit because `junit-platform-launcher` was missing.
5. `/Users/josephkwok/.gradle/wrapper/dists/gradle-9.6.1-bin/4ticwg1pgcbps2hj28r8so764/gradle-9.6.1/bin/gradle test --tests seedu.eventmanager.event.EventServiceTest --no-daemon` — exit 1; seven compiled tests failed on the deliberately unimplemented `EventService` methods.
6. The same focused test command after implementation — exit 0; all focused event tests passed.
7. `/Users/josephkwok/.gradle/wrapper/dists/gradle-9.6.1-bin/4ticwg1pgcbps2hj28r8so764/gradle-9.6.1/bin/gradle test --no-daemon` — exit 0 after the PostgreSQL and JavaFX layers were added.
8. `/Users/josephkwok/.gradle/wrapper/dists/gradle-9.6.1-bin/4ticwg1pgcbps2hj28r8so764/gradle-9.6.1/bin/gradle wrapper --gradle-version 9.6.1 --distribution-type bin --no-daemon` — exit 0.
9. `./gradlew clean test --no-daemon && git diff --check && git status --short` — exit 0; the wrapper build and all tests passed, the diff check was clean, and status showed only this task's tracked/untracked files plus the pre-existing ignored build/cache outputs.
10. The first temporary-PostgreSQL integration command was rejected before execution because its cleanup used recursive removal; no server was started by that command.
11. An isolated PostgreSQL 18 cluster was initialized under `/tmp`, the focused `JdbcEventRepositoryIntegrationTest` was run with `EVENT_MANAGER_TEST_DB_URL`, and a read-only SQL count check returned `1,2` for one event and two audit records — exit 0. The server was stopped by the shell trap.
12. The complete `./gradlew clean test --no-daemon` suite was run against another isolated PostgreSQL 18 cluster, followed by the same count query — exit 0; all 12 tests passed with no skips and the query returned `1,2`.
13. `./gradlew run --args=--version --no-daemon && git diff --check && git status --short --branch` — exit 0; the packaged entry point printed `Event Venue Manager 0.1.0` and the diff check passed.
14. The application was launched with `./gradlew run --no-daemon` against an isolated PostgreSQL 18 cluster and development organizer/club IDs. JavaFX reached a live running state; the process was then intentionally interrupted with Ctrl-C (exit 130), which also stopped PostgreSQL through the shell trap.
15. After adding JavaFX native-access configuration, the live launch smoke was repeated against a fresh isolated PostgreSQL cluster. The application remained live without the earlier native-access warning and was intentionally stopped with Ctrl-C (exit 130).
16. The final current-state check ran `./gradlew clean test --no-daemon` against a fresh isolated PostgreSQL 18 cluster, followed by `git diff --check` and a trailing-whitespace scan over changed source, tests, and documentation — exit 0.
17. After the final scope and credential-redaction refinements, the complete PostgreSQL-backed suite, `git diff --check`, a no-change assertion for `tools/graders`, XML result inspection, and final repository status inspection all completed successfully. The XML reports showed 12 tests, zero failures, zero errors, and zero skips.
18. The TDD skill, branch/status, organizer view, form parser, parser tests, and User Guide were inspected before the time-input change — exit 0; this confirmed that raw ISO-8601 UTC text was the current UI contract.
19. `./gradlew test --tests seedu.eventmanager.ui.EventFormParserTest --tests seedu.eventmanager.ui.SingaporeDateTimesTest --no-daemon` before implementation — exit 1; six compiled tests failed on the intentionally unimplemented Singapore conversion/parser methods.
20. The same focused test command after implementation — exit 0; all Singapore conversion and form-parser tests passed.
21. The complete `./gradlew clean test --no-daemon` suite was run against an isolated PostgreSQL 18 cluster, followed by `git diff --check` and XML result inspection — exit 0; all 16 tests passed with zero failures, errors, or skips.
22. The JavaFX application was launched against a fresh isolated PostgreSQL cluster using the development organizer adapter. It reached a live running state with the new date controls and was intentionally stopped with Ctrl-C (exit 130); the shell trap stopped PostgreSQL.

## Actual verification results

- Red: seven `EventServiceTest` cases failed because create, edit, get, and list
  raised `UnsupportedOperationException`; this matched the missing behavior.
- Green: the same focused suite passed after implementing validation,
  authorization, persistence calls, auditing, and version checks.
- Broader suite: passed after adding JavaFX compilation, pgJDBC, form parsing,
  safe database configuration, and the schema migration.
- PostgreSQL integration: all 12 tests passed without skips against PostgreSQL
  18.6. The integration assertion verified round-trip create/edit, two committed
  audit records, optimistic locking, and no audit addition for a stale write.
- JavaFX launch smoke: the app initialized and remained running against the
  isolated database until intentionally stopped. No scripted visual interaction
  was performed.
- Live JavaFX interaction was not run in this non-interactive tool session;
  compilation and controller-independent form tests do not prove rendered UI
  behavior.
- Singapore-time UI boundary tests verify that `1 Oct 2026 18:00 SGT` becomes
  `2026-10-01T10:00:00Z`, stored instants crossing Singapore midnight convert
  back to the correct date and time, friendly SGT text is produced, and missing
  or invalid inputs are rejected.
- The post-change JavaFX launch smoke reached a live state with the native date
  pickers against an isolated PostgreSQL database; visual interactions were not
  automated.

## Problems, corrections, and skill revisions

The first baseline test command exposed a Gradle 9 runtime requirement for the
JUnit Platform launcher. `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'`
was added before the behavioral red run. No skill file was changed.

## Outcome and limitations

The create/edit event workflow is implemented at the domain, PostgreSQL adapter,
and JavaFX UI levels. Organizers now enter and view Singapore-local dates and
times while persistence remains UTC. Shared login, venue approval, publication,
and detailed visual/UI interaction verification remain integration dependencies
rather than claims of this feature.

Suggested commit message: `feat: add organizer event creation and editing`

## AI-generated mini reflection

This task was the first time the repository's TDD skill was applied to an actual
MP2 product feature rather than a synthetic fixture or agent-workflow tool. The
strongest result was connecting authorization and validation to a working JavaFX
screen and verifying transactional event/audit persistence against a real
PostgreSQL server. The main limitation is that shared authentication is still
represented by a development identity. Replacing raw UTC entry with Singapore
date pickers also showed why storage formats should not leak into user-facing
forms. The UI has received only launch smoke checks, so future work should
integrate the team authentication contract and add automated interaction tests
for the rendered form.

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
