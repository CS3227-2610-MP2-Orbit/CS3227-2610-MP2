# 009 — Joseph's registration contract readiness

Date: 26 September 2026
Contributor: Johannsen
Branch/revision: attendee, 04dfc57, existing uncommitted catalogue work preserved
Agent: Codex desktop, single agent
Skills: requirements-and-acceptance; code-review-and-verification (source/status check only)

## Original prompt (verbatim)

> can i check if the attendee branch that joseph needs
>
> Use users.username for displayName for now. It's the only human-readable field in the shared users table, and it's unique. If we want real names later, we can agree on a display_name column with Jordan, since that's his schema, and nothing changes on my side.
> Your definition works for me: confirmed + checked-in, excluding cancelled, keyed by users.user_id. A few things from my side:
>
> Please return each attendee at most once per event, even if they cancelled and re-registered.
> Only expose user_id and username through RegisteredAttendee, nothing like password_hash.
> Your call on deactivated accounts (active = false). I'd lean towards excluding them, but please document whatever you choose.
> Ordering doesn't matter; I sort on my side.
>
> Once it's ready, I'll swap NoEventRegistrations for your implementation in EventManagerApplication. That covers Volunteers, View registrations and Announcements

Whitespace normalized from the pasted message; wording retained.

## Findings and decisions

- Request interpreted as checking readiness, not authorizing implementation, push,
  or another merge. No application code modified.
- Local attendee HEAD is 04dfc57. Remote attendee remains b5d4e93; neither contains
  registration storage/implementation. Local uncommitted work is catalogue only.
- Local and latest main registration package contain README only. Joseph's open
  branch contains EventRegistrations/RegisteredAttendee. The current browse PR
  cannot supply real registrants to his Organizer features.
- Fetch discovered origin/main advanced to 4987149: authentication PR #22 merged
  at 2026-09-26T03:34:22Z. New main still has an Attendee placeholder. It was fetched,
  not merged into the active branch. Registration/volunteer PRs #23/#24 remain open.
- Joseph's pasted response settles the username mapping, status inclusion,
  deduplication and minimal DTO shape. Inactive-account handling is delegated but
  not implemented: recommend exclude inactive accounts from this read view while
  retaining records/history. Do not infer automatic cancellation/seat release.
- His explicit offer to do application wiring keeps that change on his side;
  Johannsen supplies the concrete adapter, schema and registration lifecycle.

## Proposed handoff acceptance criteria

| ID | Observable criterion | Evidence needed |
| --- | --- | --- |
| REG-CONTRACT-01 | Confirmed and checked-in registrations appear for the requested event; cancelled registrations do not. | Real repository integration tests |
| REG-CONTRACT-02 | Each attendee appears at most once per event after cancellation/re-registration, including when old history remains. | Lifecycle and query tests |
| REG-CONTRACT-03 | DTO has attendeeId from users.user_id and displayName from users.username; no credential fields. | Projection review and mapping tests |
| REG-CONTRACT-04 | Proposed: inactive users are absent from the returned list, without deleting/cancelling their records as a side effect. | Inactive/reactivation tests; policy confirmation |
| REG-CONTRACT-05 | Event scoping holds; single-attendee lookup matches the same inclusion rules. | Cross-event and default-lookup tests |

Ordering is not a contract requirement. Account role changes, seat accounting
after deactivation, and historical organizer reporting are not settled by this
read-only DTO agreement. Existing Organizer authorization remains in its services.

## Checks actually performed

- Read both skill files; inspected working tree with `git status --short --branch`.
- `git fetch origin`: success; updated remote-tracking main only.
- `git log -1 --oneline HEAD`, `git log -1 --oneline origin/attendee`.
- `rg --files src/main/java/seedu/eventmanager/registration src/main/java/seedu/eventmanager/attendee`.
- `rg -n 'EventRegistrations|RegisteredAttendee|NoEventRegistrations|CHECKED_IN|CONFIRMED|registration' src/main/java/seedu/eventmanager/ui/EventManagerApplication.java src/main/java/seedu/eventmanager/registration src/main/resources/db`.
- `git ls-tree -r --name-only origin/attendee -- src/main/java/seedu/eventmanager/attendee src/main/java/seedu/eventmanager/registration`.
- `git log --oneline HEAD..origin/main` and registration tree inspection on origin/main.
- Read `EventRegistrations.java` and `RegisteredAttendee.java` with git show on
  origin/feature-view-registration; inspected new main authentication/Attendee routing.
- `gh pr list --state open --json number,title,headRefName,url`.
- `gh pr view 22 --json state,mergedAt,url`: confirmed merged.

No relevant registration tests exist locally, so none were run. Prior catalogue
test passes do not establish registration readiness. No external message, commit,
push, PR mutation or new merge was performed. Only this log was added.

## Outcome and AI-generated mini reflection

The branch is not yet the integration Joseph needs. The contract is substantially
clearer, but implementing and testing registration persistence plus its read-only
adapter is still necessary. If unblocking Joseph becomes the priority, that should
be the next separately agreed feature PR, rather than claiming the catalogue PR
provides registration. Skills helped distinguish implemented, agreed and proposed.
No skill correction was required or made in this task.

## Student review

- [ ] I confirmed the prompt and whitespace normalization are accurate.
- [ ] I confirmed the file changes and commands are accurate.
- [ ] I confirmed findings and verification limits are accurate.
- [ ] I added any omitted mistakes or disagreements.

Reviewed by:
Review date:
