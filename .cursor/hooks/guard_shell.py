#!/usr/bin/env python3
"""Gate shell commands for MP2 local development (parse only; never exec input)."""

from __future__ import annotations

import re
import shlex
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from audit_lib import ROOT, audit, emit, read_stdin_json  # noqa: E402


DENY_PATTERNS = [
    (re.compile(r"\brm\s+-[^\n]*rf\b|\brm\s+-[^\n]*fr\b"), "Destructive recursive delete is blocked."),
    (re.compile(r"\bgit\s+reset\s+--hard\b"), "git reset --hard is blocked."),
    (re.compile(r"\bgit\s+clean\b"), "git clean is blocked."),
    (re.compile(r"\bgit\s+push\b[^\n]*\s--force\b|\bgit\s+push\b[^\n]*\s-f\b"), "Force push is blocked."),
    (re.compile(r"\bgit\s+push\b[^\n]*\s(origin\s+)?(main|master)\b"), "Push to main/master requires human approval outside this hook; blocked by default."),
    (re.compile(r"\bDROP\s+(DATABASE|TABLE|SCHEMA)\b", re.I), "Destructive SQL DDL is blocked."),
    (re.compile(r"\bTRUNCATE\b", re.I), "TRUNCATE is blocked."),
    (re.compile(r"\bflyway\b[^\n]*\bclean\b", re.I), "flyway clean is blocked."),
]

ASK_PATTERNS = [
    (re.compile(r"\b(curl|wget|nc|ncat|ssh)\b"), "Network / remote shell tool needs human approval."),
    (re.compile(r"\bnpm\s+i(nstall)?\b|\bpip\s+install\b|\bbrew\s+install\b"), "Dependency installation needs human approval."),
    (re.compile(r"--refresh-dependencies"), "Gradle dependency refresh needs human approval."),
]

ALLOW_PREFIXES = (
    "git status",
    "git diff",
    "git log",
    "git branch",
    "git show",
    "git rev-parse",
    "ls",
    "pwd",
    "rg ",
    "grep ",
    "find ",
    "cat ",
    "head ",
    "tail ",
    "sed -n",
    "wc ",
    "python3 ",
    "./gradlew",
    "gradlew",
)


def _command_from(payload: dict) -> str:
    for key in ("command", "cmd", "shell_command"):
        value = payload.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
    return ""


def _escapes_repo(command: str) -> bool:
    # Rough path escape: absolute paths outside repo or ../ climbing far.
    if re.search(r"(^|[\s\"'])/(etc|Users|home|tmp|var|private)/", command) and str(ROOT) not in command:
        # Allow gradle caches references only if clearly gradle-related — still ask/deny absolute elsewhere
        if "./gradlew" in command or "gradlew" in command:
            return False
        if re.search(r"\b(cat|less|rg|grep|head|tail|sed)\b", command):
            return True
    if re.search(r"(^|[\s])\.\./\.\./\.\.", command):
        return True
    return False


def classify(command: str) -> tuple[str, str]:
    if not command:
        return "allow", "Empty command."
    if _escapes_repo(command):
        return "deny", "Command appears to access paths outside the project."

    for pattern, reason in DENY_PATTERNS:
        if pattern.search(command):
            return "deny", reason
    for pattern, reason in ASK_PATTERNS:
        if pattern.search(command):
            return "ask", reason

    lowered = command.lstrip()
    for prefix in ALLOW_PREFIXES:
        if lowered.startswith(prefix) or f"&& {prefix}" in lowered or f"; {prefix}" in lowered:
            return "allow", "Routine local development command."

    # Safe git checkout of a branch name only (not git checkout -- file)
    if re.match(r"^git\s+checkout\s+(?!--)[\w./-]+$", lowered):
        return "allow", "Branch checkout."
    if re.search(r"\bgit\s+checkout\s+--\b", lowered):
        return "deny", "git checkout -- (discard working tree) is blocked."

    # Default: ask rather than silently allow unknown commands
    return "ask", "Unrecognized command; obtain human approval."


def main() -> int:
    payload = read_stdin_json()
    if payload.get("_parse_error"):
        audit("beforeShellExecution", "deny", "Malformed hook input.", tool="shell")
        emit(
            {
                "permission": "deny",
                "user_message": "Shell hook received malformed input.",
                "agent_message": "Malformed beforeShellExecution payload; command blocked.",
            }
        )
        return 0

    command = _command_from(payload)
    # Touch shlex only to ensure we never eval; ignore parse failures.
    try:
        shlex.split(command)
    except ValueError:
        pass

    decision, reason = classify(command)
    audit(
        "beforeShellExecution",
        decision,
        reason,
        tool="shell",
        command=command,
    )
    body = {
        "permission": decision,
        "user_message": reason,
        "agent_message": reason,
    }
    if decision == "ask":
        body["user_message"] = reason + " Approve in the UI if this is intentional."
    emit(body)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
