---
name: requirements-and-acceptance
description: Turn a feature request or behavior change into source-backed business rules and testable acceptance criteria. Use when planning a feature, clarifying ambiguous requirements, or defining regression expectations before implementation.
---

# Requirements and acceptance criteria

Produce an implementation-ready description of observable behavior, with open
policy decisions exposed rather than silently invented. This skill specifies
behavior; it does not itself authorize implementation.

## Gather evidence

Read the repository instructions, request, relevant specification sections, and
the affected code, tests, and documentation. Trace existing shared contracts and
role ownership before proposing changes. Record sources by path and symbol or
section. Treat existing behavior as evidence, not automatically as correct policy.

Classify each important rule as required by the assignment, agreed by the team,
observed in code, or proposed. If sources conflict, describe the conflict and the
affected decision. Do not invent a deadline, cancellation cutoff, waitlist policy,
check-in window, or role permission just because an example needs one.

## Define the behavior

- Identify the actor, resource, goal, scope, and relevant dependencies.
- Describe permitted state transitions, preconditions, and persistent outcomes.
  Include rejected transitions and whether rejection must leave state unchanged.
- For affected workflows, consider role and ownership checks, duplicate requests,
  boundary values, time boundaries, concurrent changes, and dependency failures.
  Include only cases that matter to this request.
- Distinguish required side effects, such as notifications and audit records,
  from optional improvements. Define failure/transaction expectations where the
  outcome depends on several writes; expose unsettled policy as a decision.
- Write stable criterion IDs, such as `REG-01`, with Given/When/Then conditions
  and an observable result. Map each criterion to an appropriate test level.
  Avoid criteria that merely prescribe a class name or implementation detail.

When a missing decision changes permissions, persisted state, or the user-visible
contract, mark dependent criteria unresolved and ask a focused question if the
session has not already answered it. Continue defining independent behavior.
For low-impact details, state a reasonable assumption and proceed.

## Deliver and check

Return a compact record containing sources, scope, rules/decisions, acceptance
criteria, dependencies, and unresolved questions. Save it only when the task calls
for a repository artifact; otherwise include it in the task response or log.

Before implementation, check that each settled criterion has a testable outcome,
that affected failure paths are covered, and that proposals are not represented
as accepted requirements. Mark readiness as ready, partially ready, or blocked
with the specific unresolved dependency. Keep planned behavior separate from the
current User Guide. Record the actual interaction using the repository convention.
