# 001 — AI-assisted testing and review skill integration

Date: 22 September 2026
Contributor: Joseph
Branch and starting revision: `main`, `3d14c2a`
Agent/tool: Codex
Skills used: `/Users/josephkwok/.codex/skills/.system/skill-creator/SKILL.md` (read and applied)

## Objective

Integrate the agreed AI-assisted unit-testing, integration-testing, and
eight-step code-review guidance into the existing shared repository skills.
Remove the previous requirements-only interaction record. Do not change product
source or application behavior.

## Original prompts (verbatim)

> PLEASE IMPLEMENT THIS PLAN:
> # Integrate AI-assisted testing and review skills
>
> ## Summary
>
> Extend the existing repository skills instead of creating overlapping new skills. Delete the Joseph requirements-review log, then add the requested AI-assisted testing prompts and eight-step review workflow.
>
> ## Changes
>
> - Delete `logs/joseph/001-mp2-requirements-review.md`.
> - Update `.agents/skills/test-driven-implementation/SKILL.md` with:
>   - Explicit unit-test prompting guidance.
>   - Explicit integration-test prompting guidance.
>   - Structured test-case template: scope, setup, action, expected result, side effects, errors, and edge cases.
>   - Requirement to explain generated test reasoning.
>   - Guidance to review, refine, and consolidate AI-generated tests.
>   - Verification that invalid inputs leave prohibited side effects unchanged.
>   - Guidance to use mocks only where appropriate and test real integration boundaries when required.
> - Update `.agents/skills/code-review-and-verification/SKILL.md` with the eight-step review workflow:
>   1. Run tests, builds, static analysis, and available security checks.
>   2. Verify context, intent, and architecture.
>   3. Assess readability and maintainability.
>   4. Inspect dependencies for hallucinated or suspicious packages.
>   5. Check AI-specific defects, including deleted or weakened tests.
>   6. Request collaborative review for complex or sensitive changes.
>   7. Automate repeatable checks through CI and security tooling when available.
>   8. Improve and document the review workflow based on observed failures.
> - Add a concise Joseph interaction log for this skill implementation only, not every conversational prompt.
>
> ## Validation
>
> - Run the bundled `quick_validate.py` against both modified skill folders.
> - Run `git diff --check`.
> - Perform isolated behavioral checks using:
>   - A unit-testing prompt for event-capacity or registration logic.
>   - An integration-testing prompt involving persistence and notification verification.
>   - A review prompt containing a deliberately flawed dependency, authorization rule, or weakened test.
> - Confirm the resulting guidance distinguishes checks that were actually run from checks that are unavailable.
> - Do not add CodeQL, Dependabot, CI workflows, or `CONTRIBUTING.md` in this pass; the skills may recommend them, but their configuration is deferred.
>
> ## Assumptions
>
> - “Delete the 001-mp2- file” means delete only `logs/joseph/001-mp2-requirements-review.md`.
> - The existing three-skill structure remains the project’s shared Agentic SE design.
> - No application source, APIs, database schema, or product features will be changed.

## Response summary

Updated the existing test-driven implementation and code-review skills, removed
the prior Joseph requirements-review log, and retained the existing three-skill
structure. This log records the skill implementation task only, not every
planning or conversational prompt.

## Assumptions and design decisions

- Unit and integration testing guidance belongs in
  `test-driven-implementation`; review guidance belongs in
  `code-review-and-verification`.
- CodeQL, Dependabot, CI, and contribution-file configuration remain deferred;
  the review skill only requires available tooling to be reported accurately.
- The application source, tests, APIs, persistence, and product documentation
  remain unchanged.

## Files changed

- `.agents/skills/test-driven-implementation/SKILL.md`
- `.agents/skills/code-review-and-verification/SKILL.md`
- `logs/joseph/001-mp2-requirements-review.md` — deleted as requested.
- `logs/joseph/001-skill-integration.md`

## Commands actually executed

From `/Users/josephkwok/Desktop/Website/cs3227/MP2`:

```sh
git status --short --branch
sed -n '1,280p' .agents/skills/test-driven-implementation/SKILL.md
sed -n '1,300p' .agents/skills/code-review-and-verification/SKILL.md
sed -n '1,260p' /Users/josephkwok/.codex/skills/.system/skill-creator/SKILL.md
python3 /Users/josephkwok/.codex/skills/.system/skill-creator/scripts/quick_validate.py .agents/skills/test-driven-implementation
python3 /Users/josephkwok/.codex/skills/.system/skill-creator/scripts/quick_validate.py .agents/skills/code-review-and-verification
git diff --check
git diff -- .agents/skills/test-driven-implementation/SKILL.md .agents/skills/code-review-and-verification/SKILL.md
rg -n "Prompt AI-assisted tests precisely|unit tests|integration tests|structured test|prohibited side effects|Eight-step review workflow|Run functional|Scrutinize dependencies|AI-specific pitfalls|Automate repeatable|Improve the workflow" .agents/skills/test-driven-implementation/SKILL.md .agents/skills/code-review-and-verification/SKILL.md
rg --files logs/joseph | sort
```

The first `apply_patch` attempt failed because its context assumed a different
line wrap in the testing skill. No files were changed by that failed attempt.
The patch was narrowed to stable section boundaries and applied successfully.

## Actual verification results

- Both modified skill folders passed the bundled structural validator with
  `Skill is valid!` and exit status 0.
- `git diff --check` completed with exit status 0.
- The diff inspection confirmed that only the two existing skill files were
  modified, the old requirements-review file was removed, and the replacement
  skill-integration log was added.
- Manual same-agent forward checks covered three synthetic prompts:
  - A unit-test prompt for event-capacity or registration behavior was directed
    to request scope, setup, action, expected result, prohibited side effects,
    edge cases, and rationale.
  - An integration-test prompt involving persistence and notifications was
    directed to identify the real boundary, explicit mocks, successful and
    failure cases, and absence of notification on invalid input.
  - A review prompt containing a suspicious dependency, an authorization defect,
    and a weakened test was directed through the eight review steps, including
    unavailable-check reporting and AI-specific test inspection.
- No application test suite was run because this change only modifies Markdown
  skill instructions and does not change application code.

## Problems, corrections, and skill revisions

The initial patch context mismatch was corrected without changing the intended
scope. The existing duplicated phrase in the review skill (`Update an Update an`)
was corrected while integrating the new workflow. The forward checks were
same-agent manual evaluations, not independent benchmarks; they demonstrate that
the instructions cover the requested cases but do not prove reliable behavior
across all future tasks.

## Outcome and limitations

The implementation is limited to repository skill instructions and the required
interaction record. No application feature was implemented.

Suggested commit message: `docs: extend AI-assisted testing and review skills`

## Student review

- [ ] I confirmed that the original prompts are accurate.
- [ ] I confirmed that the changed-file list is accurate.
- [ ] I confirmed that recorded commands were actually executed.
- [ ] I confirmed that verification results and limitations are accurate.
- [ ] I added any mistakes or disagreements omitted by the AI.

Reviewed by:
Review date:
