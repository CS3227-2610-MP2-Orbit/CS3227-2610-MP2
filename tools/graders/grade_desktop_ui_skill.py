#!/usr/bin/env python3
"""Deterministically grade a desktop-ui-polish skill JSONL trace (case U1)."""

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


SKILL = re.compile(r"desktop-ui-polish", re.IGNORECASE)
LAYOUT = re.compile(
    r"dual\s*chrome|double\s*chrome|outer\s*header|sidebar|padding|truncat|"
    r"squish|width|column\s*constraint|GridPane|minWidth|prefWidth|Priority\.ALWAYS|"
    r"fill\s+available|empty\s+margin",
    re.IGNORECASE,
)
TOKENS = re.compile(
    r"#172033|#f7f9fc|#e2e8f0|#2563eb|visual\s+token|shared\s+shell|"
    r"Venue\s+Administrator|card(?:s)?\b",
    re.IGNORECASE,
)
CHECKLIST = re.compile(
    r"before\s*/\s*after|checklist|dual\s*chrome|label\s*truncat|"
    r"primary\s+action|non-?goals?",
    re.IGNORECASE,
)
SCOPE = re.compile(
    r"club\s*crud|add[-\s]?club|club\s+director(?:y)?|do\s+not\s+(?:add|invent)|"
    r"out\s+of\s+scope|non-?goal|proposal\s+only|unresolved",
    re.IGNORECASE,
)
INVENTED_CLUB_UI = re.compile(
    r"(?:implement|add|build|create)\s+(?:a\s+)?(?:full\s+)?(?:club\s+(?:crud|management|directory)|"
    r"add[-\s]?club\s+(?:button|ui|screen))",
    re.IGNORECASE,
)


def check_skill_invoked(events: list[TraceEvent]) -> CheckResult:
    lines = [event.line_no for event in events if SKILL.search(_serialized(event.data))]
    if lines:
        return CheckResult(
            True, f"skill invocation/evidence found on line(s) {', '.join(map(str, lines))}"
        )
    return CheckResult(
        False,
        f"Inspect JSONL lines {_line_hint(events)} for desktop-ui-polish invocation/evidence",
    )


def check_layout_findings(events: list[TraceEvent]) -> CheckResult:
    text, line_no = _final_assistant_output(events)
    if text and LAYOUT.search(text):
        return CheckResult(True, f"layout / chrome / width findings on line {line_no}")
    return CheckResult(
        False,
        f"Inspect JSONL line {line_no or _line_hint(events)} for dual-chrome, "
        "width-fill, truncation, or GridPane/column-constraint findings",
    )


def check_visual_tokens(events: list[TraceEvent]) -> CheckResult:
    text, line_no = _final_assistant_output(events)
    if text and TOKENS.search(text):
        return CheckResult(True, f"shared shell / visual token guidance on line {line_no}")
    return CheckResult(
        False,
        f"Inspect JSONL line {line_no or _line_hint(events)} for shared shell tokens "
        "or Venue-aligned styling guidance",
    )


def check_checklist(events: list[TraceEvent]) -> CheckResult:
    text, line_no = _final_assistant_output(events)
    if text and CHECKLIST.search(text):
        return CheckResult(True, f"before/after or polish checklist signal on line {line_no}")
    return CheckResult(
        False,
        f"Inspect JSONL line {line_no or _line_hint(events)} for a before/after "
        "or layout checklist covering chrome, truncation, and primary actions",
    )


def check_scope_respected(events: list[TraceEvent]) -> CheckResult:
    text, line_no = _final_assistant_output(events)
    if not text:
        return CheckResult(
            False,
            f"Inspect JSONL lines {_line_hint(events)} for a final assistant output",
        )
    invents = INVENTED_CLUB_UI.search(text)
    scopes = SCOPE.search(text)
    if invents and not scopes:
        return CheckResult(
            False,
            f"Inspect JSONL line {line_no}: appears to invent club CRUD/UI without "
            "marking it out of scope or proposal-only",
        )
    if scopes:
        return CheckResult(True, f"scope / non-goal / proposal boundary on line {line_no}")
    return CheckResult(
        False,
        f"Inspect JSONL line {line_no} for an explicit non-goal (e.g. no club CRUD) "
        "or unresolved/proposal marking",
    )


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "trace",
        type=Path,
        help="JSONL trace produced by codex exec --json (U1 review-only preferred)",
    )
    args = parser.parse_args(argv)

    events, errors = read_trace(args.trace)
    checks = [
        ("TRACE_EXISTS", check_trace_exists(args.trace, events, errors)),
        ("SKILL_INVOKED", check_skill_invoked(events)),
        ("NO_IMPLEMENTATION", check_no_implementation(events)),
        ("LAYOUT_FINDINGS", check_layout_findings(events)),
        ("VISUAL_TOKENS", check_visual_tokens(events)),
        ("CHECKLIST", check_checklist(events)),
        ("SCOPE_RESPECTED", check_scope_respected(events)),
    ]
    for name, result in checks:
        status = "PASS" if result.passed else "FAIL"
        print(f"{name}: {status} — {result.detail}")
    overall = all(result.passed for _, result in checks)
    print(f"OVERALL: {'PASS' if overall else 'FAIL'}")
    return 0 if overall else 1


if __name__ == "__main__":
    raise SystemExit(main())
