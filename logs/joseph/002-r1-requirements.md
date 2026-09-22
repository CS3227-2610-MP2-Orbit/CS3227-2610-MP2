# 002 — R1 attendee cancellation requirements

Date: 22 September 2026
Contributor: Joseph
Branch and starting revision: `main`, `3d14c2a`
Agent/tool: Codex
Skills used: `/Users/josephkwok/Desktop/Website/cs3227/MP2/.agents/skills/requirements-and-acceptance/SKILL.md` (read and applied)

## Objective

Define sourced rules, open policy decisions, and testable Given/When/Then
acceptance criteria for the request: “Attendees can cancel before the event.”
Do not implement application code.

## Original prompts (verbatim)

```text
codex exec --json --full-auto "$(cat <<'EOF'
Use $requirements-and-acceptance only.
Do not implement code.
Task (case R1): Define acceptance criteria for: "Attendees can cancel before the event."
Read AGENTS.md and .agents/skills/requirements-and-acceptance/SKILL.md first.
Output: sourced rules, open decisions, and Given/When/Then criteria only.
EOF
)" | tee traces/r1-requirements.jsonl
```

## Response summary

The acceptance definition is partially ready. The request establishes only that
attendee cancellation before an event is desired. Existing repository material
assigns attendee registration responsibility and identifies cancellation as an
ambiguous-policy exercise, but does not settle the cutoff, clock, ownership,
eligibility, side effects, or failure semantics. The criteria below keep those
dependencies explicit and do not authorize implementation.

## Assumptions and design decisions

- Sourced rule: the user-requested behavior is attendee cancellation before an
  event; this is the requested product intent, not a complete policy.
- Sourced rule: attendee work includes registration, notifications, check-in,
  and attendance history; registration records and related business rules belong
  to the registration package.
- Sourced rule: the current User Guide describes only the initial application
  skeleton, so no cancellation behavior is documented as implemented.
- Sourced rule: the Agentic SE examples are explicitly not additional product
  requirements; the R1 validation record is evidence of missing policy, not an
  accepted cancellation contract.
- No proposal was promoted to an accepted rule. In particular, no arbitrary
  deadline, time zone, waitlist behavior, notification guarantee, role
  exception, or post-check-in policy was invented.

## Files changed

- `logs/joseph/002-r1-requirements.md`

## Commands actually executed

From `/Users/josephkwok/Desktop/Website/cs3227/MP2`:

```sh
pwd && printf '%s\n' '--- status ---' && git status --short --branch && printf '%s\n' '--- root files ---' && rg --files -g 'AGENTS.md' -g '.agents/skills/requirements-and-acceptance/SKILL.md' -g 'docs/**' -g 'logs/**' -g 'src/**' -g 'pom.xml' -g 'build.gradle*' -g 'README*' | sed -n '1,240p'
printf '%s\n' '--- AGENTS.md ---' && sed -n '1,260p' AGENTS.md && printf '%s\n' '--- requirements skill ---' && sed -n '1,260p' .agents/skills/requirements-and-acceptance/SKILL.md && printf '%s\n' '--- interaction template ---' && sed -n '1,240p' logs/templates/interaction.md && printf '%s\n' '--- existing joseph log ---' && sed -n '1,260p' logs/joseph/001-skill-integration.md
printf '%s\n' '--- docs/UserGuide.md ---' && sed -n '1,320p' docs/UserGuide.md && printf '%s\n' '--- docs/DeveloperGuide.md ---' && sed -n '1,360p' docs/DeveloperGuide.md && printf '%s\n' '--- attendee README ---' && sed -n '1,260p' src/main/java/seedu/eventmanager/attendee/README.md && printf '%s\n' '--- registration README ---' && sed -n '1,260p' src/main/java/seedu/eventmanager/registration/README.md && printf '%s\n' '--- all source/test files ---' && rg --files src | sort
printf '%s\n' '--- cancellation/registration references ---' && rg -n -i "cancel|registration|attendee|event" --glob '!logs/**' --glob '!build/**' . | sed -n '1,260p' && printf '%s\n' '--- project README ---' && sed -n '1,280p' README.md && printf '%s\n' '--- AgenticSE ---' && sed -n '1,360p' docs/AgenticSE.md && printf '%s\n' '--- MainTest ---' && sed -n '1,220p' src/test/java/seedu/eventmanager/MainTest.java && printf '%s\n' '--- recent commits ---' && git log --oneline -8
printf '%s\n' '--- skill validation record ---' && sed -n '1,180p' docs/skill-validation/2026-09-22.md && printf '%s\n' '--- website/index.md ---' && sed -n '1,160p' website/index.md && printf '%s\n' '--- git diff summary and existing untracked log metadata ---' && git diff --stat && find logs/joseph -maxdepth 1 -type f -print | sort
```

## Actual verification results

- Read `AGENTS.md` and the requirements-and-acceptance skill before analysis.
- Inspected the current branch, status, relevant documentation, attendee and
  registration package notes, source/test inventory, cancellation references,
  and the existing R1 validation record.
- No application tests were run because this task did not implement code.
- No product source, tests, or user-facing documentation were changed.

## Problems, corrections, and skill revisions

No observable agent mistake or skill correction occurred.

## Outcome and limitations

Acceptance criteria were produced with unresolved policy dependencies. The
repository contains no formal cancellation contract or implemented registration
model, so persistence, notification, audit, concurrency, and UI behavior cannot
be claimed as verified. The student must settle the listed policy decisions
before implementation.

Suggested commit message: `docs: define attendee cancellation acceptance criteria`






# Reflections on Agentic SE

## 1. What we customized

Our team uses a single engineering agent (Codex) with shared repository
customization rather than three separate personal agents:

- **Instructions / guardrails:** `AGENTS.md` (role ownership, shared services,
  no invented requirements, logging rules, commit boundaries)
- **Skills:** three reusable SWE skills under `.agents/skills/`
  1. `requirements-and-acceptance`
  2. `test-driven-implementation`
  3. `code-review-and-verification`
- **Workflow docs:** `docs/AgenticSE.md` (how to invoke skills + validation cases)
- **Evidence:** per-contributor logs under `logs/<name>/`

This matches the lecture idea of customizing one agent: durable rules live in the
repo so Joseph, Jordan, and Johannsen get the same behavior on role-specific work.

We treated lecture concepts as follows:
- **Skills** = task playbooks (`SKILL.md`)
- **Config** = Codex skill enable/path control when needed (`config.toml` style);
  shared behavior still mainly comes from `AGENTS.md` + checked-in skills
- **Graders / model-assisted testing** = how we check whether skills work
- **Memory** = optional; we preferred repo instructions so knowledge is shareable

## 2. Why “testing skills” matters (R1 / T1 / C1)

Creating skill files is not proof that the agent follows them. So we defined
fixed **skill test cases** (like unit tests for the agent process):

| Case | Skill | Probe task | What “correct” means |
| --- | --- | --- | --- |
| **R1** | requirements-and-acceptance | “Attendees can cancel before the event.” | Expose missing policy; do not invent cutoff/ownership; write conditional Given/When/Then; do not implement |
| **T1** | test-driven-implementation | Scratch registration book wrongly accepts duplicates | Show a real failing behavioral test first; minimal fix; green assertions; do not call compile failure “red” |
| **C1** | code-review-and-verification | Cancel by registration ID without ownership check | Find the non-owner path + state loss; give reproduction; do not over-claim about production code |

These cases are documented in `docs/AgenticSE.md` and first exercised in
`docs/skill-validation/2026-09-22.md`.

**How we know a run is “correct”:** not vibes — comparison against the rubric.
Input is fixed; expected behaviors are listed; actual output/trace is compared;
pass/fail is recorded. That is the same structure as ordinary testing, applied to
agent workflow instead of app features.

## 3. Interesting skill 1 — requirements-and-acceptance (R1)

### Purpose
Stop implementation from starting on invented product rules. For MP2 this matters
because many workflows (cancellation, capacity, venue approval, check-in windows)
look simple in a sentence but hide policy decisions.

### What I ran
I used the lecture-style reproducible Codex execution and logged it in
`logs/joseph/002-r1-requirements.md`:

```bash
codex exec --json --full-auto "$(cat <<'EOF'
Use $requirements-and-acceptance only.
Do not implement code.
Task (case R1): Define acceptance criteria for: "Attendees can cancel before the event."
Read AGENTS.md and .agents/skills/requirements-and-acceptance/SKILL.md first.
Output: sourced rules, open decisions, and Given/When/Then criteria only.
EOF
)" | tee traces/r1-requirements.jsonl