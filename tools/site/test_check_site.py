"""Regression tests for the website checker, not application tests."""

from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from check_site import check


class SiteCheckerTest(unittest.TestCase):
    def setUp(self):
        self.directory = TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name).resolve()
        self.document = (
            '<html lang="en"><head><title>Test</title>'
            '<meta name="viewport" content="width=device-width"></head>'
            '<body><main id="main"><h1>Test</h1>{}</main></body></html>'
        )
        for name in ("index.html", "UserGuide.html", "DeveloperGuide.html",
                     "AgenticSE.html", "Reflections.html", "404.html"):
            (self.root / name).write_text(self.document.format(""), encoding="utf-8")

    def run_check(self, content=""):
        (self.root / "index.html").write_text(
            self.document.format(content), encoding="utf-8")
        output = StringIO()
        with redirect_stdout(output):
            failed = check(self.root, "/CS3227-2610-MP2/")
        return failed, output.getvalue()

    def test_valid_relative_link_and_fragment(self):
        self.assertFalse(self.run_check('<a href="UserGuide.html#main">Guide</a>')[0])

    def test_missing_required_page(self):
        (self.root / "Reflections.html").unlink()
        failed, output = self.run_check()
        self.assertTrue(failed)
        self.assertIn("missing required page: Reflections.html", output)

    def test_missing_asset(self):
        failed, output = self.run_check('<img alt="Test" src="missing.png">')
        self.assertTrue(failed)
        self.assertIn("missing local target", output)

    def test_missing_anchor(self):
        failed, output = self.run_check('<a href="UserGuide.html#absent">Guide</a>')
        self.assertTrue(failed)
        self.assertIn("missing anchor", output)

    def test_base_path_escape(self):
        failed, output = self.run_check('<a href="/UserGuide.html">Guide</a>')
        self.assertTrue(failed)
        self.assertIn("escapes project base path", output)

    def test_duplicate_id(self):
        failed, output = self.run_check('<section id="main"></section>')
        self.assertTrue(failed)
        self.assertIn("duplicate id", output)

    def test_external_links_are_not_checked(self):
        self.assertFalse(self.run_check('<a href="https://example.invalid/">External</a>')[0])


if __name__ == "__main__":
    unittest.main()
