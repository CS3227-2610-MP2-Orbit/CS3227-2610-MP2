# Agentic SE workflow

The team uses one engineering agent with three reusable SWE skills. They apply to
Organizer, Venue Administrator, and Attendee work; the examples below are not
additional product requirements. Project-wide rules live in [`AGENTS.md`](../AGENTS.md).

| Skill | Use it for | Expected output |
| --- | --- | --- |
| [requirements-and-acceptance](../.agents/skills/requirements-and-acceptance/SKILL.md) | Clarifying a feature before implementation | Sourced rules, testable criteria, assumptions, and unresolved decisions |
| [test-driven-implementation](../.agents/skills/test-driven-implementation/SKILL.md) | Implementing settled behavior or fixing a bug | An observed failing test, minimal fix, passing checks, and verification limits |
| [code-review-and-verification](../.agents/skills/code-review-and-verification/SKILL.md) | Reviewing a diff or a targeted implementation | Evidence-backed findings, checks run, and remaining uncertainty |

## Using the skills

The skills are checked into `.agents/skills/` so they travel with the repository.
Open the agent task in the repository and use the skill by name where supported.
If a running session has not discovered newly added skills, explicitly ask it to
read the linked `SKILL.md` and follow it. Do not assume a file was loaded merely
because it exists. Each skill is self-contained and can also be read explicitly
by another coding agent.

Example requests, to be adapted to the current task:

```text
Use $requirements-and-acceptance to define acceptance criteria for attendee
cancellation. Inspect our existing contracts and identify unresolved policies.
Do not implement the feature yet.
```

```text
Use $test-driven-implementation to implement the agreed duplicate-registration
criterion. Show the expected failing assertion before the implementation change,
then run the focused and relevant broader tests.
```

```text
Use $code-review-and-verification to review the current branch against origin/main.
Check the accepted criteria, permissions, shared contracts, and verification
evidence. Report findings without changing the implementation.
```

Use the actual comparison branch for a review. `main` is the baseline when these
skills were added; the assignment's required submission branch is `master`.
Changing the default branch remains a team coordination task.

## Validating the skills

Structural validation checks skill metadata, naming, and unfinished scaffolding.
It does not prove that an agent follows the workflow correctly. Initial structural
checks used Codex's bundled `skill-creator/scripts/quick_validate.py`; that local
authoring tool is not a repository build dependency.

Behavioral evaluation should give the agent the prompt and raw fixture first, then
assess its output against the rubric. Use an isolated scratch directory for
intentionally faulty code. Do not seed defects in the application. Record the
skill revision, prompt, outputs, commands, failures, and any skill correction in
the contributor's interaction log. Keep expected results separate from the prompt
when doing a future independent evaluation.

| Case | Input task | Evaluation rubric |
| --- | --- | --- |
| R1: ambiguous policy | Specify cancellation when the only request is “Attendees can cancel before the event.” | Exposes cutoff/time-zone, ownership, eligibility, and side-effect decisions; identifies what is known; does not silently choose a policy; gives conditional, observable criteria |
| T1: duplicate registration | Fix a scratch registration book whose duplicate call returns success | Runs a compiling behavioral test that fails for the duplicate outcome, implements the rule, and reruns meaningful assertions; does not call a compile error the red result |
| C1: ownership defect | Review cancellation that removes a caller-supplied registration ID without checking its owner | Identifies the specific non-owner path and state loss, provides a reproduction, and distinguishes the finding from untested production behavior |

The initial [validation record](skill-validation/2026-09-22.md) contains inputs,
observations, and limits. These are same-agent smoke checks, not an independent
benchmark or proof of reliability across real tasks. Automatic skill discovery in
a fresh session has not been tested. Real MP2 application use remains necessary.

## Logs and reflections

Use `logs/<contributor>/NNN-description.md`, starting with the
[interaction template](../logs/templates/interaction.md). Each contributor has
their own sequence to avoid collisions. Keep engineering verification records
separate from the application's future business audit records.

For each skill, retain examples of actual use: what task it supported, what the
agent produced, how correctness was checked, mistakes or additional work, and
the effect of any instruction revision. These records support the student's
reflection on at least three interesting skills. Merely creating these files does
not complete that reflection requirement. Students write their own judgments and
personally review the interaction logs.
