---
name: desktop-ui-polish
description: Polish JavaFX role screens for layout, shared visual shell, and primary actions without inventing product policy. Use when fixing squished layouts, dual chrome, truncated labels, empty states, or aligning Club Organizer / Venue Administrator / Attendee screens to the shared desktop look.
---

# Desktop UI polish

Improve an existing JavaFX role screen so it is usable at normal window sizes and
visually consistent with the shared Event Venue Manager shell. This skill
polishes presentation; it does not invent business rules, club directories, or
cross-role workflows.

Work as a **single agent**. Do not delegate to subagents for this skill.

## Gather evidence

Read `AGENTS.md`, the target UI class(es) under `src/main/java/seedu/eventmanager/ui/`,
and a comparable shell such as `VenueAdministratorDashboardView` when aligning
look-and-feel. Inspect how the screen is hosted (for example `EventManagerApplication`
workspace chrome versus an inner role sidebar).

Classify each change as:

- layout / spacing / width fill
- shared visual tokens (sidebar, page background, cards, primary actions)
- control behavior already implied by the feature (clear form, revert, save)
- proposal only (new product capability such as club CRUD)

Do not implement proposals silently.

## Layout rules

1. Prefer **one role chrome**: either an outer app header **or** an inner dark
   sidebar for navigation—not both competing for width unless the outer bar is
   intentionally minimal and documented.
2. Content must **fill available width** when the stage grows. Avoid fixed narrow
   hubs that leave large empty side margins while truncating labels.
3. Use GridPane (or equivalent) **column constraints**: readable label column,
   growing field column. Labels must wrap or use full wording—not `Descrip…`.
4. Lists and forms need sensible `min` / `pref` / `max` widths and
   `Priority.ALWAYS` on the primary growing region.
5. Empty states and primary actions (New, Save, Reset/Revert, Home) must be
   obvious after the polish.

## Shared visual tokens

Match the Venue Administrator shell unless the task says otherwise:

- Sidebar background `#172033`, nav text `#dce4f2`
- Page background `#f7f9fc`
- Cards: white, light border `#e2e8f0`, modest radius
- Primary action: distinct filled button (for example blue `#2563eb` on white)

Do not invent a new brand palette per role.

## Behavior bounds

- Keep business validation in services; UI only presents and invokes it.
- Do not add club create/edit, venue approval from Organizer, or auth redesign
  under the guise of UI polish. If the user asks, mark it unresolved/proposal.
- Prefer existing callbacks (Home, New event, Revert) over parallel duplicate
  controls that fight each other.
- When fixing “button does nothing,” make the create/edit mode transition
  observable (heading, feedback, defaults, selection clear).

## Output checklist

Before finishing, report a short before/after checklist covering at least:

1. Dual chrome / width fill
2. Label truncation
3. Primary actions (including New / Reset or equivalent)
4. Shared shell tokens
5. Explicit non-goals (what you did **not** invent)

If the task is review-only, produce the checklist as findings without editing
`src/`. If the task authorizes implementation, keep the diff minimal and run
`./gradlew classes` (and relevant tests when they exist). Note that this repo
has no JavaFX interaction-test framework—manual `./gradlew run` checks remain a
verification limit unless automation is added.

## Example invocation

```text
Use $desktop-ui-polish on Club Organizer events.
Remove squished dual chrome, fix New event create-mode, and keep Venue shell tokens.
Do not add club CRUD.
```
