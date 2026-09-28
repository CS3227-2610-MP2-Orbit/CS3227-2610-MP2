# 012 — Capacity and venue-state validation

Contributor: Jordan  
Agent: Codex  
Related records: `003-backend-and-rbac.md`, `007-documentation-review.md`

## Prompt summary

Jordan identified bugs where approval ignored expected attendance, organizers
could increase event capacity above room capacity, and venues deactivated after
submission could still be approved. Jordan authorized fixes for these
Venue Administrator-related bugs while leaving the publish/release race to
Joseph.

## Interaction and outcome

The agent added approval-time checks for the current venue status and capacity.
An inactive venue or an attendance value above capacity is rejected before the
request is persisted or side effects are created.

Event editing now rejects a capacity increase above the capacity of the venue
associated with the latest approved request. The event and request remain
unchanged when the validation fails.

## Verification

Regression tests cover capacity-exceeded and inactive-venue approval failures,
including preservation of the submitted request and absence of booking,
notification, and audit side effects.

