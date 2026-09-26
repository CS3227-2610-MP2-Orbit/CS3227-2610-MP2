# 014 — Capacity sync unit and integration tests

Date: 2026-09-24
Contributor: joseph
Branch and starting revision: `feautre-request-venue`
Agent/tool: Cursor Agent (Composer)
Skills used (paths and revision or change description):
- `.agents/skills/test-driven-implementation/SKILL.md` — regression coverage for
  capacity→attendance sync (behavior already implemented)

## Objective

Add unit and integration tests for draft capacity edits syncing open venue-request
`expectedAttendance`, without changing product behavior.

## Original prompts (verbatim)

> can you create some unit testing and integrationg testing for this

## Response summary

Expanded `EventServiceTest` edge cases; extended the in-memory Organizer→Admin
pipeline e2e to assert attendance sync; added
`EventCapacitySyncIntegrationTest` against PostgreSQL (gated by test DB env).

## Assumptions and design decisions

- Unit tests use in-memory fakes (always run).
- Integration tests require `EVENT_MANAGER_TEST_DB_URL` **or**
  `DATABASE_INTEGRATION_TESTS=true` (uses DatabaseBootstrap / `.env`).
- Covered: open DRAFT/SUBMITTED sync; APPROVED/REJECTED unchanged; no request;
  stale version leaves attendance alone; JDBC status gate.

## Files changed

- `src/test/java/seedu/eventmanager/event/EventServiceTest.java`
- `src/test/java/seedu/eventmanager/event/EventCapacitySyncIntegrationTest.java` (new)
- `src/test/java/seedu/eventmanager/e2e/OrganizerToAdminVenuePipelineE2ETest.java`

## Commands actually executed

```sh
./gradlew test --tests 'seedu.eventmanager.event.EventServiceTest' \
  --tests 'seedu.eventmanager.e2e.OrganizerToAdminVenuePipelineE2ETest' --no-daemon
# exit 0

DATABASE_INTEGRATION_TESTS=true \
  EVENT_MANAGER_TEST_DB_URL='jdbc:postgresql://localhost:5432/event_manager' \
  EVENT_MANAGER_TEST_DB_USER=josephkwok \
  ./gradlew test --tests 'seedu.eventmanager.event.EventCapacitySyncIntegrationTest' \
  --no-daemon
# exit 0; XML: tests=3 skipped=0 failures=0
```

## Actual verification results

- Unit / e2e: BUILD SUCCESSFUL.
- Integration: 3 Postgres cases executed (not skipped), all passed.

## Problems, corrections, and skill revisions

None. Coverage addition on already-green behavior (regression, not red/green
feature TDD).

## Outcome and limitations

Sync behavior is covered at unit, in-memory pipeline, and real JDBC boundaries.
JavaFX UI still not automated.

Suggested commit message:

```
Add unit and Postgres tests for capacity-to-attendance sync on edit.
```

## AI-generated mini reflection

Strongest outcome: Postgres proof that status-gated attendance updates work end
to end with `EventService.editEvent`. Limitation: integration suite stays env-
gated so CI without a DB will skip unless configured.

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
