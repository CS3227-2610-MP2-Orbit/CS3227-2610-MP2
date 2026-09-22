import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from grade_requirements_skill import (
    check_criteria_present,
    check_no_implementation,
    check_open_decisions,
    check_skill_invoked,
    check_trace_exists,
    read_trace,
)


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "tools" / "graders" / "grade_requirements_skill.py"


def event(payload):
    return json.dumps(payload)


class RequirementsSkillGraderTest(unittest.TestCase):
    def write_trace(self, lines):
        trace = tempfile.NamedTemporaryFile(
            mode="w", encoding="utf-8", suffix=".jsonl", delete=False
        )
        self.addCleanup(lambda: Path(trace.name).unlink(missing_ok=True))
        trace.write("\n".join(lines) + "\n")
        trace.close()
        return Path(trace.name)

    def good_trace(self):
        return self.write_trace(
            [
                event(
                    {
                        "type": "user_message",
                        "text": "Use $requirements-and-acceptance only.",
                    }
                ),
                event(
                    {
                        "type": "function_call",
                        "name": "exec_command",
                        "arguments": {
                            "cmd": "sed -n '1,260p' .agents/skills/requirements-and-acceptance/SKILL.md"
                        },
                    }
                ),
                event(
                    {
                        "type": "item.completed",
                        "item": {
                            "type": "agent_message",
                            "text": (
                                "Open decisions remain: cutoff, timezone, ownership, "
                                "eligibility, and side effects.\n"
                                "Given an eligible registration before the cutoff\n"
                                "When the attendee requests cancellation\n"
                                "Then the registration is cancelled."
                            ),
                        },
                    }
                ),
            ]
        )

    def test_good_trace_passes_all_checks(self):
        trace = self.good_trace()
        events, errors = read_trace(trace)

        self.assertTrue(check_trace_exists(trace, events, errors).passed)
        self.assertTrue(check_skill_invoked(events).passed)
        self.assertTrue(check_no_implementation(events).passed)
        self.assertTrue(check_open_decisions(events).passed)
        self.assertTrue(check_criteria_present(events).passed)

    def test_src_patch_fails_no_implementation(self):
        trace = self.write_trace(
            [
                event(
                    {
                        "type": "function_call",
                        "name": "apply_patch",
                        "arguments": "*** Update File: src/main/java/App.java\n@@",
                    }
                )
            ]
        )
        events, errors = read_trace(trace)

        result = check_no_implementation(events)

        self.assertFalse(result.passed)
        self.assertIn("line 1", result.detail)

    def test_src_write_command_fails_no_implementation(self):
        trace = self.write_trace(
            [
                event(
                    {
                        "type": "function_call",
                        "name": "exec_command",
                        "arguments": {"cmd": "printf code > src/main/java/App.java"},
                    }
                )
            ]
        )
        events, errors = read_trace(trace)

        result = check_no_implementation(events)

        self.assertFalse(result.passed)
        self.assertIn("src/main/java/App.java", result.detail)

    def test_invalid_json_reports_line_for_trace_check(self):
        trace = self.write_trace(["not json"])
        events, errors = read_trace(trace)

        result = check_trace_exists(trace, events, errors)

        self.assertFalse(result.passed)
        self.assertIn("line 1", result.detail)

    def test_closed_decisions_do_not_pass_open_decisions_check(self):
        trace = self.write_trace(
            [
                event(
                    {
                        "type": "item.completed",
                        "item": {
                            "type": "agent_message",
                            "text": "No open decisions remain; the cutoff is settled.",
                        },
                    }
                )
            ]
        )
        events, errors = read_trace(trace)

        result = check_open_decisions(events)

        self.assertFalse(result.passed)

    def test_cli_prints_checks_and_returns_failure_for_missing_criteria(self):
        trace = self.write_trace(
            [
                event(
                    {
                        "type": "user_message",
                        "text": "Use $requirements-and-acceptance.",
                    }
                ),
                event(
                    {
                        "type": "item.completed",
                        "item": {
                            "type": "agent_message",
                            "text": "Open decisions remain about the cutoff.",
                        },
                    }
                ),
            ]
        )

        completed = subprocess.run(
            [sys.executable, str(SCRIPT), str(trace)],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=False,
        )

        self.assertEqual(completed.returncode, 1)
        self.assertIn("TRACE_EXISTS: PASS", completed.stdout)
        self.assertIn("SKILL_INVOKED: PASS", completed.stdout)
        self.assertIn("NO_IMPLEMENTATION: PASS", completed.stdout)
        self.assertIn("OPEN_DECISIONS: PASS", completed.stdout)
        self.assertIn("CRITERIA_PRESENT: FAIL", completed.stdout)
        self.assertIn("Inspect JSONL", completed.stdout)

    def test_cli_returns_pass_for_complete_trace(self):
        completed = subprocess.run(
            [sys.executable, str(SCRIPT), str(self.good_trace())],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=False,
        )

        self.assertEqual(completed.returncode, 0)
        self.assertIn("OVERALL: PASS", completed.stdout)


if __name__ == "__main__":
    unittest.main()
