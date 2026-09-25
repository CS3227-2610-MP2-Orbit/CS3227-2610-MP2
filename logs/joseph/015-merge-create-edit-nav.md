# 015 — Merge Organizer create/edit navigation

Date: 2026-09-25
Contributor: joseph
Branch and starting revision: `feautre-request-venue`
Agent/tool: Cursor Agent
Skills used (paths and revision or change description):
- `.agents/skills/desktop-ui-polish/SKILL.md` — Organizer events screen layout

## Objective

Remove the duplicate **New event** sidebar entry. Create and edit already share
one screen (list + form), so the sidebar keeps a single **Events** entry and a
**+ New event** button sits above the events list.

## Original prompts (verbatim)

> right now in the edit event tab, users can also create and save new event. in fact the. edit and create tab i feel are quite repetitive as it has the exact same UI. Do you think its better to get rid of one?

> yes please, update the UI

## Response summary

- Removed the **New event** sidebar nav button.
- Added a primary **+ New event** button in the "Your events" card header; it
  clears selection and switches the form to create mode.
- Edit-mode heading now reads "Edit draft event — <title>" so the mode is clear.
- Nav highlighting simplified: **Events** is active on the events screen,
  **Request venue** on its screen.
- Empty-list feedback no longer refers to the removed nav item.
- User Guide and Developer Guide steps updated.

## Assumptions and design decisions

- Creating and editing drafts both stay available; only the duplicate nav entry
  was removed (UI change, no business-rule change).

## Files changed

- `src/main/java/seedu/eventmanager/ui/OrganizerEventView.java`
- `docs/UserGuide.md`
- `docs/DeveloperGuide.md`

## Commands actually executed

```sh
./gradlew classes test --tests 'seedu.eventmanager.event.EventServiceTest' \
  --tests 'seedu.eventmanager.ui.*' --no-daemon
```

Exit status: 0 (BUILD SUCCESSFUL)

## Actual verification results

- Compilation and the listed unit tests passed.
- The JavaFX screen was not exercised by automated tests (the repo has no
  JavaFX interaction-test framework). Manual `./gradlew run` check: not run by
  the agent.

## Problems, corrections, and skill revisions

None.

## Outcome and limitations

Organizer sidebar now has **Events** and **Request venue** only; create is
reached via **+ New event** on the events list. Manual desktop check remains
for the student.

Suggested commit message:

```
Merge Organizer create/edit navigation into a single Events screen.
```

## AI-generated mini reflection

The change removes a nav entry that duplicated an existing screen mode rather
than a real feature. Main limitation: UI behaviour is verified only by
compilation, not by an automated UI test.

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
