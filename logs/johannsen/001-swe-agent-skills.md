# 001 — Shared SWE agent skills

Date: 22 September 2026
Contributor: Johannsen
Branch and starting revision: `attendee`, `183ec1a`
Agent/tool: Codex
Skills used: bundled `skill-creator`; the three new repository skills were read
and exercised as described in `docs/skill-validation/2026-09-22.md`.

## Objective

Create the agreed requirements, TDD, and review skills as shared SWE practices,
add supporting instructions and interaction-record conventions, and publish the
change on a branch for a pull request.

## Original prompts (verbatim)

The related planning prompts and implementation request were:

> looks good, shall we focus on the agentic SE requirements first? like the skill.md? or

> i want skills that should be like SWE practice etc right? or what do you think the kind of skills we should have

> lets do those skills then push it to a branch for pr

Earlier repository setup and MP1 inspection preceded this task. This log does not
reconstruct that earlier session or claim its prompts are included here.

## Response summary

The initial suggestion included an Attendee-specific implementation skill. After
the user's correction, the design changed to three shared engineering practices:
requirements and acceptance criteria, test-driven implementation, and code review
and verification. Added project instructions, usage/evaluation documentation, and
a contributor-specific logging convention. Preserved the existing application
skeleton and teammate branch contents.

## Assumptions and design decisions

- Use the already-requested `attendee` branch and target the existing default
  `main` for review. The spec's `master` submission requirement remains a separate
  team coordination item.
- Share the skills through repository-local `.agents/skills/`; no per-user skill
  installation or extra agent is required to read the files explicitly.
- Keep the skills usable by all roles and distinguish agreed rules from proposals.
- Define a compiling, behaviorally failing test as the meaningful TDD red result.
- Keep review findings evidence-based and separate review from authorization to fix.
- Use contributor-specific log sequences; retain the original initialization log.
- Test with isolated, synthetic fixtures and disclose that the same author knew
  their expected outcomes. Do not represent these as independent evaluations.
- Keep personal reflection and student review fields for the student.

## Files changed

- `AGENTS.md`
- `.agents/skills/requirements-and-acceptance/SKILL.md`
- `.agents/skills/test-driven-implementation/SKILL.md`
- `.agents/skills/code-review-and-verification/SKILL.md`
- `docs/AgenticSE.md`
- `docs/DeveloperGuide.md`
- `docs/skill-validation/2026-09-22.md`
- `logs/templates/interaction.md`
- `logs/johannsen/001-swe-agent-skills.md`

Temporary Java fixtures were created under `/tmp/mp2-skill-validation.9E0VG7` for
evaluation only and are not repository application changes.

## Commands actually executed

From the repository, inspection included:

```sh
git status --short --branch
git remote -v
git log -5 --oneline
git ls-remote --heads origin
gh auth status
gh repo view --json defaultBranchRef,nameWithOwner,url
gh pr list --head attendee --json number,url,state
java -version
javac -version
/usr/libexec/java_home -V
```

These commands completed successfully. Authentication output was not copied into
the repository. `cat`/`sed` inspections covered the skill-creator instructions,
validator source, existing guides/build/logs, local MP2 spec, and new skill bodies.

Structural validation, each returning exit 0:

```sh
python3 /Users/johannsenlum/.codex/skills/.system/skill-creator/scripts/quick_validate.py .agents/skills/requirements-and-acceptance
python3 /Users/johannsenlum/.codex/skills/.system/skill-creator/scripts/quick_validate.py .agents/skills/test-driven-implementation
python3 /Users/johannsenlum/.codex/skills/.system/skill-creator/scripts/quick_validate.py .agents/skills/code-review-and-verification
```

Scratch directory creation: `mktemp -d /tmp/mp2-skill-validation.XXXXXX` (exit 0).
From that directory:

```sh
javac -d classes RegistrationBook.java RegistrationBookTest.java && java -cp classes RegistrationBookTest
javac -d classes CancellationService.java CancellationReviewRepro.java && java -cp classes CancellationReviewRepro
```

Registration was run before the fix (exit 1) and after the fix (exit 0).
Cancellation reproduction exited 1, exposing the deliberately seeded defect.
`git diff --check` also passed (exit 0) after the initial edits.

## Actual verification results

- All three skill files passed the bundled structural validator.
- Requirements walkthrough exposed unresolved policy instead of choosing a cutoff.
- Registration fixture: red = 2 passed / 1 failed with the expected duplicate
  assertion; green = 3 passed / 0 failed after the minimal change.
- Review fixture: demonstrated non-owner cancellation and loss of the owner's
  record; the defect was reported without changing the review fixture.
- Full scenario details and evaluation limits are in
  `docs/skill-validation/2026-09-22.md`.

## Problems, corrections, and skill revisions

The user redirected the initial feature-specific skill idea toward SWE practices;
the implementation follows that correction. During current environment inspection,
Java 21.0.10 was available, not the Java 25 reported during the earlier setup
session. The synthetic tests therefore explicitly record Java 21 and do not claim
to verify the MP2 runtime. No product source was changed to accommodate it.

## Outcome and limitations

The three skills, shared instructions, usage guide, evaluation record, and log
template are implemented. At the point this initial record was written,
publication and final diff/link checks were still pending. Their actual outcomes
will be recorded after execution.

The current branch lacks a Gradle wrapper. Application Gradle/JUnit checks were
not run for this instruction/documentation-only change. Real feature use, fresh
session discovery, independent evaluation, and student reflection remain pending.

Suggested commit message: `chore: add shared SWE agent skills and validation records`

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
