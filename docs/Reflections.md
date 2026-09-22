# Reflections

This document records reflections on basic Agentic SE, including interesting
skills, prompts, outcomes, and lessons learned.

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
