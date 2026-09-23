# 012 — Request venues (Organizer submit)

Date: 2026-09-23
Contributor: Joseph
Branch and starting revision: `feautre-request-venue`
Agent/tool: Cursor (tutor-approved); single-agent
Skills used:
- `.agents/skills/requirements-and-acceptance/SKILL.md`
- `.agents/skills/test-driven-implementation/SKILL.md`
- `.agents/skills/desktop-ui-polish/SKILL.md`

## Objective

Implement Organizer submit of a venue booking request for an owned event so
Venue Admin’s existing `SUBMITTED` queue can approve/reject it. Skip Clubs UI.
Adapt to Jordan’s `VenueRequest` contract; transitional organizer UUID mapping.

## Original prompts (verbatim)

> Request venues (Organizer submit)
>
> Implement the plan as specified, it is attached for your reference. Do NOT
> edit the plan file itself.
>
> To-do's from the plan have already been created. Do not create them again.
> Mark them as in_progress as you work, starting with the first one. Don't stop
> until you have completed all the to-dos.

(Prior planning chat: implement first / merge with Jordan later; skip Clubs UI;
do not log that planning message.)

## Response summary

Implemented `OrganizerVenueRequestService` + `OrganizerIds`, extended
`VenueRequestRepository.findOpenByEventId`, wired JDBC venue/request repos into
the Organizer app path, added **Request venue** Organizer UI (shared shell),
and updated UG/DG. Unit tests for VR-01–VR-04 and OrganizerIds pass. Dual-role
UI smoke (VR-05/VR-06 manual) was not run in this session.

## Assumptions and design decisions

Locked defaults from approved plan:

| Decision | Choice |
| --- | --- |
| Request shape | Jordan `VenueRequest` + `SUBMITTED` |
| Conflicts | No hard check on submit (Admin approve) |
| Attendance | `event.capacity()` |
| Times | Event `Instant` → `OffsetDateTime` UTC |
| Organizer UUID | Parse if UUID-shaped; else nameUUID `organizer:` + userId |
| Duplicate | Reject second open `SUBMITTED`/`DRAFT` for same event |
| Event eligibility | Owned event; draft OK |
| Non-goals | Clubs UI, supersede, Admin changes |

### Acceptance criteria

- **VR-01** Owned event + ACTIVE venue → persist `SUBMITTED` request with
  matching ids, UTC window, attendance = capacity. (unit tested)
- **VR-02** Non-owned event → access denied; no row. (unit tested)
- **VR-03** Unknown/inactive venue or missing event → error; no row. (unit tested)
- **VR-04** Existing open `SUBMITTED` → duplicate rejected; prior unchanged. (unit tested)
- **VR-05** Successful submit appears in Admin `findSubmitted` — **manual not run**
- **VR-06** Organizer UI Request venue screen — implemented; **manual UI not run**

## Files changed

- `src/main/java/seedu/eventmanager/event/OrganizerIds.java`
- `src/main/java/seedu/eventmanager/event/OrganizerVenueRequestService.java`
- `src/main/java/seedu/eventmanager/service/VenueRequestRepository.java`
- `src/main/java/seedu/eventmanager/storage/JdbcVenueRequestRepository.java`
- `src/main/java/seedu/eventmanager/ui/EventManagerApplication.java`
- `src/main/java/seedu/eventmanager/ui/OrganizerEventView.java`
- `src/test/java/seedu/eventmanager/event/OrganizerIdsTest.java`
- `src/test/java/seedu/eventmanager/event/OrganizerVenueRequestServiceTest.java`
- `docs/UserGuide.md`, `docs/DeveloperGuide.md`
- `logs/joseph/012-request-venues-implement.md`, `logs/prompts_summary.md`
- `src/main/java/seedu/eventmanager/event/README.md`

## Commands actually executed

Working directory: `/Users/josephkwok/Desktop/Website/cs3227/MP2`

- `./gradlew test --tests 'seedu.eventmanager.event.OrganizerVenueRequestServiceTest' --tests 'seedu.eventmanager.event.OrganizerIdsTest' --no-daemon` → BUILD SUCCESSFUL
- `./gradlew test --tests 'seedu.eventmanager.event.*' --tests 'seedu.eventmanager.venue.VenueRequestValidatorTest' --no-daemon` → BUILD SUCCESSFUL
- `./gradlew classes --no-daemon` → BUILD SUCCESSFUL
- `./gradlew run` dual-role smoke → **not run** (no interactive session)

## Actual verification results

- OrganizerIds + OrganizerVenueRequestService unit tests: pass (VR-01–VR-04)
- Broader `event.*` + VenueRequestValidatorTest: pass
- Compile `classes`: pass
- Manual Admin pending-list after Organizer submit: not run
- JavaFX layout polish: implemented per shared tokens; visual check not run

## Problems, corrections, and skill revisions

- Invalid UUID fixtures with non-hex letters in early test draft; corrected to
  hex-only UUIDs before green run.
- Organizer app path now also runs Flyway (`DatabaseBootstrap.migrate`) so
  `venue_requests` exists when request UI is used without opening Admin first.
- No skill file edits.

## Outcome and limitations

Outcome: Organizer can submit `SUBMITTED` venue requests into Jordan’s table
from the desktop UI. Limitation: transitional `OrganizerIds` mapping; no
supersede/withdraw; conflict still only on Admin approve; VR-05/VR-06 manual
evidence not captured this session.

Suggested commit message:
`feat(organizer): submit venue requests into admin pipeline`

## AI-generated mini reflection

AI-generated reflection: Implementing against Jordan’s existing contract first
unblocked the venue pipeline without inventing a second request model. Strongest
outcome is unit-tested ownership/duplicate/ACTIVE-venue rules plus a Request
venue screen in the shared Organizer shell. Main limitation is unverified
end-to-end UI against a live Postgres Admin queue. Useful next step: manual
dual-role smoke, then align organizer UUID with Jordan’s users table.

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
