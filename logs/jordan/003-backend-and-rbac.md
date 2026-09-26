# 003 — Backend workflow and authorization

Date: September 2026  
Contributor: Jordan  
Branch: `branch-venue-admin`  
Agent/tool: Codex  
Skills used:
- `.agents/skills/test-driven-implementation/SKILL.md` — guided backend behavior, failure-path, and workflow test coverage.
- `.agents/skills/code-review-and-verification/SKILL.md` — reviewed backend authorization, state transitions, and transaction boundaries.
- `.agents/skills/security-and-rbac/SKILL.md` — added retrospectively to capture the RBAC and direct-API authorization work.

## Objective

Implement the Venue Administrator backend while preserving SRP and keeping
business logic separate from presentation code.

## Implementation summary

- Added request and booking state types.
- Added request validation for identifiers, time ranges, and attendance.
- Added `VenueAdministratorService` for approval and rejection orchestration.
- Added repository, transaction, notification, audit, and authorization
  boundaries.
- Added PostgreSQL migration constraints and booking conflict protection.

## Authorization review

The backend requires an authenticated actor and the
`VENUE_ADMINISTRATOR` role. Organizer and attendee actions are rejected.
Resource-level authorization is required through the shared authorization
boundary so venue scope is not enforced only by the frontend.

## Verification

Main Java compilation passed during development. Gradle test execution was
temporarily blocked by dependency/network and JavaFX plugin resolution issues.
