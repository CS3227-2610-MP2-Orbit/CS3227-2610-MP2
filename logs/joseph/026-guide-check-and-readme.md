# 026 — Check the guides and point the README at them

Date: 2026-09-28
Contributor: Joseph
Branch and starting revision: `add-documentation-reflection` (working tree)
Agent/tool: Cursor
Skills used: none. Documentation check against the current checkout.

## Objective

Check `docs/UserGuide.md` and `docs/DeveloperGuide.md` against the current screens and services, correct mismatches, and make `README.md` point to those guides.

## Original prompts (verbatim)

> can you scan throuhg the UG and DG to make sure that everything is correct. Then can u update the readme.md file and specify it to follow the UG and DG

## Response summary

Corrected the first administrator account, the dashboard wording, a stale-save description, the administrator password length, an announcement with no registrants, the startup error claim, and the register/check-in use case. Replaced the README so it sends readers to the User Guide for use and the Developer Guide for design.

## Assumptions and design decisions

- The seeded `admin` / `admin123` account in `V5__development_admin_seed.sql` is local-only and should be mentioned, because a fresh database otherwise has no administrator.
- Use-case numbering keeps UC04b beside UC04 rather than renumbering later cases.

## Files changed

- `docs/UserGuide.md`
- `docs/DeveloperGuide.md`
- `README.md`
- `logs/joseph/026-guide-check-and-readme.md`

## Commands actually executed

No application command was run. The check was a read of the guides against `JdbcLocalSessionService`, `JdbcUserAccessRepository`, `OrganizerEventView`, `VenueAdministratorDashboardView`, `JdbcVenueRequestRepository`, `CheckInAvailabilityText`, `InboxService`, and `V5__development_admin_seed.sql`.

## Actual verification results

Not run: `./gradlew test` and `./gradlew run`. Button labels and rules were taken from the Java sources above.

## Problems, corrections, and skill revisions

The user guide said a Venue Administrator could only be created by an existing administrator. A fresh database already inserts `admin`. The developer-guide check-in use case previously continued after cancellation, which the service rejects.

## Outcome and limitations

The README now defers product description to the two guides. The guides were not clicked through in the running app.
