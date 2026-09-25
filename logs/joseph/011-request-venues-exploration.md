# 011 — Explore Organizer "Request venues" planning surface

Date: 2026-09-23
Contributor: Joseph
Branch and starting revision: `create-hooks` at `750ff53`
Agent/tool: Cursor (subagent explore under parent planning task)
Skills used: `.agents/skills/requirements-and-acceptance/SKILL.md` (read;
exploration/classification only — no implementation authorized)

## Objective

Search the MP2 checkout for venue-request contracts, Organizer UI, docs,
shared services, Flyway schema, status enums, and notification/audit behavior
to support planning Organizer "Request venues" (submit a venue booking for an
event). Distinguish observed, required, and proposed; surface Jordan's
expected request contract and blockers.

## Original prompts (verbatim)

> Explore the MP2 repo at /Users/josephkwok/Desktop/Website/cs3227/MP2 for
> planning Organizer "Request venues" (submit venue booking for an event).
>
> Search thoroughly for:
> - Venue booking, venue request, VenueRequest, Booking, reservation, approve,
>   reject
> - OrganizerEventView and organizer packages
> - UserGuide / docs mentioning venue pipeline or organizer cannot request
> - Shared services/interfaces between Organizer and Venue Admin
> - Flyway migrations related to bookings/requests
> - Status enums for venue requests
> - Notification/audit on venue decisions
>
> Return a structured report with file paths, symbols, observed vs required vs
> proposed, and what Jordan's side already expects as the request contract
> (event id, venue id, start/end, attendance, status). Note conflicts and
> blockers.

## Response summary

Produced a source-backed planning report (parent task deliverable). Current
checkout has Jordan's Venue Administrator approve/reject pipeline and Joseph's
draft event editor, but **no** Organizer submit-venue-request path. An older
local branch `request-venues` contains Joseph's pre-merge prototype that
conflicts with Jordan's domain types. No MP2 assignment specification file is
present in the repository.

## Assumptions and design decisions

- **Required (assignment):** unavailable — no MP2 spec file found under this
  repo or adjacent `cs3227` tree during search.
- **Team/docs:** Venue Administrator owns requests, availability, conflicts,
  approvals; Organizer owns events; docs explicitly say organizer-to-venue
  request integration is not implemented.
- **Observed (Jordan):** `VenueRequest` UUID fields + `SUBMITTED` →
  `APPROVED`/`REJECTED`; booking created on approve; audit + notification
  outbox on decisions.
- **Proposed (Joseph, unmerged `request-venues`):** `VenueGateway` /
  `VenueRequestService` with `PENDING` status, String venue/organizer ids,
  conflict-on-submit, supersede on schedule/capacity change — not in current
  checkout after venue merge.
- Classification of Joseph's earlier criteria (`VEN-REQ-*`) remains
  **proposed/team** until revalidated against Jordan's durable schema.

## Files changed

- `logs/joseph/011-request-venues-exploration.md` (this record)

## Commands actually executed

Working directory: `/Users/josephkwok/Desktop/Website/cs3227/MP2`

- Repository greps/globs for venue request, booking, OrganizerEventView,
  migrations, notifications, and docs.
- Reads of Jordan domain/service/storage, Organizer event model/UI, User/Developer
  guides, Flyway `V1`–`V4`, and related logs.
- `git branch -a`, `git log`, `git show request-venues:...` to inspect the
  unmerged Organizer prototype and its contract diffs.
- `git status --short --branch` / `git rev-parse --short HEAD` to confirm
  checkout `create-hooks` @ `750ff53` lacks `VenueRequestService` and log
  `007`.

No product tests were run (exploration only).

## Actual verification results

Not applicable — no implementation or regression suite executed for this task.

## Problems, corrections, and skill revisions

None. Skill used for classification discipline only.

## Outcome and limitations

Exploration complete for planning. Blockers: identity UUID vs String mismatch,
conflicting `VenueRequest`/`VenueRequestStatus` models across branches, no
organizer submit API on Jordan's side, no FK/link from `venue_requests.event_id`
to `organizer_event`, and absent assignment PDF/spec in-repo.

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
