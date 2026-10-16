"""Regression tests: Android CI must cover draft and stacked pull requests."""
from pathlib import Path
import re
import unittest

WORKFLOW = Path(__file__).resolve().parents[1] / '.github/workflows/build-apk.yml'


class PullRequestCiCoverageTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workflow = WORKFLOW.read_text(encoding='utf-8')

    def test_pr_trigger_does_not_filter_non_default_base_branches(self):
        trigger = re.search(
            r'(?m)^  pull_request:\s*\n((?:^    [^\n]*\n|^\s*\n)*)',
            self.workflow,
        )
        self.assertIsNotNone(trigger, 'Missing pull_request event trigger')
        self.assertNotRegex(trigger.group(1), r'(?m)^\s*branches\s*:')

    def test_build_validation_is_not_skipped_for_draft_prs(self):
        build = re.search(r'(?m)^  build:\s*\n((?:^    [^\n]*\n|^\s*\n)*)', self.workflow)
        self.assertIsNotNone(build, 'Missing build job')
        self.assertNotRegex(build.group(1), r'(?m)^\s*if\s*:.*\bdraft\b')


if __name__ == '__main__':
    unittest.main()
