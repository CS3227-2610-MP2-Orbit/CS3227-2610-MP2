# 013 — Manage event capacity (with venue-request sync)

Date: 2026-09-24
Contributor: joseph
Branch and starting revision: `feautre-request-venue` @ `9102926`
Agent/tool: Cursor Agent (Composer)
Skills used (paths and revision or change description):
- `.agents/skills/test-driven-implementation/SKILL.md` — EventCapacityService TDD
- `.agents/skills/desktop-ui-polish/SKILL.md` — Organizer Capacity sidebar screen

## Objective

Organizer-only Capacity screen to update **draft** event capacity and sync
`expectedAttendance` on open (`SUBMITTED`/`DRAFT`) venue requests the Organizer
owns—without changing Venue Admin UI, approve/reject, or booking logic.

## Original prompts (verbatim)

> Manage event capacity (with venue-request sync)
>
> Implement the plan as specified, it is attached for your reference. Do NOT edit
> the plan file itself.
>
> To-do's from the plan have already been created. Do not create them again. Mark
> them as in_progress as you work, starting with the first one. Don't stop until
> you have completed all the to-dos.

(Prior conversation established the plan: Organizer Capacity only; additive
`updateExpectedAttendance` on shared repo for open requests; no Admin service/UI
changes.)

## Response summary

Implemented `EventCapacityService.updateCapacity` with optimistic versioning,
ownership/draft checks, `UPDATE_CAPACITY` audit, and open-request attendance
sync via additive `VenueRequestRepository.updateExpectedAttendance`. Wired
Organizer **Capacity** sidebar UI and updated the User Guide.

## Assumptions and design decisions

- **Assignment / team:** Organizer owns draft capacity; pending request attendance
  should match capacity after an Organizer update.
- **Locked defaults (plan):** DRAFT events only; sync open DRAFT/SUBMITTED only;
  decided requests untouched; no Admin code changes.
- **Shared JDBC:** Narrow `UPDATE ... WHERE status IN ('DRAFT','SUBMITTED')` is
  allowed as Organizer write path completion, not an Admin feature change.
- Soft warn if capacity exceeds venue capacity was optional; not implemented as a
  hard rule (UI feedback focuses on sync status).

## Files changed

- `src/main/java/seedu/eventmanager/event/EventCapacityService.java` (new)
- `src/main/java/seedu/eventmanager/event/CapacityUpdateResult.java` (new)
- `src/test/java/seedu/eventmanager/event/EventCapacityServiceTest.java` (new)
- `src/main/java/seedu/eventmanager/event/EventAuditRecord.java` — `UPDATE_CAPACITY`
- `src/main/java/seedu/eventmanager/service/VenueRequestRepository.java` — additive method
- `src/main/java/seedu/eventmanager/storage/JdbcVenueRequestRepository.java` — JDBC update
- `src/main/java/seedu/eventmanager/ui/OrganizerEventView.java` — Capacity screen
- `src/main/java/seedu/eventmanager/ui/EventManagerApplication.java` — wire service
- `src/main/java/seedu/eventmanager/event/README.md`
- `docs/UserGuide.md` — Managing capacity + feature summary row

## Commands actually executed

```sh
./gradlew test --tests 'seedu.eventmanager.event.EventCapacityServiceTest' \
  --tests 'seedu.eventmanager.event.OrganizerVenueRequestServiceTest' \
  --tests 'seedu.eventmanager.event.EventServiceTest' --no-daemon
```

Exit status: 0 (BUILD SUCCESSFUL)

## Actual verification results

- Unit: `EventCapacityServiceTest` covered sync on open SUBMITTED, leave APPROVED
  unchanged, and auth/version failure paths (as implemented in that test class).
- Related event/venue-request unit suites above: passed in the same Gradle run.
- Desktop UI / real DB manual Capacity → Admin observation: **not run** in this
  session (no `./gradlew run` interaction record).

### Manual check notes (for student)

1. `./gradlew run` → Club Organizer → create draft → Request venue → Capacity →
   change capacity → Save → confirm feedback mentions pending sync.
2. Open Venue Administrator Venue requests (observe only) and confirm
   `expectedAttendance` matches new capacity for the pending request.
3. After Admin approve/reject, Capacity update should change event only and report
   decided request unchanged.

## Problems, corrections, and skill revisions

None recorded beyond continuing from a mid-UI session; UG and capacity UI
wiring completed in this continuation. Skill paths followed without revision.

## Outcome and limitations

Capacity management is implemented for Organizer drafts with open-request
attendance sync. Venue Admin approve/reject and booking paths were not modified.
Full JavaFX + Postgres end-to-end check remains for the student.

Suggested commit message:

```
Add Organizer capacity management with pending venue-request attendance sync.
```

## AI-generated mini reflection

The task showed a narrow Organizer write path can keep pending request attendance
aligned without touching Admin decide logic. Strongest outcome: TDD service plus
status-gated JDBC update. Main limitation: desktop/DB manual sync check not run
here. Useful next step: student runs the three manual steps above before merge.

## Follow-up (2026-09-24): fold into edit event; remove Capacity UI

**Prompt (verbatim):** yes please, i dont want a new UI button

**Change:** Removed Organizer **Capacity** sidebar. Pending-request attendance
sync now runs inside `EventService.editEvent` when capacity changes. Deleted
`EventCapacityService` / its dedicated test; coverage moved to
`EventServiceTest`. UG “Managing capacity” section removed; editing section
documents the sync.

**Command:**
```sh
./gradlew test --tests 'seedu.eventmanager.event.EventServiceTest' \
  --tests 'seedu.eventmanager.event.OrganizerVenueRequestServiceTest' \
  --tests 'seedu.eventmanager.event.JdbcEventRepositoryIntegrationTest' --no-daemon
```
Exit status: 0

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:

