"""Regression guard: PR builds run for draft and non-main target branches."""
import re
import unittest
from pathlib import Path

WORKFLOW = Path(__file__).resolve().parents[1] / ".github" / "workflows" / "build-apk.yml"


class PullRequestCiPolicyTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = WORKFLOW.read_text(encoding="utf-8")

    def test_pr_event_does_not_filter_stacked_branch_targets(self):
        event = re.search(r"(?m)^  pull_request:\s*\n((?:^    [^\n]*\n|^\s*\n)*)", self.text)
        self.assertIsNotNone(event, "pull_request event missing")
        self.assertNotRegex(event.group(1), r"(?m)^\s*branches\s*:")

    def test_pr_base_edits_retrigger_verification(self):
        event = re.search(r"(?m)^  pull_request:\s*\n((?:^    [^\n]*\n|^\s*\n)*)", self.text)
        self.assertIsNotNone(event, "pull_request event missing")
        self.assertRegex(event.group(1), r"(?m)^    types\s*:\s*\[[^\]]*\bedited\b[^\]]*\]")

    def test_draft_prs_are_not_skipped(self):
        job = re.search(r"(?m)^  build:\s*\n((?:^    [^\n]*\n|^\s*\n)*)", self.text)
        self.assertIsNotNone(job, "build job missing")
        self.assertNotRegex(job.group(1), r"(?m)^\s*if\s*:.*\bdraft\b")

    def test_stable_release_remains_manual_and_main_only(self):
        self.assertIn("permissions:\n  contents: read", self.text)
        self.assertIn("github.event_name == 'workflow_dispatch' && inputs.artifact_mode == 'stable' && github.ref == 'refs/heads/main'", self.text)
        self.assertIn("RUBIKA_STABLE_SIGNER_SHA256", self.text)
        self.assertIn("scripts/verify_stable_release.py", self.text)


if __name__ == "__main__":
    unittest.main()
