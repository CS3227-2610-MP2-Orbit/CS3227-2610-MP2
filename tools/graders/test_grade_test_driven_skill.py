import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from grade_requirements_skill import check_trace_exists, read_trace
from grade_test_driven_skill import (
    check_behavioral_red,
    check_green_verified,
    check_implementation_changed,
    check_skill_invoked,
)


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "tools" / "graders" / "grade_test_driven_skill.py"


def event(payload):
    return json.dumps(payload)


class TestDrivenSkillGraderTest(unittest.TestCase):
    def write_trace(self, events):
        trace = tempfile.NamedTemporaryFile(
            mode="w", encoding="utf-8", suffix=".jsonl", delete=False
        )
        self.addCleanup(lambda: Path(trace.name).unlink(missing_ok=True))
        trace.write("\n".join(event(item) for item in events) + "\n")
        trace.close()
        return Path(trace.name)

    def good_trace(self):
        return self.write_trace(
            [
                {
                    "type": "user_message",
                    "text": "Use $test-driven-implementation for T1.",
                },
                {
                    "type": "item.completed",
                    "item": {
                        "type": "command_execution",
                        "command": "javac RegistrationBook.java RegistrationBookTest.java && java RegistrationBookTest",
                        "aggregated_output": "AssertionError: duplicate should return false",
                        "exit_code": 1,
                    },
                },
                {
                    "type": "function_call",
                    "name": "apply_patch",
                    "arguments": "*** Update File: RegistrationBook.java\n@@",
                },
                {
                    "type": "item.completed",
                    "item": {
                        "type": "command_execution",
                        "command": "javac RegistrationBook.java RegistrationBookTest.java && java RegistrationBookTest",
                        "aggregated_output": "3 tests passed; duplicate returned false; count unchanged",
                        "exit_code": 0,
                    },
                },
                {
                    "type": "item.completed",
                    "item": {
                        "type": "agent_message",
                        "text": (
                            "Red: the compiling test failed with AssertionError: duplicate "
                            "should return false, exit 1. The minimal implementation was "
                            "changed. Green: 3 tests passed, exit 0; duplicate returned "
                            "false and the count remained unchanged."
                        ),
                    },
                },
            ]
        )

    def test_complete_t1_trace_passes_all_checks(self):
        trace = self.good_trace()
        events, errors = read_trace(trace)

        self.assertTrue(check_trace_exists(trace, events, errors).passed)
        self.assertTrue(check_skill_invoked(events).passed)
        self.assertTrue(check_behavioral_red(events).passed)
        self.assertTrue(check_implementation_changed(events).passed)
        self.assertTrue(check_green_verified(events).passed)

    def test_compile_error_is_not_behavioral_red(self):
        trace = self.write_trace(
            [
                {
                    "type": "user_message",
                    "text": "Use $test-driven-implementation.",
                },
                {
                    "type": "item.completed",
                    "item": {
                        "type": "command_execution",
                        "aggregated_output": "error: cannot find symbol",
                        "exit_code": 1,
                    },
                },
            ]
        )
        events, errors = read_trace(trace)

        result = check_behavioral_red(events)

        self.assertFalse(result.passed)
        self.assertIn("Inspect JSONL", result.detail)

    def test_final_claim_without_test_execution_is_not_red(self):
        trace = self.write_trace(
            [
                {
                    "type": "item.completed",
                    "item": {
                        "type": "agent_message",
                        "text": "Red: AssertionError, exit 1; then the tests passed.",
                    },
                }
            ]
        )
        events, errors = read_trace(trace)

        self.assertFalse(check_behavioral_red(events).passed)

    def test_cli_passes_complete_trace(self):
        completed = subprocess.run(
            [sys.executable, str(SCRIPT), str(self.good_trace())],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=False,
        )

        self.assertEqual(completed.returncode, 0)
        self.assertIn("BEHAVIORAL_RED: PASS", completed.stdout)
        self.assertIn("GREEN_VERIFIED: PASS", completed.stdout)
        self.assertIn("OVERALL: PASS", completed.stdout)


if __name__ == "__main__":
    unittest.main()
