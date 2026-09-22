# 003 — Deterministic requirements-skill trace grader

Date: 22 September 2026
Contributor: Joseph
Branch and starting revision: `joseph-edit-skills`, `33efa1d`
Agent/tool: Codex
Skills used: `/Users/josephkwok/Desktop/Website/cs3227/MP2/.agents/skills/test-driven-implementation/SKILL.md` (read and applied)

## Objective

Create a lightweight, deterministic Python grader for JSONL traces produced by
`codex exec --json --full-auto`, covering only trace existence, skill use, no
application implementation, unresolved policy decisions, and
Given/When/Then criteria. Document its usage under the Agentic SE validation
section. Do not modify application source files.

## Original prompts (verbatim)

```text
Create a lightweight deterministic grader for Codex skill traces in this MP2 repo.

Input: a JSONL trace from:
  codex exec --json --full-auto "<prompt>"

Write a small script (Python preferred) at:
  tools/graders/grade_requirements_skill.py

It must parse the JSONL events and return pass/fail for these checks only:
1. TRACE_EXISTS: file is non-empty JSONL
2. SKILL_INVOKED: evidence the requirements-and-acceptance skill was used
  (skill name/path appears in tool/file reads or explicit invocation)
3. NO_IMPLEMENTATION: no app source files under src/ were created/modified
4. OPEN_DECISIONS: final assistant output mentions unresolved policy decisions
  (e.g. cutoff, timezone, ownership, eligibility, side effects)
5. CRITERIA_PRESENT: final output includes at least one Given/When/Then style criterion

Print a clear PASS/FAIL per check, then overall PASS only if all pass.
If a check fails, print which events/lines to inspect in the JSONL.

Do not call an LLM inside this grader. Keep checks deterministic and debuggable.
Also add a short usage section in docs/AgenticSE.md under Validating the skills.
```

## Response summary

Added the requested standard-library Python grader and focused unittest suite.
The grader parses object-valued JSONL events, extracts the final assistant
message, detects explicit requirements-skill evidence, reports source-write
evidence with line numbers, checks unresolved policy language and ordered
Given/When/Then text, and exits non-zero when any check fails. Added the short
usage section to `docs/AgenticSE.md`.

## Assumptions and design decisions

- A JSONL trace must contain at least one valid JSON object and no malformed
  non-blank lines; blank lines are ignored.
- Skill evidence is satisfied by the requirements skill name/path appearing in
  an event, including an explicit `$requirements-and-acceptance` invocation or
  a skill file-read event.
- `NO_IMPLEMENTATION` is trace-based: patch headers, file-write tools, and
  common shell write/edit commands targeting `src/` are reported. Read-only
  commands such as `sed` are not reported.
- The final assistant output is taken from the last assistant/agent-message
  event supported by common Codex JSON event shapes.
- Open-decision detection requires both unresolved-decision language and a
  policy topic such as cutoff, timezone, ownership, eligibility, or side
  effects. A statement that no open decisions remain does not pass.
- No LLM, network service, Java source, or Gradle dependency was added.

## Files changed

- `tools/graders/grade_requirements_skill.py`
- `tools/graders/test_grade_requirements_skill.py`
- `docs/AgenticSE.md`
- `logs/joseph/003-requirements-trace-grader.md`

Pre-existing `logs/joseph/reflections.md` was preserved. No application source
files under `src/` were changed by this task.

## Commands actually executed

From `/Users/josephkwok/Desktop/Website/cs3227/MP2`:

```sh
git status --short --branch
sed -n '1,320p' .agents/skills/test-driven-implementation/SKILL.md
rg --files tools docs logs | sort | sed -n '1,260p'
rg -n -A80 -B10 "Validating the skills" docs/AgenticSE.md
rg --files -g '*.jsonl' -g '*.json' -g '*trace*' . | sort | sed -n '1,200p'
python3 -m unittest discover -s tools/graders -p 'test_*.py'
python3 -m unittest discover -s tools/graders -p 'test_*.py'
python3 -m unittest discover -s tools/graders -p 'test_*.py'
python3 -m unittest discover -s tools/graders -p 'test_*.py' && python3 -m py_compile tools/graders/grade_requirements_skill.py && git diff --check
git rev-parse --short HEAD && git status --short --branch
git diff -- logs/joseph/002-r1-requirements.md | sed -n '1,260p'
```

The first unittest command ran before the target module existed and exited 1
with `ModuleNotFoundError`; this was treated as test setup failure, not intended
behavioral red evidence. After a minimal scaffold, the second run exited 1 with
four assertion failures. The final focused run completed with exit 0.

## Actual verification results

- Focused Python suite: 7 tests passed, exit 0.
- `py_compile` for the grader: exit 0.
- `git diff --check`: exit 0.
- CLI tests verified overall exit 0 for a complete trace and exit 1 with
  per-check output plus `Inspect JSONL` guidance for a failing criterion.
- Tests covered valid and invalid JSONL, explicit skill invocation and file
  reads, read-only source inspection, patch and shell writes under `src/`,
  unresolved versus settled decisions, criteria extraction, and CLI output.
- No Gradle/Java test run was needed because no application source or Java test
  was changed.

## Problems, corrections, and skill revisions

The initial focused test run failed because the new module did not yet exist.
The missing test setup was corrected with a minimal scaffold before evaluating
behavior. A later patch broadened source-write detection for multiline patches,
move/write commands, and hyphenated policy terms; the full focused suite still
passed. No repository skill instructions were revised.

## Outcome and limitations

The requested grader and documentation usage are complete. Detection is
deliberately lightweight and event-based; it does not prove filesystem state
outside the recorded trace and may not recognize future Codex event shapes or
unusual write commands. No application implementation was performed.

Suggested commit message: `test: add deterministic requirements trace grader`

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by: Joseph kwok  
Review date: 22/09




