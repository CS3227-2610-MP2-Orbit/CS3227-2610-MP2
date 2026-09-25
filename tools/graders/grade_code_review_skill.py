#!/usr/bin/env python3
"""Deterministically grade a code-review-and-verification skill JSONL trace."""

from __future__ import annotations

import argparse
import re
from pathlib import Path

from grade_requirements_skill import (
    CheckResult,
    TraceEvent,
    _final_assistant_output,
    _line_hint,
    _serialized,
    check_no_implementation,
    check_trace_exists,
    read_trace,
)


SKILL = re.compile(r"code-review-and-verification", re.IGNORECASE)
OWNERSHIP = re.compile(r"owner|ownership|non-owner|actor", re.IGNORECASE)
AUTHORIZATION_DEFECT = re.compile(
    r"ignore[sd]?\s+(?:the\s+)?actor|(?:does not|doesn't|never|missing|without)\s+"
    r"(?:check|verify|enforce)[\s\S]{0,50}(?:owner|ownership|actor)|"
    r"non-owner[\s\S]{0,60}(?:can|able|allowed)[\s\S]{0,40}(?:cancel|remove)",
    re.IGNORECASE,
)
STATE_MUTATION = re.compile(
    r"remove[sd]?|delete[sd]?|cancel(?:led|s)?|exists\s*=\s*false|state\s+loss",
    re.IGNORECASE,
)
PROTECTED_STATE = re.compile(
    r"another\s+attendee|owner(?:'s)?\s+registration|leave[sd]?\s+(?:it|the\s+registration)\s+intact|"
    r"intact|exists\s*=\s*true",
    re.IGNORECASE,
)
REPRODUCTION = re.compile(r"repro(?:duction)?|Expected\s*:|Actual\s*:|AssertionError", re.IGNORECASE)
EXECUTION_EVENT = re.compile(r"command_execution|exec_command|shell_command", re.IGNORECASE)
EXPECTED_ACTUAL = re.compile(
    r"Expected\s*:[\s\S]{0,250}Actual\s*:|AssertionError[\s\S]{0,200}non-owner|"
    r"non-owner[\s\S]{0,200}AssertionError",
    re.IGNORECASE,
)
FIXTURE_SCOPE = re.compile(r"synthetic|fixture|sample|scratch|deliberately\s+faulty", re.IGNORECASE)
NOT_PRODUCTION = re.compile(
    r"not\s+(?:a\s+)?claim[\s\S]{0,80}(?:production|application)|"
    r"not\s+(?:the\s+)?production|does\s+not\s+(?:prove|show)[\s\S]{0,80}(?:production|application)",
    re.IGNORECASE,
)


def check_skill_invoked(events: list[TraceEvent]) -> CheckResult:
    lines = [event.line_no for event in events if SKILL.search(_serialized(event.data))]
    if lines:
        return CheckResult(True, f"skill invocation/evidence found on line(s) {', '.join(map(str, lines))}")
    return CheckResult(
        False,
        f"Inspect JSONL lines {_line_hint(events)} for code-review-and-verification invocation/evidence",
    )


def check_ownership_defect(events: list[TraceEvent]) -> CheckResult:
    text, line_no = _final_assistant_output(events)
    if text and OWNERSHIP.search(text) and AUTHORIZATION_DEFECT.search(text):
        return CheckResult(True, f"ownership/actor authorization defect identified on line {line_no}")
    return CheckResult(
        False,
        f"Inspect JSONL line {line_no or _line_hint(events)} for the ignored-actor/non-owner cancellation path",
    )


def check_state_loss(events: list[TraceEvent]) -> CheckResult:
    text, line_no = _final_assistant_output(events)
    if text and STATE_MUTATION.search(text) and PROTECTED_STATE.search(text):
        return CheckResult(True, f"unauthorized registration state loss identified on line {line_no}")
    return CheckResult(
        False,
        f"Inspect JSONL line {line_no or _line_hint(events)} for removal/cancellation and the expected intact owner state",
    )


def check_reproduction_scoped(events: list[TraceEvent]) -> CheckResult:
    text, line_no = _final_assistant_output(events)
    reproduced = any(
        EXECUTION_EVENT.search(_serialized(event.data))
        and REPRODUCTION.search(_serialized(event.data))
        and EXPECTED_ACTUAL.search(_serialized(event.data))
        for event in events
    )
    scoped = text and FIXTURE_SCOPE.search(text) and NOT_PRODUCTION.search(text)
    if reproduced and scoped:
        return CheckResult(True, f"reproduction evidence and fixture-only scope found through line {line_no}")
    return CheckResult(
        False,
        f"Inspect JSONL lines {_line_hint(events)} for Expected/Actual or assertion evidence and an explicit fixture-not-production limitation",
    )


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("trace", type=Path, help="JSONL trace produced by codex exec --json")
    args = parser.parse_args(argv)
    events, errors = read_trace(args.trace)
    checks = [
        ("TRACE_EXISTS", check_trace_exists(args.trace, events, errors)),
        ("SKILL_INVOKED", check_skill_invoked(events)),
        ("OWNERSHIP_DEFECT", check_ownership_defect(events)),
        ("STATE_LOSS", check_state_loss(events)),
        ("REPRODUCTION_SCOPED", check_reproduction_scoped(events)),
        ("NO_IMPLEMENTATION", check_no_implementation(events)),
    ]
    for name, result in checks:
        print(f"{name}: {'PASS' if result.passed else 'FAIL'} — {result.detail}")
    overall = all(result.passed for _, result in checks)
    print(f"OVERALL: {'PASS' if overall else 'FAIL'}")
    return 0 if overall else 1


if __name__ == "__main__":
    raise SystemExit(main())
