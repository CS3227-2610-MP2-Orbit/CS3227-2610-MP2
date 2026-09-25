#!/usr/bin/env python3
"""Run focused Gradle verification at most once per agent stop."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from audit_lib import (  # noqa: E402
    ROOT,
    audit,
    emit,
    read_stdin_json,
    verify_marker_exists,
    write_verify_marker,
)


VERIFY_CMD = [
    "./gradlew",
    "test",
    "--tests",
    "seedu.eventmanager.common.*",
    "--tests",
    "seedu.eventmanager.event.*",
    "--tests",
    "seedu.eventmanager.MainTest",
    "--no-daemon",
]


def main() -> int:
    read_stdin_json()  # consume stdin; ignore body for once-per-stop gate

    if verify_marker_exists():
        audit(
            "stop",
            "allow",
            "Verification already recorded for this stop cycle.",
            tool="verify",
            command=" ".join(VERIFY_CMD),
            exit_status=0,
        )
        emit({})
        return 0

    try:
        completed = subprocess.run(
            VERIFY_CMD,
            cwd=str(ROOT),
            capture_output=True,
            text=True,
            check=False,
        )
    except OSError as error:
        audit("stop", "deny", f"Could not run verification: {error}", tool="verify")
        emit(
            {
                "followup_message": (
                    "Stop verification could not run ./gradlew. "
                    f"OS error: {error}. Do not claim the task is complete."
                )
            }
        )
        return 0

    write_verify_marker()
    cmd = " ".join(VERIFY_CMD)
    if completed.returncode == 0:
        audit("stop", "allow", "Verification passed.", tool="verify", command=cmd, exit_status=0)
        emit({})
        return 0

    tail = (completed.stdout or "")[-800:]
    audit(
        "stop",
        "deny",
        "Verification failed.",
        tool="verify",
        command=cmd,
        exit_status=completed.returncode,
    )
    emit(
        {
            "followup_message": (
                "Stop verification failed. "
                f"Command `{cmd}` exited {completed.returncode}. "
                "Do not claim successful completion. "
                f"Output tail:\n{tail}"
            )
        }
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
