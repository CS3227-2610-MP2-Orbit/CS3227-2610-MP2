#!/usr/bin/env python3
"""Deterministic tests for MP2 Cursor hook scripts (stdlib only)."""

from __future__ import annotations

import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


HOOKS = Path(__file__).resolve().parents[1]
ROOT = HOOKS.parents[1]
PYTHON = sys.executable


def run_hook(script: str, payload: dict, env: dict | None = None) -> subprocess.CompletedProcess:
    merged = os.environ.copy()
    if env:
        merged.update(env)
    return subprocess.run(
        [PYTHON, str(HOOKS / script)],
        input=json.dumps(payload),
        text=True,
        capture_output=True,
        cwd=str(ROOT),
        env=merged,
        check=False,
    )


class HookTests(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.audit = Path(self.tmp.name) / "hook-events.jsonl"
        self.marker = Path(self.tmp.name) / ".verify-once"
        self.env = {
            "MP2_HOOK_AUDIT_PATH": str(self.audit),
            "MP2_HOOK_VERIFY_MARKER": str(self.marker),
        }

    def _audit_lines(self) -> list[dict]:
        if not self.audit.exists():
            return []
        return [json.loads(line) for line in self.audit.read_text(encoding="utf-8").splitlines() if line.strip()]

    def test_subagent_denied(self) -> None:
        result = run_hook("deny_subagent.py", {"subagent_type": "explore"}, self.env)
        self.assertEqual(result.returncode, 0)
        body = json.loads(result.stdout)
        self.assertEqual(body["permission"], "deny")
        self.assertIn("single-agent", body["user_message"])
        lines = self._audit_lines()
        self.assertEqual(lines[-1]["decision"], "deny")
        self.assertNotIn("prompt", lines[-1])

    def test_gradle_test_allowed(self) -> None:
        result = run_hook(
            "guard_shell.py",
            {"command": "./gradlew test --tests seedu.eventmanager.MainTest"},
            self.env,
        )
        body = json.loads(result.stdout)
        self.assertEqual(body["permission"], "allow")

    def test_rm_rf_denied(self) -> None:
        result = run_hook("guard_shell.py", {"command": "rm -rf build"}, self.env)
        body = json.loads(result.stdout)
        self.assertEqual(body["permission"], "deny")

    def test_pip_install_asks(self) -> None:
        result = run_hook("guard_shell.py", {"command": "pip install requests"}, self.env)
        body = json.loads(result.stdout)
        self.assertEqual(body["permission"], "ask")

    def test_env_read_denied(self) -> None:
        result = run_hook("protect_secrets.py", {"file_path": str(ROOT / ".env")}, self.env)
        body = json.loads(result.stdout)
        self.assertEqual(body["permission"], "deny")
        self.assertIn("<secret-file:.env>", json.dumps(self._audit_lines()[-1]))

    def test_env_example_allowed(self) -> None:
        result = run_hook(
            "protect_secrets.py",
            {"file_path": str(ROOT / ".env.example")},
            self.env,
        )
        body = json.loads(result.stdout)
        self.assertEqual(body["permission"], "allow")

    def test_malformed_shell_input(self) -> None:
        merged = os.environ.copy()
        merged.update(self.env)
        result = subprocess.run(
            [PYTHON, str(HOOKS / "guard_shell.py")],
            input="{not-json",
            text=True,
            capture_output=True,
            cwd=str(ROOT),
            env=merged,
            check=False,
        )
        body = json.loads(result.stdout)
        self.assertEqual(body["permission"], "deny")

    def test_audit_is_jsonl(self) -> None:
        run_hook("deny_subagent.py", {"subagent_type": "generalPurpose"}, self.env)
        run_hook("guard_shell.py", {"command": "git status"}, self.env)
        for line in self.audit.read_text(encoding="utf-8").splitlines():
            json.loads(line)

    def test_verify_once_per_stop(self) -> None:
        # First call writes marker even if gradle fails in sandbox; we stub by
        # pre-creating marker and ensuring second call does not spawn work twice.
        self.marker.write_text("verified\n", encoding="utf-8")
        result = run_hook("verify_on_stop.py", {"status": "completed"}, self.env)
        self.assertEqual(result.returncode, 0)
        lines = self._audit_lines()
        self.assertTrue(any("already recorded" in line.get("reason", "") for line in lines))

    def test_hooks_are_command_not_prompt(self) -> None:
        config = json.loads((ROOT / ".cursor" / "hooks.json").read_text(encoding="utf-8"))
        for entries in config["hooks"].values():
            for entry in entries:
                self.assertNotEqual(entry.get("type"), "prompt")
                self.assertIn("command", entry)
                self.assertTrue(entry["command"].startswith("python3 "))


if __name__ == "__main__":
    unittest.main()
