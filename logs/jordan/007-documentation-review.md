# 007 — Documentation review and CI investigation

Date: September 2026  
Contributor: Jordan  
Branch: `branch-bugs-fixing`  
Agent/tool: Codex

## Original prompt

> can you check if the user guide and developer guide make sense

## Objective

Review `docs/UserGuide.md` and `docs/DeveloperGuide.md` against the current
Venue Administrator implementation and recent workflow changes, without
modifying the guides during the review.

## Review performed

Checked the guides for:

- PostgreSQL and `.env` setup;
- shared login, logout, and role switching;
- venue creation and active/inactive availability;
- venue request approval and rejection;
- capacity validation during approval and event editing;
- deactivated-account behavior;
- CI/CD, monitoring, testing, and known limitations.

## Findings

The guides are broadly consistent with the current implementation. They already
document that:

- an approval fails when expected attendance exceeds venue capacity;
- an inactive venue cannot be used for future active-venue checks;
- existing approved bookings are not automatically cancelled by deactivation;
- deactivated accounts lose access on their next checked action;
- logout returns the user to shared login and permits role switching.

Minor wording issues were identified:

1. The User Guide says `Home` or `Log out` is available for every role, although
   the Venue Administrator workspace currently keeps `Log out` in its sidebar
   and does not expose a duplicate Home button.
2. The User Guide refers to the administrator deactivation behavior as a known
   issue; the current behavior intentionally blocks approval/rejection while
   some read-only/admin screens may remain visible until another check or logout.
3. Manual testing instructions could explicitly cover capacity rejection,
   deactivation after request submission, deactivated administrators, and
   switching roles after logout.

## Evidence

Relevant files inspected:

- `docs/UserGuide.md`
- `docs/DeveloperGuide.md`
- `src/main/java/seedu/eventmanager/ui/EventManagerApplication.java`
- `src/main/java/seedu/eventmanager/ui/VenueAdministratorFxApplication.java`
- `src/main/java/seedu/eventmanager/service/VenueAdministratorService.java`
- `src/main/java/seedu/eventmanager/event/EventService.java`

Relevant checks:

```text
rg -n -i "venue|administrator|deactivat|capacity|logout|session|active|approval|reject|availability|publish|register" docs/UserGuide.md docs/DeveloperGuide.md
Test-Path .env.example  -> True
Test-Path src/main/resources/db/migration/V5__development_admin_seed.sql -> True
```

## Outcome and limitations

No documentation changes were made in this review. The guides are usable for
the current feature set, with the minor wording and manual-test improvements
listed above still available as follow-up work.

The review did not establish that every JavaFX interaction is automatically
validated; the Developer Guide correctly identifies manual JavaFX verification
as remaining work.
