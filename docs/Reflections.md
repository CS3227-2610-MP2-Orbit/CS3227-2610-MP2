# Reflections

This document records reflections on basic Agentic SE, including interesting
skills, prompts, outcomes, and lessons learned.

## Account-management scope

The current Venue Administrator account-management flow supports editing a
normal user's username, role, and active status. It intentionally does not
provide a change-password or password-reset function yet. That capability is a
future enhancement requiring a dedicated secure and audited workflow.
## Skills used for Venue Administrator work

The project skills are stored under `.agents/skills/` and are shared through
the repository. Jordan's detailed evidence is recorded in `logs/jordan/`.

| Skill | Relevance to Jordan's work |
| --- | --- |
| [requirements-and-acceptance](../.agents/skills/requirements-and-acceptance/SKILL.md) | Defined venue-request states, validation rules, and observable acceptance criteria. |
| [test-driven-implementation](../.agents/skills/test-driven-implementation/SKILL.md) | Structured unit, integration, end-to-end, and regression testing. |
| [code-review-and-verification](../.agents/skills/code-review-and-verification/SKILL.md) | Reviewed RBAC, transactions, error paths, observability, and verification evidence. |
| [desktop-ui-polish](../.agents/skills/desktop-ui-polish/SKILL.md) | Guided the Venue Administrator dashboard layout and user-facing states. |
| [security-and-rbac](../.agents/skills/security-and-rbac/SKILL.md) | Captures authentication, role, direct-API, and object-level authorization checks. |
| [database-migration-and-integrity](../.agents/skills/database-migration-and-integrity/SKILL.md) | Captures PostgreSQL schema, constraints, conflict prevention, and rollback concerns. |
| [observability-and-error-handling](../.agents/skills/observability-and-error-handling/SKILL.md) | Captures structured logging, safe errors, notifications, audit events, and metrics. |

The final three skills were added retrospectively after the related Venue
Administrator implementation. They describe and organize lessons from work
already completed; future changes should invoke them before implementation.

## Agent skill: Venue Workflow Review Agent

The Venue Workflow Review Agent is used to review venue-related pull requests
against the project's existing architecture and business rules. It is intended
to produce review material, not to make code changes automatically.

### Task

Review changes related to venues, bookings, and approval workflows. The review
checks:

- whether state transitions are valid;
- whether authorization checks are present;
- whether audit logging is complete;
- whether important side effects, such as notifications, are handled;
- whether new business rules have adequate tests;
- whether database updates could race or become inconsistent;
- whether business logic is duplicated; and
- whether the changes violate the existing architecture.

### Input

- Git diff for the proposed change;
- surrounding source code;
- existing tests;
- project documentation and architecture; and
- the current venue, booking, authorization, audit, and notification
  boundaries.

### Output

The agent reports findings under the following headings:

- **Critical**
- **High**
- **Medium**
- **Low**

For each finding, it provides evidence pointing to the relevant file or
function, the smallest appropriate recommended fix, a test recommendation, and
the agent's confidence together with any missing information. The agent must
not invent requirements that are absent from the project specification or
existing code.

### Human responsibility

The human developer remains responsible for validating the findings against the
intended requirements, implementing or approving fixes, and deciding whether a
pull request is ready to merge. The agent's output is review evidence and
guidance, not an automatic approval or replacement for engineering judgment.

### Reflection prompts

This skill provides evidence for reflecting on Agentic SE:

- What tasks were handled effectively by the agent?
- Where did the agent require additional guidance or correction?
- Which findings were confirmed, rejected, or refined by the developer?
- Did the agent identify tests or workflow risks that would otherwise have been
  missed?
