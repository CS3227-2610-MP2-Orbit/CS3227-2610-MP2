#!/usr/bin/env python3
"""Deterministically grade a test-driven-implementation skill JSONL trace."""

from __future__ import annotations

import argparse
import re
from pathlib import Path

from grade_requirements_skill import (
    PATCH_FILE,
    CheckResult,
    TraceEvent,
    _all_strings,
    _line_hint,
    _serialized,
    check_trace_exists,
    read_trace,
)


SKILL = re.compile(r"test-driven-implementation", re.IGNORECASE)
ASSERTION_FAILURE = re.compile(
    r"AssertionError|assertion\s+(?:failed|failure)|failing\s+assertion|"
    r"duplicate\s+should\s+(?:return|be)\s+false",
    re.IGNORECASE,
)
NONZERO = re.compile(
    r'"exit_code"\s*:\s*[1-9]\d*|\bexit(?:ed|\s+code)?\s*[=:]?\s*[1-9]\d*|'
    r"\bFAILED\b",
    re.IGNORECASE,
)
EXECUTION_EVENT = re.compile(r"command_execution|exec_command|shell_command", re.IGNORECASE)
PASSING = re.compile(
    r'"exit_code"\s*:\s*0|\bexit(?:ed|\s+code)?\s*[=:]?\s*0\b|'
    r"\b\d+\s+tests?\s+passed\b|\btests?\s+passed\b",
    re.IGNORECASE,
)
MEANINGFUL_OUTCOME = re.compile(
    r"duplicate[\s\S]{0,100}(?:false|reject)|(?:false|reject)[\s\S]{0,100}duplicate",
    re.IGNORECASE,
)
UNCHANGED_COUNT = re.compile(
    r"(?:count|size)[\s\S]{0,60}(?:unchanged|one|1)|"
    r"(?:unchanged|one|1)[\s\S]{0,60}(?:count|size)",
    re.IGNORECASE,
)
JAVA_PATH = re.compile(r"(?<![\w.-])(?:\./)?[A-Za-z0-9_./-]+\.java\b")
WRITE_TOOL = re.compile(
    r"apply_patch|write_file|writefile|edit_file|create_file|update_file|file_write|file_edit",
    re.IGNORECASE,
)


def _matching_lines(events: list[TraceEvent], *patterns: re.Pattern) -> list[int]:
    return [
        event.line_no
        for event in events
        if all(pattern.search(_serialized(event.data)) for pattern in patterns)
    ]


def _red_lines(events: list[TraceEvent]) -> list[int]:
    return _matching_lines(events, EXECUTION_EVENT, ASSERTION_FAILURE, NONZERO)


def _is_implementation_path(path: str) -> bool:
    name = Path(path.strip().strip("'\"")).name.lower()
    return name.endswith(".java") and "test" not in name and "repro" not in name


def _implementation_changes(events: list[TraceEvent]) -> list[tuple[int, str]]:
    red_lines = _red_lines(events)
    if not red_lines:
        return []
    first_red = min(red_lines)
    changes = []
    for event in events:
        if event.line_no <= first_red:
            continue
        raw = _serialized(event.data)
        for text in _all_strings(event.data):
            for match in PATCH_FILE.finditer(text):
                path = match.group(1).splitlines()[0].strip()
                if _is_implementation_path(path):
                    changes.append((event.line_no, path))
        if WRITE_TOOL.search(raw):
            for match in JAVA_PATH.finditer(raw):
                path = match.group(0)
                if _is_implementation_path(path):
                    changes.append((event.line_no, path))
    return sorted(set(changes))


def check_skill_invoked(events: list[TraceEvent]) -> CheckResult:
    lines = [event.line_no for event in events if SKILL.search(_serialized(event.data))]
    if lines:
        return CheckResult(True, f"skill invocation/evidence found on line(s) {', '.join(map(str, lines))}")
    return CheckResult(
        False,
        f"Inspect JSONL lines {_line_hint(events)} for test-driven-implementation invocation/evidence",
    )


def check_behavioral_red(events: list[TraceEvent]) -> CheckResult:
    lines = _red_lines(events)
    if lines:
        return CheckResult(True, f"non-zero behavioral assertion failure found on line(s) {', '.join(map(str, lines))}")
    return CheckResult(
        False,
        f"Inspect JSONL lines {_line_hint(events)} for a non-zero assertion failure; compilation/setup errors do not count",
    )


def check_implementation_changed(events: list[TraceEvent]) -> CheckResult:
    changes = _implementation_changes(events)
    if changes:
        evidence = "; ".join(f"line {line}: {path}" for line, path in changes)
        return CheckResult(True, f"implementation edit after red: {evidence}")
    return CheckResult(
        False,
        f"Inspect JSONL lines {_line_hint(events)} for a non-test Java implementation edit after the behavioral red result",
    )


def check_green_verified(events: list[TraceEvent]) -> CheckResult:
    changes = _implementation_changes(events)
    if not changes:
        return CheckResult(
            False,
            f"Inspect JSONL lines {_line_hint(events)}; no implementation edit establishes the start of the green phase",
        )
    last_change = max(line for line, _ in changes)
    lines = [
        event.line_no
        for event in events
        if event.line_no > last_change
        and EXECUTION_EVENT.search(_serialized(event.data))
        and PASSING.search(_serialized(event.data))
        and MEANINGFUL_OUTCOME.search(_serialized(event.data))
        and UNCHANGED_COUNT.search(_serialized(event.data))
    ]
    if lines:
        return CheckResult(True, f"passing rerun with duplicate/count assertions found on line(s) {', '.join(map(str, lines))}")
    return CheckResult(
        False,
        f"Inspect JSONL lines after {last_change} for an exit-0/passing rerun asserting duplicate rejection and unchanged count",
    )


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("trace", type=Path, help="JSONL trace produced by codex exec --json")
    args = parser.parse_args(argv)
    events, errors = read_trace(args.trace)
    checks = [
        ("TRACE_EXISTS", check_trace_exists(args.trace, events, errors)),
        ("SKILL_INVOKED", check_skill_invoked(events)),
        ("BEHAVIORAL_RED", check_behavioral_red(events)),
        ("IMPLEMENTATION_CHANGED", check_implementation_changed(events)),
        ("GREEN_VERIFIED", check_green_verified(events)),
    ]
    for name, result in checks:
        print(f"{name}: {'PASS' if result.passed else 'FAIL'} — {result.detail}")
    overall = all(result.passed for _, result in checks)
    print(f"OVERALL: {'PASS' if overall else 'FAIL'}")
    return 0 if overall else 1


if __name__ == "__main__":
    raise SystemExit(main())
