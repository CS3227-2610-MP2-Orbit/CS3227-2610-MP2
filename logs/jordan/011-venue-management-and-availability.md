# 011 — Venue CRUD, availability, and utilization

Contributor: Jordan  
Agent: Codex  
Related records: `002-venue-workflow-and-database.md`, `004-ui-and-testing.md`

## Prompt summary

Jordan asked the agent to complete Venue Administrator feature work for
creating, editing, activating/deactivating, and viewing venues, with a basic
dashboard and utilization information.

## Interaction and outcome

The agent reused the shared venue repository and service boundaries. The
administrator can create and edit venue details, toggle availability, refresh
the venue list, and view available-venue information from the dashboard.
Availability is intentionally a simple active/inactive control; additional
restriction rules were not added.

Deactivating a venue after approval is allowed. It blocks future approvals and
active-venue checks for publishing and registration, but does not automatically
cancel existing approved bookings.

## Verification

Venue UI refresh behavior and backend venue operations were reviewed against
the existing repository and integration contracts. Manual JavaFX verification
remains necessary for final screen behavior.

