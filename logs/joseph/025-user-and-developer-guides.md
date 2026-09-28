# 025 — User guide and developer guide for all three roles

Date: 2026-09-28
Contributor: Joseph
Branch and starting revision: working tree at `96177f4`
Agent/tool: Cursor
Skills used: none as a formal skill invocation. Documentation-only edit. Product behaviour was taken from the current services and screens, not from a new feature proposal.

## Objective

Rewrite the user guide and developer guide so they follow the earlier AB3-style guide shape and describe Club Organizer, Venue Administrator, and Attendee as implemented.

## Original prompts (verbatim)

> can u now update the documentation and user guide by following my old project's guidelines. Additionalyl, throughuly read through the code first and understand all features, as i think you only have knowledge of the club oragnization so far and how it works. but i need to update the docs to contain all 3 roles
>
> https://github.com/josephkwok001/tp/blob/master/docs/DeveloperGuide.md
> https://github.com/josephkwok001/tp/blob/master/docs/UserGuide.md
>
> Developer guide has all the use case, user story, NFR, architecture daigrams, etc.
>
> User guide focus on the user who do not have technical background. talk about the features, how to use them.

## Response summary

Read the old EstateSearch / AB3 guides and the current Event Venue Manager services and role screens. Replaced `docs/UserGuide.md` with plain-language steps for all three roles. Replaced `docs/DeveloperGuide.md` with an architecture diagram, component notes, user stories, five use cases, non-functional requirements, and manual test steps.

## Assumptions and design decisions

- The old guides set the section shape: quick start and feature steps for users; architecture, stories, use cases, and non-functional requirements for developers.
- Only behaviour found in the current checkout is described as available. Password change, club rename/delete, email, QR check-in, and withdrawing a still-waiting request are listed as absent.
- Venue administrators share one access level. The older “claim access” instruction was not kept, because `JdbcAuthorizationService` only checks the venue-administrator role.
- Self-signup offers Attendee and Club Organizer. Venue Administrator accounts are created under Users and access.

## Files changed

- `docs/UserGuide.md`
- `docs/DeveloperGuide.md`
- `logs/joseph/025-user-and-developer-guides.md`

## Commands actually executed

From `/Users/josephkwok/Desktop/Website/cs3227/MP2`:

- `git rev-parse --short HEAD && git status --short -- docs/UserGuide.md docs/DeveloperGuide.md` — exit 0. Revision `96177f4`. Both guides modified.

No Gradle test and no `./gradlew run` were run. This change does not alter application code.

## Actual verification results

- Guides were checked against `EventService`, `OrganizerVenueRequestService`, `VenueAdministratorService`, `ClubService`, `AnnouncementService`, `VolunteerService`, `RegistrationService`, `JdbcAuthorizationService`, and the role views’ button labels.
- Not run: the desktop app, so screen wording was taken from the JavaFX button text rather than a live click-through.

## Problems, corrections, and skill revisions

The previous developer guide’s manual test still said approval was forbidden until an administrator claimed a venue. That does not match the current authorization code, so the new guide does not say that.

## Outcome and limitations

Both guides now cover the three roles. The user guide avoids service and schema names. The developer guide’s diagrams are Mermaid, not exported UML images. JavaFX was not launched to confirm every sentence against the running window.
