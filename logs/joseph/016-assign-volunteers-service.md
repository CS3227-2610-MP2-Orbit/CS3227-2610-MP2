# 016 — Assign volunteers (Organizer service and persistence)

Date: 2026-09-25
Contributor: joseph
Branch and starting revision: `feautre-request-venue` @ `d770614`
Agent/tool: Cursor Agent
Skills used (paths and revision or change description):
- `.agents/skills/test-driven-implementation/SKILL.md` — red/green for `VolunteerService`

## Objective

Implement the Club Organizer side of "Assign volunteers" without building
Attendee (Johannsen) features.

## Original prompts (verbatim)

> actually what does assign volunteer even mean?

> I'm assuming assign volunteers to atendee? Meaning johannsen's side, what do you think?

> dont think of the short term right now, im thinking of long term, where Johansen will finish his featuee soonand stuff

> can you implement the assign volunteer logic on my side now, dont need to do johannsen stuff

## Response summary

Added `VolunteerService` (assign, remove, list, available attendees), a JDBC
repository with atomic business audit records, an organizer schema migration,
and a proposed `EventRegistrations` contract with a placeholder implementation.
No UI.

## Assumptions and design decisions

- **Team feature list (source):** "Assign volunteers" under Club Organizer; no
  MP2 spec text defines it further.
- **Adopted from discussion (user-directed, long-term design):** volunteers are
  attendees registered for the event; eligibility checked via
  `EventRegistrations`.
- **Agent decisions (not team-agreed policy):** optional role label, max 60
  chars; duplicates rejected; any owned event (no status restriction, because
  registration will apply to published events); attendee names not persisted
  (read from registrations at list time).
- **Proposal:** `EventRegistrations` contract, pending agreement with Johannsen.
- **Unresolved, not implemented:** behavior on registration cancellation,
  volunteer caps, notifications, attendee sign-up, volunteer QR check-in.

## Files changed

- `src/main/java/seedu/eventmanager/registration/EventRegistrations.java` (new)
- `src/main/java/seedu/eventmanager/registration/RegisteredAttendee.java` (new)
- `src/main/java/seedu/eventmanager/registration/NoEventRegistrations.java` (new)
- `src/main/java/seedu/eventmanager/registration/README.md`
- `src/main/java/seedu/eventmanager/volunteer/VolunteerService.java` (new)
- `src/main/java/seedu/eventmanager/volunteer/VolunteerRepository.java` (new)
- `src/main/java/seedu/eventmanager/volunteer/JdbcVolunteerRepository.java` (new)
- `src/main/java/seedu/eventmanager/volunteer/VolunteerAssignment.java` (new)
- `src/main/java/seedu/eventmanager/volunteer/VolunteerAuditRecord.java` (new)
- `src/main/java/seedu/eventmanager/volunteer/AssignedVolunteer.java` (new)
- `src/main/java/seedu/eventmanager/volunteer/README.md`
- `src/main/resources/db/organizer/V2__create_event_volunteers.sql` (new)
- `src/main/java/seedu/eventmanager/storage/DatabaseMigration.java` — runs both organizer migrations
- `src/test/java/seedu/eventmanager/volunteer/VolunteerServiceTest.java` (new)
- `src/test/java/seedu/eventmanager/volunteer/JdbcVolunteerRepositoryIntegrationTest.java` (new)
- `src/test/java/seedu/eventmanager/event/JdbcEventRepositoryIntegrationTest.java` — truncate includes volunteer tables (new FK)
- `src/test/java/seedu/eventmanager/event/EventCapacitySyncIntegrationTest.java` — same

## Commands actually executed

```sh
# Red (service stubbed with UnsupportedOperationException)
./gradlew test --tests 'seedu.eventmanager.volunteer.VolunteerServiceTest' --no-daemon
# exit: task failed; all tests failed with UnsupportedOperationException (compiled OK)

# Green
./gradlew test --tests 'seedu.eventmanager.volunteer.VolunteerServiceTest' --no-daemon
# BUILD SUCCESSFUL; tests=17 skipped=0 failures=0

# Local test database (already existed)
/Applications/Postgres.app/Contents/Versions/latest/bin/psql -d postgres -tAc \
  "SELECT 1 FROM pg_database WHERE datname='event_manager_test'"

# Integration (dedicated test DB)
EVENT_MANAGER_TEST_DB_URL='jdbc:postgresql://localhost:5432/event_manager_test' \
  EVENT_MANAGER_TEST_DB_USER=josephkwok \
  ./gradlew test --tests 'seedu.eventmanager.volunteer.*' \
  --tests 'seedu.eventmanager.event.JdbcEventRepositoryIntegrationTest' \
  --tests 'seedu.eventmanager.event.EventCapacitySyncIntegrationTest' --no-daemon
# BUILD SUCCESSFUL; volunteer integration 4/4, service 17/17,
# JdbcEventRepositoryIntegrationTest 1/1, EventCapacitySyncIntegrationTest 3/3, 0 skipped

# Full suite (no DB env set)
./gradlew test --no-daemon
# BUILD SUCCESSFUL; tests=109 skipped=10 failures=0 errors=0
```

## Actual verification results

- Unit: 17 `VolunteerServiceTest` cases (normal, role boundaries, unregistered,
  duplicate, ownership, unknown event, remove, list, available attendees).
- Integration (real PostgreSQL, faked registrations): persistence round trip with
  audit, database-level duplicate rollback, missing remove without audit, foreign
  key rejection without audit.
- Full suite passed; 10 skips are DB-gated tests when no test DB env is set.
- No UI or end-to-end desktop verification (no UI exists for this feature).

## Problems, corrections, and skill revisions

- Adding the `event_volunteer` foreign key would break existing integration test
  truncation of `organizer_event`; updated those truncate statements.
- Earlier session (log 014) ran `EventCapacitySyncIntegrationTest` with
  `EVENT_MANAGER_TEST_DB_URL` pointing at the app database `event_manager`; that
  test truncates venue and event tables, so local demo data there would have
  been cleared. This session used `event_manager_test` instead.

## Outcome and limitations

Organizer volunteer assignment logic and storage exist and are tested, but the
feature is not available in the desktop app (no UI, placeholder registrations).
It becomes usable once Attendee registration implements `EventRegistrations`
and an Organizer screen is added.

Suggested commit message:

```
Add Organizer volunteer assignment service and persistence.
```

## AI-generated mini reflection

Coding against a small registration contract let the Organizer side be built and
tested before the Attendee feature exists. Main limitation: the contract is a
proposal, and several volunteer policies remain undecided.

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
