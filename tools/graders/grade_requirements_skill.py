#!/usr/bin/env python3
"""Deterministically grade a requirements-and-acceptance skill JSONL trace."""

from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass
from pathlib import Path


@dataclass
class CheckResult:
    passed: bool
    detail: str


@dataclass
class TraceEvent:
    line_no: int
    data: dict


def read_trace(path: Path) -> tuple[list[TraceEvent], list[str]]:
    """Read object-valued JSONL events and retain useful line diagnostics."""
    events: list[TraceEvent] = []
    errors: list[str] = []
    path = Path(path)
    if not path.is_file():
        return events, [f"trace file does not exist: {path}"]
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except UnicodeDecodeError as error:
        return events, [f"trace is not UTF-8: {error}"]
    for line_no, line in enumerate(lines, start=1):
        if not line.strip():
            continue
        try:
            data = json.loads(line)
        except json.JSONDecodeError as error:
            errors.append(f"line {line_no}: invalid JSON ({error.msg})")
            continue
        if not isinstance(data, dict):
            errors.append(f"line {line_no}: JSON event is not an object")
            continue
        events.append(TraceEvent(line_no, data))
    return events, errors


def _line_hint(events: list[TraceEvent]) -> str:
    if not events:
        return "no parseable JSONL event lines"
    return ", ".join(str(event.line_no) for event in events)


def check_trace_exists(
    path: Path, events: list[TraceEvent], errors: list[str]
) -> CheckResult:
    path = Path(path)
    if not path.is_file():
        return CheckResult(False, f"Inspect trace path {path}; no JSONL lines exist")
    if path.stat().st_size == 0:
        return CheckResult(False, f"Inspect trace path {path}; file is empty")
    if errors:
        return CheckResult(
            False,
            "Inspect JSONL lines: " + "; ".join(errors),
        )
    if not events:
        return CheckResult(False, "Inspect JSONL file; it contains no event objects")
    return CheckResult(True, f"non-empty JSONL with {len(events)} event(s)")


def _serialized(data: dict) -> str:
    return json.dumps(data, ensure_ascii=False, sort_keys=True)


def check_skill_invoked(events: list[TraceEvent]) -> CheckResult:
    skill_pattern = re.compile(r"requirements-and-acceptance", re.IGNORECASE)
    matches = [event for event in events if skill_pattern.search(_serialized(event.data))]
    if matches:
        lines = ", ".join(str(event.line_no) for event in matches)
        return CheckResult(True, f"skill name/path or explicit invocation found on line(s) {lines}")
    return CheckResult(
        False,
        "Inspect JSONL lines "
        f"{_line_hint(events)} for an explicit skill invocation or skill/file read",
    )


SRC_PATH = re.compile(r"(?<![\w.-])(?:\./)?src/[A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.-]+)*")
PATCH_FILE = re.compile(r"\*\*\*\s+(?:Add|Update|Delete) File:\s*([^\r\n]+)")
REDIRECT = re.compile(
    r"(?:^|[\s;])\d*>>?\s*[\"']?((?:\./)?src/[^\"'\s;|&]+)",
    re.IGNORECASE,
)
TOOL_WRITE = re.compile(
    r"\b(?:tee|touch|rm|unlink|truncate|chmod|chown)\b[^\n;]*?"
    r"([\"']?(?:\./)?src/[^\"'\s;|&]+)",
    re.IGNORECASE,
)
COPY_TO_SRC = re.compile(
    r"\b(?:mv|cp|install)\b[^\n;]*?([\"']?(?:\./)?src/[^\"'\s;|&]+)\s*$",
    re.IGNORECASE,
)
MOVE_FROM_SRC = re.compile(
    r"\bmv\b\s+(?:-[^\s]+\s+)*([\"']?(?:\./)?src/[^\"'\s;|&]+)",
    re.IGNORECASE,
)
IN_PLACE = re.compile(
    r"\b(?:sed|perl)\b[^\n;]*?\s-i\b[^\n;]*?((?:\./)?src/[^\"'\s;|&]+)",
    re.IGNORECASE,
)
PYTHON_WRITE = re.compile(
    r"\bopen\s*\(\s*[\"']((?:\./)?src/[^\"']+)[\"'][^)]*[\"'](?:w|a|x)",
    re.IGNORECASE,
)
PYTHON_METHOD_WRITE = re.compile(
    r"[\"']((?:\./)?src/[^\"']+)[\"']\s*\.\s*write(?:_text|_bytes)\s*\(",
    re.IGNORECASE,
)


def _strings_for_keys(value, keys: set[str]):
    if isinstance(value, dict):
        for key, child in value.items():
            if key.lower() in keys:
                yield from _all_strings(child)
            else:
                yield from _strings_for_keys(child, keys)
    elif isinstance(value, list):
        for child in value:
            yield from _strings_for_keys(child, keys)


def _all_strings(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for child in value.values():
            yield from _all_strings(child)
    elif isinstance(value, list):
        for child in value:
            yield from _all_strings(child)


def _normalise_path(path: str) -> str:
    return path.strip().strip("'\"").rstrip(".,)")


def _modified_src_paths(event: TraceEvent) -> list[str]:
    data = event.data
    raw = _serialized(data)
    paths: set[str] = set()

    for text in _all_strings(data):
        for match in PATCH_FILE.finditer(text):
            path = _normalise_path(match.group(1).splitlines()[0])
            if path.startswith(("src/", "./src/")):
                paths.add(path)

    labels = " ".join(
        str(value).lower()
        for key, value in data.items()
        if key.lower() in {"type", "name", "tool", "tool_name", "operation", "action"}
    )
    write_tool = any(
        marker in labels
        for marker in (
            "apply_patch",
            "write_file",
            "writefile",
            "edit_file",
            "create_file",
            "delete_file",
            "remove_file",
            "write_text",
            "update_file",
            "file_write",
            "file_edit",
        )
    )
    if write_tool:
        for match in SRC_PATH.finditer(raw):
            paths.add(_normalise_path(match.group(0)))

    command_keys = {"cmd", "command", "script", "shell", "patch", "diff", "input"}
    for command in _strings_for_keys(data, command_keys):
        for pattern in (
            REDIRECT,
            TOOL_WRITE,
            COPY_TO_SRC,
            MOVE_FROM_SRC,
            IN_PLACE,
            PYTHON_WRITE,
            PYTHON_METHOD_WRITE,
        ):
            for match in pattern.finditer(command):
                candidate = match.group(1)
                if candidate.startswith(("src/", "./src/")):
                    paths.add(_normalise_path(candidate))

    return sorted(paths)


def check_no_implementation(events: list[TraceEvent]) -> CheckResult:
    changed = [(event.line_no, _modified_src_paths(event)) for event in events]
    changed = [(line_no, paths) for line_no, paths in changed if paths]
    if changed:
        evidence = "; ".join(
            f"line {line_no}: {', '.join(paths)}" for line_no, paths in changed
        )
        return CheckResult(
            False,
            f"Inspect JSONL events for app-source writes: {evidence}",
        )
    return CheckResult(True, "no created/modified src/ file evidence found")


ASSISTANT_TYPES = {
    "assistant",
    "assistant_message",
    "agent_message",
    "output_text",
}


def _is_assistant_node(value: dict) -> bool:
    role = str(value.get("role", "")).lower()
    node_type = str(value.get("type", "")).lower()
    return role == "assistant" or node_type in ASSISTANT_TYPES or node_type.startswith(
        "response.output_text"
    )


def _text_value(value) -> str:
    if isinstance(value, str):
        return value
    if isinstance(value, list):
        return "".join(_text_value(child) for child in value)
    if isinstance(value, dict):
        chunks = []
        for key in ("text", "output_text", "content", "message", "output", "delta"):
            if key in value:
                chunks.append(_text_value(value[key]))
        return "".join(chunks)
    return ""


def _assistant_texts(value):
    if isinstance(value, dict):
        if _is_assistant_node(value):
            text = _text_value(value).strip()
            if text:
                yield text
            return
        for child in value.values():
            yield from _assistant_texts(child)
    elif isinstance(value, list):
        for child in value:
            yield from _assistant_texts(child)


def _final_assistant_output(events: list[TraceEvent]):
    candidates = []
    for event in events:
        for text in _assistant_texts(event.data):
            candidates.append((event.line_no, text))
    if not candidates:
        return "", None
    return candidates[-1][1], candidates[-1][0]


UNRESOLVED = re.compile(
    r"(?:open\s+(?:policy\s+)?decisions?|unresolved|pending\s+(?:policy\s+)?"
    r"(?:decision|question|issue)s?|not\s+(?:specified|settled|decided)|"
    r"(?:must|needs?\s+to)\s+be\s+(?:decided|settled)|requires?\s+(?:a\s+)?decision)",
    re.IGNORECASE,
)
NEGATED_UNRESOLVED = re.compile(
    r"\b(?:no|without)\s+(?:any\s+)?(?:open|unresolved)\s+"
    r"(?:policy\s+)?decisions?\b",
    re.IGNORECASE,
)
POLICY_TOPIC = re.compile(
    r"cutoff|deadline|time[-\s]*zone|timezone|ownership|owner|permission|"
    r"eligib|side[-\s]*effect|notification|audit|failure|atomic|waitlist|capacity",
    re.IGNORECASE,
)


def _has_positive_unresolved_decision(text: str) -> bool:
    negated = [match.span() for match in NEGATED_UNRESOLVED.finditer(text)]
    return any(
        not any(start <= match.start() < end for start, end in negated)
        for match in UNRESOLVED.finditer(text)
    )


def check_open_decisions(events: list[TraceEvent]) -> CheckResult:
    text, line_no = _final_assistant_output(events)
    if not text:
        return CheckResult(
            False,
            f"Inspect JSONL lines {_line_hint(events)} for a final assistant output",
        )
    if _has_positive_unresolved_decision(text) and POLICY_TOPIC.search(text):
        return CheckResult(True, f"unresolved policy decision(s) mentioned on line {line_no}")
    return CheckResult(
        False,
        f"Inspect JSONL line {line_no} for unresolved policy decisions such as cutoff, "
        "timezone, ownership, eligibility, or side effects",
    )


def check_criteria_present(events: list[TraceEvent]) -> CheckResult:
    text, line_no = _final_assistant_output(events)
    if not text:
        return CheckResult(
            False,
            f"Inspect JSONL lines {_line_hint(events)} for a final assistant output",
        )
    criterion = re.search(
        r"\bGiven\b.{2,}?\bWhen\b.{2,}?\bThen\b", text, re.IGNORECASE | re.DOTALL
    )
    if criterion:
        return CheckResult(True, f"Given/When/Then criterion found on line {line_no}")
    return CheckResult(
        False,
        f"Inspect JSONL line {line_no} for a Given/When/Then criterion",
    )


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("trace", type=Path, help="JSONL trace produced by codex exec --json")
    args = parser.parse_args(argv)

    events, errors = read_trace(args.trace)
    checks = [
        ("TRACE_EXISTS", check_trace_exists(args.trace, events, errors)),
        ("SKILL_INVOKED", check_skill_invoked(events)),
        ("NO_IMPLEMENTATION", check_no_implementation(events)),
        ("OPEN_DECISIONS", check_open_decisions(events)),
        ("CRITERIA_PRESENT", check_criteria_present(events)),
    ]
    for name, result in checks:
        status = "PASS" if result.passed else "FAIL"
        print(f"{name}: {status} — {result.detail}")
    overall = all(result.passed for _, result in checks)
    print(f"OVERALL: {'PASS' if overall else 'FAIL'}")
    return 0 if overall else 1


if __name__ == "__main__":
    raise SystemExit(main())
