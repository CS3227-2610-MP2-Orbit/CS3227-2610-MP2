#!/usr/bin/env python3
"""Deny all Cursor subagent starts (MP2 single-agent requirement)."""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from audit_lib import audit, emit, read_stdin_json  # noqa: E402


def main() -> int:
    payload = read_stdin_json()
    subagent = ""
    if isinstance(payload, dict):
        subagent = str(
            payload.get("subagent_type")
            or payload.get("type")
            or payload.get("agent")
            or ""
        )
    reason = "MP2 requires a single-agent workflow."
    audit(
        "subagentStart",
        "deny",
        reason,
        tool="subagent",
        command=subagent or None,
    )
    emit(
        {
            "permission": "deny",
            "user_message": reason,
        }
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
