---
name: test-driven-implementation
description: Implement or fix agreed software behavior through a verified failing test, a minimal implementation, and refactoring. Use for behavior changes and regression fixes with settled acceptance criteria; not for documentation-only or formatting changes.
---

# Test-driven implementation

Deliver a small behavior change with evidence that its tests detect the missing
or broken behavior. Work from the supplied acceptance criteria; clarify material
policy gaps before implementing the affected behavior.

## Inspect and select a slice

Read repository instructions, the affected implementation and contracts, existing
tests, and build configuration. Identify the smallest coherent criterion or bug
reproduction. Inspect local changes before editing and preserve unrelated work.
Use the project's test framework and available build entrypoint. If required
tooling is absent, report it; do not label an unexecuted test as passing or invent
a successful red/green cycle.

## Red: prove the test detects the problem

Write a test at the lowest level that can demonstrate the observable behavior.
Use integration tests where real persistence, transaction boundaries, or wiring
are essential; use UI tests for behavior that requires UI interaction.

Run the focused test before changing production behavior. Record the command,
exit status, failing assertion, and why the failure matches the missing behavior.
A missing dependency, compilation failure, or unrelated fixture failure is not
the intended red result. Resolve test setup first; minimal API scaffolding is
acceptable if it does not implement the behavior under test.

If the test already passes, inspect whether the behavior already exists or the
test does not exercise the defect. Do not manufacture a production regression to
claim TDD. For already-correct behavior, label the work as regression coverage;
an isolated mutation can separately test whether the assertion is meaningful.

## Green: implement the behavior

Implement only the selected criteria using existing domain and shared service
contracts. Keep rules out of UI handlers, enforce role/resource access at the
workflow boundary, and preserve agreed atomicity and failure behavior.

Assert results and relevant state changes, including the absence of prohibited
side effects. Avoid asserting only that a mock was called or copying the production
algorithm into the test. Prefer fixed clocks, controlled identifiers, and isolated
fixtures over sleep-based or order-dependent tests. Run the focused tests again
and record the actual outcome.

## Refactor and verify

Refactor only where it improves the touched code while retaining passing tests.
Run the relevant broader suite and required repository checks. Check boundaries,
invalid input, authorization, repeat operations, and failures that matter to this
slice. For shared contracts, check affected consumers or identify those unavailable
in the checkout. Do not imply concurrency safety from a sequential unit test.

Update affected documentation and the interaction log. Report criteria completed,
files changed, red/green evidence, broader verification, and remaining limitations.
If execution is blocked, separate written tests from verified tests. Never weaken
an assertion simply to make an incorrect implementation pass.
