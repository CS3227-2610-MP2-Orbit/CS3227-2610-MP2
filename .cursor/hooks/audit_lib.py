#!/usr/bin/env python3
"""Shared JSONL audit helper for project Cursor hooks (stdlib only)."""

from __future__ import annotations

import json
import os
import re
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[2]
AUDIT_PATH = Path(
    os.environ.get("MP2_HOOK_AUDIT_PATH", str(ROOT / "evals" / "artifacts" / "hook-events.jsonl"))
)
MARKER_PATH = Path(
    os.environ.get("MP2_HOOK_VERIFY_MARKER", str(ROOT / "evals" / "artifacts" / ".verify-once"))
)


def repo_root() -> Path:
    return ROOT


def read_stdin_json() -> dict[str, Any]:
    import sys

    raw = sys.stdin.read()
    if not raw.strip():
        return {}
    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        return {"_parse_error": True, "_raw_len": len(raw)}
    return data if isinstance(data, dict) else {"_parse_error": True}


def emit(payload: dict[str, Any]) -> None:
    import sys

    sys.stdout.write(json.dumps(payload, ensure_ascii=False))
    sys.stdout.flush()


def _safe_command(command: str | None) -> str | None:
    if not command:
        return None
    # Redact obvious secret assignments; keep structure for audit.
    redacted = re.sub(
        r"(?i)(password|token|secret|api[_-]?key)\s*=\s*\S+",
        r"\1=<redacted>",
        command,
    )
    if len(redacted) > 240:
        redacted = redacted[:237] + "..."
    return redacted


def _safe_path(path: str | None) -> str | None:
    if not path:
        return None
    name = Path(path).name
    lowered = name.lower()
    if lowered in {".env", ".env.local"} or lowered.endswith(
        (".pem", ".key", "id_rsa", "id_ed25519", "credentials.json")
    ):
        return f"<secret-file:{name}>"
    try:
        resolved = Path(path).resolve()
        if ROOT in resolved.parents or resolved == ROOT:
            return str(resolved.relative_to(ROOT))
    except OSError:
        pass
    return name


def audit(
    event: str,
    decision: str,
    reason: str,
    *,
    tool: str | None = None,
    command: str | None = None,
    path: str | None = None,
    exit_status: int | None = None,
) -> None:
    AUDIT_PATH.parent.mkdir(parents=True, exist_ok=True)
    record = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "event": event,
        "tool": tool,
        "command": _safe_command(command),
        "path": _safe_path(path),
        "decision": decision,
        "reason": reason,
        "exit_status": exit_status,
    }
    with AUDIT_PATH.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(record, ensure_ascii=False) + "\n")


def clear_verify_marker() -> None:
    if MARKER_PATH.exists():
        MARKER_PATH.unlink()


def verify_marker_exists() -> bool:
    return MARKER_PATH.exists()


def write_verify_marker() -> None:
    MARKER_PATH.parent.mkdir(parents=True, exist_ok=True)
    MARKER_PATH.write_text("verified\n", encoding="utf-8")
