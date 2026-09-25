# 009 — Organizer reset fix and Venue-like UI

Date: 2026-09-23
Contributor: Joseph
Branch and starting revision: `create-edit-events` (dirty tree after merge-venue UI)
Agent/tool: Cursor (tutor-approved)
Skills used: `.agents/skills/test-driven-implementation/SKILL.md` (behavior fix;
JavaFX visual change has no automated UI test framework in this checkout)

## Objective

Fix Club Organizer Reset while editing a saved event, clarify that clubs are
configured via `.env` (no add-club UI), and restyle the Organizer events screen
to follow the Venue Administrator dark-sidebar / card layout.

## Original prompts (verbatim)

> I dont think the reset button works? It only works for a new event, but if i
> want to edit an old event after clicking it, the reset does not work.
>
> Also how do we add new clubs? Is there a UI button for that as well? Also i
> feel the UI is quite different to jordan's UI, can you make it similar as well?

## Response summary

Updated `OrganizerEventView` so Reset clears a new form and, when editing,
**Revert changes** reloads the last saved draft from the database with visible
feedback. Restyled the screen with the Venue shell colors (sidebar `#172033`,
page background `#f7f9fc`, white cards). Documented that clubs come from
`EVENT_MANAGER_CLUB_IDS` — no club-create UI was added (would invent a product
feature outside the create/edit-events scope).

## Assumptions and design decisions

- Reset while editing means discard unsaved field edits, not delete the event.
- Club CRUD UI is unresolved/out of scope for this feature; `.env` remains the
  development identity source until shared club management exists.
- Visual parity is shell-level (sidebar/cards/colors), not a copy of every Venue
  screen.

## Files changed

- `src/main/java/seedu/eventmanager/ui/OrganizerEventView.java`
- `docs/UserGuide.md`
- `logs/joseph/009-organizer-reset-and-ui.md`

## Commands actually executed

```sh
./gradlew test --no-daemon --tests 'seedu.eventmanager.event.*' --tests 'seedu.eventmanager.ui.EventFormParser*'
./gradlew classes --no-daemon
```

## Actual verification results

- `./gradlew --no-daemon classes test --tests 'seedu.eventmanager.event.*' --tests 'seedu.eventmanager.ui.*'` exited `0` with `BUILD SUCCESSFUL`.
- No JavaFX UI automation ran; Reset/revert and visual shell need a manual
  `./gradlew run` check.

## Problems, corrections, and skill revisions

No automated JavaFX interaction test exists for Reset; verification is compile
plus manual desktop check after `./gradlew run`.

## Outcome and limitations

Club add UI was intentionally not implemented. Shared authentication / real club
directory remain team follow-ups.

Suggested commit message: `fix(organizer): revert edits and align events UI shell`

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
