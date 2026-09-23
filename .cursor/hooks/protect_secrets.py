#!/usr/bin/env python3
"""Deny reads of likely secret files; allow safe env templates."""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from audit_lib import audit, emit, read_stdin_json  # noqa: E402


ALLOWED_NAMES = {".env.example", ".env.template", ".env.sample"}
DENIED_NAMES = {".env", ".env.local", ".env.production", ".env.development"}
DENIED_SUFFIXES = (".pem", ".p12", ".pfx")
DENIED_KEY_NAMES = {"id_rsa", "id_dsa", "id_ecdsa", "id_ed25519", "credentials.json"}
DENIED_DIR_MARKERS = ("/.ssh/", "/.aws/", "/.gnupg/")


def _path_from(payload: dict) -> str:
    for key in ("file_path", "path", "uri", "filePath"):
        value = payload.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
    # Nested shapes
    for key in ("file", "input", "arguments"):
        nested = payload.get(key)
        if isinstance(nested, dict):
            found = _path_from(nested)
            if found:
                return found
    return ""


def classify(path: str) -> tuple[str, str]:
    if not path:
        return "allow", "No path provided."
    normalized = path.replace("\\", "/")
    name = Path(normalized).name
    lower_name = name.lower()

    if lower_name in ALLOWED_NAMES:
        return "allow", "Safe env template."
    if lower_name in DENIED_NAMES:
        return "deny", "Secret env file reads are denied."
    if lower_name in DENIED_KEY_NAMES:
        return "deny", "Credential / private-key file reads are denied."
    if any(lower_name.endswith(suffix) for suffix in DENIED_SUFFIXES):
        return "deny", "Certificate / key material reads are denied."
    if any(marker in normalized for marker in DENIED_DIR_MARKERS):
        return "deny", "Credential directory access is denied."
    return "allow", "Non-secret file."


def main() -> int:
    payload = read_stdin_json()
    if payload.get("_parse_error"):
        audit("beforeReadFile", "deny", "Malformed hook input.", tool="read")
        emit({"permission": "deny", "user_message": "Secret-file hook received malformed input."})
        return 0

    path = _path_from(payload)
    decision, reason = classify(path)
    audit("beforeReadFile", decision, reason, tool="read", path=path or None)
    emit(
        {
            "permission": decision,
            "user_message": reason,
            "agent_message": reason,
        }
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
