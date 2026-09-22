import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from grade_code_review_skill import (
    check_ownership_defect,
    check_reproduction_scoped,
    check_skill_invoked,
    check_state_loss,
)
from grade_requirements_skill import (
    check_no_implementation,
    check_trace_exists,
    read_trace,
)


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "tools" / "graders" / "grade_code_review_skill.py"


def event(payload):
    return json.dumps(payload)


class CodeReviewSkillGraderTest(unittest.TestCase):
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
                    "text": "Use $code-review-and-verification for C1.",
                },
                {
                    "type": "item.completed",
                    "item": {
                        "type": "command_execution",
                        "command": "java CancellationReviewRepro",
                        "aggregated_output": (
                            "Expected: cancelled=false, exists=true\n"
                            "Actual: cancelled=true, exists=false\n"
                            "AssertionError: Non-owner cancelled another attendee's registration"
                        ),
                        "exit_code": 1,
                    },
                },
                {
                    "type": "item.completed",
                    "item": {
                        "type": "agent_message",
                        "text": (
                            "Finding: cancel ignores the actor and never verifies ownership, "
                            "so a non-owner can cancel and remove another attendee's "
                            "registration instead of leaving it intact. The reproduction shows "
                            "Expected exists=true, Actual exists=false with AssertionError. "
                            "This is a defect in the synthetic fixture, not a claim about the "
                            "production application."
                        ),
                    },
                },
            ]
        )

    def test_complete_c1_trace_passes_all_checks(self):
        trace = self.good_trace()
        events, errors = read_trace(trace)

        self.assertTrue(check_trace_exists(trace, events, errors).passed)
        self.assertTrue(check_skill_invoked(events).passed)
        self.assertTrue(check_ownership_defect(events).passed)
        self.assertTrue(check_state_loss(events).passed)
        self.assertTrue(check_reproduction_scoped(events).passed)
        self.assertTrue(check_no_implementation(events).passed)

    def test_src_patch_fails_no_implementation(self):
        trace = self.write_trace(
            [
                {
                    "type": "function_call",
                    "name": "apply_patch",
                    "arguments": "*** Update File: src/main/java/CancellationService.java\n@@",
                }
            ]
        )
        events, errors = read_trace(trace)

        result = check_no_implementation(events)

        self.assertFalse(result.passed)

    def test_final_claim_without_reproduction_execution_fails(self):
        trace = self.write_trace(
            [
                {
                    "type": "item.completed",
                    "item": {
                        "type": "agent_message",
                        "text": (
                            "Expected: exists=true. Actual: exists=false. AssertionError. "
                            "This synthetic fixture is not the production application."
                        ),
                    },
                }
            ]
        )
        events, errors = read_trace(trace)

        self.assertFalse(check_reproduction_scoped(events).passed)

    def test_cli_passes_complete_trace(self):
        completed = subprocess.run(
            [sys.executable, str(SCRIPT), str(self.good_trace())],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=False,
        )

        self.assertEqual(completed.returncode, 0)
        self.assertIn("OWNERSHIP_DEFECT: PASS", completed.stdout)
        self.assertIn("REPRODUCTION_SCOPED: PASS", completed.stdout)
        self.assertIn("NO_IMPLEMENTATION: PASS", completed.stdout)
        self.assertIn("OVERALL: PASS", completed.stdout)


if __name__ == "__main__":
    unittest.main()
