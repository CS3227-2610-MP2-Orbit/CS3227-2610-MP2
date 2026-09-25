# 005 — T1 and C1 deterministic trace graders

Date: 22 September 2026
Contributor: Joseph
Branch and starting revision: `joseph-edit-skills`, `33efa1d`
Agent/tool: Codex
Skills used: `/Users/josephkwok/Desktop/Website/cs3227/MP2/.agents/skills/test-driven-implementation/SKILL.md` (read and applied; no correction required)

## Objective

Extend the deterministic, no-LLM JSONL trace grading approach to the repository's
test-driven-implementation (T1) and code-review-and-verification (C1) skills, add
focused tests, and document usage beside the existing requirements-skill grader.

## Original prompts (verbatim)

```text
can you do the same thing for the other 2 skills?
```

## Response summary

Added one standard-library Python grader and one focused unittest module for each
remaining skill. The T1 grader checks trace validity, skill evidence, a real
command-execution assertion failure, a later non-test Java implementation edit,
and a passing rerun that verifies duplicate rejection and unchanged count. The
C1 grader checks trace validity, skill evidence, the missing ownership check,
resulting registration state loss, executed reproduction evidence with
fixture-only scope, and absence of application-source edits. Updated the Agentic
SE usage section with all three grader commands and named checks.

## Assumptions and design decisions

- “The other 2 skills” means `test-driven-implementation` and
  `code-review-and-verification`, using the documented T1 and C1 cases as the
  deterministic grading contracts.
- A prose claim is insufficient for T1 red/green or C1 reproduction evidence;
  the trace must contain a command-execution event.
- T1 red requires a non-zero behavioral assertion failure. A compilation or
  setup error without an assertion failure does not pass.
- T1 implementation evidence must occur after red and target a non-test,
  non-reproduction Java file. Green evidence must occur after that edit and
  report both duplicate rejection and unchanged count.
- C1 scope evidence must distinguish a synthetic fixture from the production
  application. Its `NO_IMPLEMENTATION` check reuses the existing deterministic
  `src/` write detector so isolated reproduction files remain allowed.
- The graders use only Python's standard library and do not call an LLM.

## Files changed

- `tools/graders/grade_test_driven_skill.py`
- `tools/graders/test_grade_test_driven_skill.py`
- `tools/graders/grade_code_review_skill.py`
- `tools/graders/test_grade_code_review_skill.py`
- `docs/AgenticSE.md`
- `logs/joseph/006-t1-c1-trace-graders.md`

Unrelated edits and additions in `AGENTS.md` and earlier Joseph logs were
preserved. The reviewed `logs/joseph/003-requirements-trace-grader.md` was not
rewritten.

## Commands actually executed

From `/Users/josephkwok/Desktop/Website/cs3227/MP2`:

```sh
git status --short --branch
sed -n '1,340p' .agents/skills/test-driven-implementation/SKILL.md
sed -n '1,360p' .agents/skills/code-review-and-verification/SKILL.md
sed -n '46,115p' docs/AgenticSE.md
sed -n '45,145p' docs/skill-validation/2026-09-22.md
sed -n '1,320p' AGENTS.md
sed -n '1,380p' tools/graders/grade_requirements_skill.py
python3 -m unittest discover -s tools/graders -p 'test_grade_test_driven_skill.py' -v
python3 -m unittest discover -s tools/graders -p 'test_grade_code_review_skill.py' -v
python3 -m unittest discover -s tools/graders -p 'test_grade_test_driven_skill.py' -v
python3 -m unittest discover -s tools/graders -p 'test_grade_code_review_skill.py' -v
python3 -m unittest discover -s tools/graders -p 'test_*.py' -v && python3 -m py_compile tools/graders/grade_requirements_skill.py tools/graders/grade_test_driven_skill.py tools/graders/grade_code_review_skill.py && git diff --check
git diff --check -- docs/AgenticSE.md
if rg -n '[[:blank:]]+$' tools/graders/grade_test_driven_skill.py tools/graders/grade_code_review_skill.py tools/graders/test_grade_test_driven_skill.py tools/graders/test_grade_code_review_skill.py; then exit 1; fi
rm -rf /Users/josephkwok/Desktop/Website/cs3227/MP2/tools/graders/__pycache__
git rev-parse --short HEAD
git status --short --untracked-files=all
```

## Actual verification results

- T1 red: the scaffold run produced three assertion failures, exit 1. The
  complete-trace and CLI assertions failed because behavior was not implemented;
  the compile-error test also confirmed diagnostics were not yet present.
- C1 red: the scaffold run produced two assertion failures and one pass, exit 1.
  The existing source-write detector correctly rejected a `src/` patch while the
  new skill, finding, and CLI behavior remained absent.
- Focused green: 4 T1 tests passed and 4 C1 tests passed, each exit 0.
- Broader grader suite: all 15 tests passed, exit 0.
- All three grader scripts passed `py_compile` before the whitespace check.
- Scoped `git diff --check` for `docs/AgenticSE.md` and trailing-whitespace checks
  for the four new Python files passed, exit 0.
- Repository-wide `git diff --check` was not clean: it exited 2 because the
  unrelated `logs/joseph/001-skill-integration.md:154` contains trailing
  whitespace. That historical/student-edited log was not changed.
- No Gradle or Java application tests were run because this task changes only
  Python grading utilities, their tests, documentation, and this log.

## Problems, corrections, and skill revisions

The first combined red command used `&&`, so the expected T1 failure prevented
the C1 suite from running; C1 was then run separately. After the initial green,
the checks were tightened so final prose alone cannot satisfy T1 execution or C1
reproduction evidence. Generated Python bytecode under `__pycache__` was removed.
No repository skill instructions were revised.

## Outcome and limitations

The T1 and C1 trace graders, tests, and documentation are complete. Their checks
are intentionally case-specific and deterministic: they grade the documented
duplicate-registration and ownership-defect probes, not arbitrary implementation
or review tasks. They depend on current Codex command-execution and agent-message
event shapes and may require updates if those shapes change.

Suggested commit message: `test: add T1 and C1 trace graders`

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
