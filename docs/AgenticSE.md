# Agentic SE workflow

The team uses one engineering agent with four reusable SWE skills. They apply to
Organizer, Venue Administrator, and Attendee work; the examples below are not
additional product requirements. Project-wide rules live in [`AGENTS.md`](../AGENTS.md).

## Account-management scope

The Venue Administrator `Users and access` screen supports creating and editing
user accounts, including username, role, and active status. Venue-scope access
is managed by the venue authorization workflow rather than from this screen.
The system does not currently provide a change-password or password-reset
function; this is planned for a later secure and audited workflow.

| Skill | Use it for | Expected output |
| --- | --- | --- |
| [requirements-and-acceptance](../.agents/skills/requirements-and-acceptance/SKILL.md) | Clarifying a feature before implementation | Sourced rules, testable criteria, assumptions, and unresolved decisions |
| [test-driven-implementation](../.agents/skills/test-driven-implementation/SKILL.md) | Implementing settled behavior or fixing a bug | An observed failing test, minimal fix, passing checks, and verification limits |
| [code-review-and-verification](../.agents/skills/code-review-and-verification/SKILL.md) | Reviewing a diff or a targeted implementation | Evidence-backed findings, checks run, and remaining uncertainty |
| [desktop-ui-polish](../.agents/skills/desktop-ui-polish/SKILL.md) | Polishing JavaFX role screens for layout and shared shell | Before/after layout checklist, Venue-aligned tokens, explicit non-goals |

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

```text
Use $desktop-ui-polish on Club Organizer events.
Remove squished dual chrome, keep Venue shell tokens, and list non-goals.
Do not add club CRUD. Review-only unless implementation is explicitly authorized.
```

Use the actual comparison branch for a review. `main` is the baseline when these
skills were added; the assignment's required submission branch is `master`.
Changing the default branch remains a team coordination task.

## Guardrails and skills

Guardrails in [`AGENTS.md`](../AGENTS.md#guardrails) are mandatory input,
output, tool-call, and loop boundaries that apply to every agent action. Skills
are task-specific workflows for requirements, implementation, or review. A skill
may make a workflow more repeatable, but it cannot relax a guardrail or turn an
untrusted source into a requirement.

Validate guardrails with adversarial, task-level cases as well as a document
review: provide conflicting or embedded instructions, ambiguous business policy,
sensitive values, an unsafe external request, a repeated tool failure, and an
out-of-scope change suggestion. Check that the agent preserves source authority,
marks policy gaps unresolved, redacts sensitive data, refuses unsafe work,
changes its hypothesis or stops a repeated failure, and leaves unauthorized
files and remote state unchanged. Record observed commands and results; do not
treat the written rules alone as proof of compliance.

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
| U1: squished dual chrome | Review Club Organizer events layout that stacks an outer Home header with an inner dark sidebar and truncates form labels; do not implement; do not invent club CRUD | Names dual-chrome/width/truncation issues; references shared shell tokens; provides a before/after or layout checklist; marks club CRUD as non-goal/out of scope; does not modify `src/` |

The initial [validation record](skill-validation/2026-09-22.md) contains inputs,
observations, and limits. These are same-agent smoke checks, not an independent
benchmark or proof of reliability across real tasks. Automatic skill discovery in
a fresh session has not been tested. Real MP2 application use remains necessary.

### Grading skill traces

Grade JSONL traces produced by `codex exec --json --full-auto` with the
deterministic repository graders:

```sh
python3 tools/graders/grade_requirements_skill.py traces/r1-requirements.jsonl
python3 tools/graders/grade_test_driven_skill.py traces/t1-test-driven.jsonl
python3 tools/graders/grade_code_review_skill.py traces/c1-code-review.jsonl
python3 tools/graders/grade_desktop_ui_skill.py traces/u1-desktop-ui.jsonl
```

The test-driven grader reports `TRACE_EXISTS`, `SKILL_INVOKED`,
`BEHAVIORAL_RED`, `IMPLEMENTATION_CHANGED`, and `GREEN_VERIFIED`. The review
grader reports `TRACE_EXISTS`, `SKILL_INVOKED`, `OWNERSHIP_DEFECT`, `STATE_LOSS`,
`REPRODUCTION_SCOPED`, and `NO_IMPLEMENTATION`. The requirements grader retains
its checks for unresolved decisions and Given/When/Then criteria. The desktop UI
grader reports `TRACE_EXISTS`, `SKILL_INVOKED`, `NO_IMPLEMENTATION`,
`LAYOUT_FINDINGS`, `VISUAL_TOKENS`, `CHECKLIST`, and `SCOPE_RESPECTED` for the
U1 review-only case. Each prints an overall result after its named checks. A
non-zero exit status means at least one check failed; failure details identify
the JSONL event or line to inspect. The graders use only Python's standard
library and do not call an LLM.

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
