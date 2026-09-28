# 029 — Assignment gap check against the MP2 write-up

Date: 2026-09-28
Contributor: Joseph
Branch and starting revision: `bug-publish-event` at `a3c6b2e`; compared to `origin/main` at `41d431e`
Agent/tool: Cursor
Skills used: `.agents/skills/requirements-and-acceptance/SKILL.md`; `.agents/skills/code-review-and-verification/SKILL.md`

## Objective

Compare the assignment write-up (project description, restrictions, submission, grading) with evidence on GitHub and in this checkout, and list gaps. No product code change.

## Original prompts (verbatim)

The user pasted the full MP2 project description, restrictions, submission instructions, and grading criteria, then asked:

> can u see what else we are missing?

## Response summary

Reported gaps against `origin/main` (what GitHub currently serves) and open PRs. Distinguished assignment requirements, team work already on a PR, and unverified items.

## Assumptions and design decisions

- Graders said they will pull `master`. The default branch is `main`; there is no `master` ref.
- GitHub Pages is configured (`/docs` on `main`). The site root URL 404s; `UserGuide.html` on that host returned content.
- PR #47 and #48 are team work that is not on `main` yet.

## Files changed

- `logs/joseph/029-assignment-gap-check.md`

## Commands actually executed

- `gh repo view`, `gh release list` (empty), `gh api .../pages`, `gh pr view 47`, `gh pr view 48`
- WebFetch of `https://cs3227-2610-mp2-orbit.github.io/CS3227-2610-MP2/` (404) and `.../UserGuide.html` (200)
- `git ls-remote --heads origin` (only `main`)
- Read `README.md`, `docs/Reflections.md`, `docs/UserGuide.md`, `docs/DeveloperGuide.md`, `logs/prompts_summary.md`, `build.gradle`, `website/`

## Actual verification results

See the conversation report. No product tests were re-run for this check.

## Problems, corrections, and skill revisions

None. This was a read-only gap list.

## Outcome and limitations

The Canvas quiz (due 4 Sep) was not checked. JavaFX screens were not launched. PR #47 claims were taken from its PR body and check rollup, not re-executed here.
