# MP2 engineering instructions

## Project context

Build one Java 25 desktop application for campus event and venue management.
Joseph owns Club Organizer, Jordan owns Venue Administrator, and Johannsen owns
Attendee. Each member also contributes shared infrastructure. Keep role-specific
UI separate while sharing authentication, persistence, notifications, audit
logging, and error handling.

Use the user's task and the available MP2 specification as the requirements
source. Distinguish assignment requirements, team decisions, proposals, and
implemented behavior. If a specification is unavailable, identify that limit;
do not infer requirements from a feature suggestion. Do not reuse MP1 application
code. Its logging and verification practices may inform this project's process.

## Repository skills

Read the relevant skill before applying it. These skills support a single agent;
they do not require delegation or additional agents.

| Task | Skill |
| --- | --- |
| Clarify feature behavior and define acceptance criteria | [requirements-and-acceptance](.agents/skills/requirements-and-acceptance/SKILL.md) |
| Implement or fix agreed behavior using a red/green/refactor cycle | [test-driven-implementation](.agents/skills/test-driven-implementation/SKILL.md) |
| Review a change and verify claims against evidence | [code-review-and-verification](.agents/skills/code-review-and-verification/SKILL.md) |

See [Agentic SE](docs/AgenticSE.md) for invocation examples and validation cases.
Use only the skills relevant to the task; a documentation edit does not require a
product implementation cycle.

## Engineering boundaries

- Inspect the current branch, changes, and affected contracts before editing.
  Preserve user and teammate changes. Code present only on another branch is not
  yet available in this checkout; report integration dependencies explicitly.
- Reuse existing shared services. Keep business rules independent of JavaFX and
  persistence implementations behind interfaces. Avoid creating duplicate role,
  identity, notification, or audit abstractions for an individual role.
- Enforce authorization and resource ownership in application/domain workflows,
  not solely in UI visibility. Keep critical rules deterministic.
- Distinguish business audit records from diagnostic logs. Do not log credentials,
  QR secrets, or unnecessary personal data. Use the shared facilities available
  in the checkout; identify missing ones before extending them.
- Match verification to the change. Report actual commands and outcomes, and
  distinguish unit, integration, and real UI/end-to-end evidence. A mocked service
  test is not proof of a working desktop application or database integration.
- Update affected documentation and acknowledgements. Describe only implemented
  features as available. Do not invent personal reflections or student approval.
- Commit, push, open PRs, merge, or publish only within the user's authorized
  scope. The assignment calls for submission on `master`; coordinate any default
  branch change separately from feature development.

## Interaction records

For each meaningful task, create or update a sequential Markdown log under
`logs/<contributor>/`, grouping closely related follow-ups together. Record the
original prompts verbatim, assumptions, decisions, files changed, exact relevant
commands, results, corrections, limitations, and outcome. Include the skill used
and whether it required correction. Do not invent missing history or test results.

Use [the log template](logs/templates/interaction.md). Leave student-review boxes,
reviewer name, and review date unfilled until the student personally reviews the
record. Student-reviewed historical records should not be silently rewritten.
