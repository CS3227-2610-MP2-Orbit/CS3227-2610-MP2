import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from grade_desktop_ui_skill import (
    check_checklist,
    check_layout_findings,
    check_scope_respected,
    check_skill_invoked,
    check_visual_tokens,
)
from grade_requirements_skill import check_no_implementation, check_trace_exists, read_trace


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "tools" / "graders" / "grade_desktop_ui_skill.py"


def event(payload):
    return json.dumps(payload)


class DesktopUiSkillGraderTest(unittest.TestCase):
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
                        "text": "Use $desktop-ui-polish only. Do not implement.",
                    }
                ),
                event(
                    {
                        "type": "function_call",
                        "name": "exec_command",
                        "arguments": {
                            "cmd": "sed -n '1,200p' .agents/skills/desktop-ui-polish/SKILL.md"
                        },
                    }
                ),
                event(
                    {
                        "type": "item.completed",
                        "item": {
                            "type": "agent_message",
                            "text": (
                                "Findings: dual chrome (outer Home header plus Organizer sidebar) "
                                "squishes width; GridPane labels truncate; list prefWidth is narrow.\n"
                                "Match shared shell tokens (#172033 sidebar, #f7f9fc page, white cards).\n"
                                "Before/after checklist: dual chrome, label truncation, primary actions, "
                                "visual tokens, non-goals.\n"
                                "Non-goal: do not add club CRUD or an add-club button."
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
        self.assertTrue(check_layout_findings(events).passed)
        self.assertTrue(check_visual_tokens(events).passed)
        self.assertTrue(check_checklist(events).passed)
        self.assertTrue(check_scope_respected(events).passed)

        completed = subprocess.run(
            [sys.executable, str(SCRIPT), str(trace)],
            check=False,
            capture_output=True,
            text=True,
        )
        self.assertEqual(completed.returncode, 0, completed.stdout + completed.stderr)
        self.assertIn("OVERALL: PASS", completed.stdout)

    def test_missing_layout_fails(self):
        trace = self.write_trace(
            [
                event({"type": "user_message", "text": "Use $desktop-ui-polish"}),
                event(
                    {
                        "type": "item.completed",
                        "item": {
                            "type": "agent_message",
                            "text": (
                                "Use shared shell tokens #172033. Checklist: non-goals. "
                                "Do not add club CRUD."
                            ),
                        },
                    }
                ),
            ]
        )
        events, _ = read_trace(trace)
        self.assertFalse(check_layout_findings(events).passed)

    def test_invented_club_crud_without_scope_fails(self):
        trace = self.write_trace(
            [
                event({"type": "user_message", "text": "Use $desktop-ui-polish"}),
                event(
                    {
                        "type": "item.completed",
                        "item": {
                            "type": "agent_message",
                            "text": (
                                "Fix dual chrome and truncation. Match #f7f9fc cards. "
                                "Checklist: dual chrome. Implement a full club management UI next."
                            ),
                        },
                    }
                ),
            ]
        )
        events, _ = read_trace(trace)
        self.assertFalse(check_scope_respected(events).passed)

    def test_src_write_fails_no_implementation(self):
        trace = self.write_trace(
            [
                event({"type": "user_message", "text": "Use $desktop-ui-polish"}),
                event(
                    {
                        "type": "function_call",
                        "name": "apply_patch",
                        "arguments": {
                            "patch": "*** Update File: src/main/java/seedu/eventmanager/ui/OrganizerEventView.java"
                        },
                    }
                ),
                event(
                    {
                        "type": "item.completed",
                        "item": {
                            "type": "agent_message",
                            "text": (
                                "Removed dual chrome; checklist done; tokens #172033; "
                                "non-goal: no club CRUD."
                            ),
                        },
                    }
                ),
            ]
        )
        events, _ = read_trace(trace)
        self.assertFalse(check_no_implementation(events).passed)


if __name__ == "__main__":
    unittest.main()
