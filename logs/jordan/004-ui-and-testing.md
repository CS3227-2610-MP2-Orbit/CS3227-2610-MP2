# 004 — JavaFX UI and automated testing

Date: September 2026  
Contributor: Jordan  
Branch: `feautre-request-venue`  
Agent/tool: Codex  
Skills used:
- `.agents/skills/desktop-ui-polish/SKILL.md` — guided the Venue Administrator dashboard layout, role-focused visibility, and UI states.
- `.agents/skills/test-driven-implementation/SKILL.md` — guided unit, integration, and end-to-end test planning and verification.

## Objective

Add a focused Venue Administrator interface and test the observable workflow
across unit, integration-style, and in-process E2E boundaries.

## UI implementation

- Added login and role checks for Venue Administrators.
- Added dashboard state handling for loading, ready, empty, and error states.
- Added pending request review with approve/reject actions.
- Added venue management, availability, and user-access screens where the
  branch architecture supported them.
- Preserved backend authority for validation and authorization.

## Tests added

- Validator tests for invalid intervals and attendance.
- Service tests for approval, rejection, conflicts, invalid states,
  authorization, notifications, audit records, and transaction failures.
- Integration-style tests using real application services with in-memory
  adapters.
- In-process E2E tests covering dashboard load, approval, rejection, and
  unauthorized roles.

## Debugging lessons

- Failed decisions must preserve the existing request table instead of
  replacing it with an empty error-state data model.
- Dashboard navigation must reload data instead of recreating cards with
  default zero values.
- Display joins must not hide valid workflow records.

## Verification limitations

The repository did not initially contain a browser or JavaFX E2E harness.
Tests therefore use deterministic in-process adapters until full PostgreSQL
and UI automation infrastructure is available.
