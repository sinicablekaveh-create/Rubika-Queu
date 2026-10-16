"""Regression tests for a single authoritative Android/core CI pipeline.

The duplicate Rubika Core Tests workflow consistently stopped before job
creation while the APK build ran the same core checks successfully.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
WORKFLOWS = ROOT / ".github" / "workflows"
BUILD = WORKFLOWS / "build-apk.yml"


class CoreCiConsolidationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workflow = BUILD.read_text(encoding="utf-8")

    def test_duplicate_push_and_pr_workflow_is_removed(self):
        self.assertFalse(
            (WORKFLOWS / "gradle.yml").exists(),
            "The redundant core-tests workflow must not trigger a second CI run",
        )

    def test_authoritative_build_keeps_core_coverage(self):
        required = (
            "node tests/online-development-worker.test.mjs",
            "overrides/ai-discovery/DiscoveryRanking.java tests/DiscoveryRankingTests.java",
            "gradle --no-daemon --max-workers=4 -p tests/discovery-runtime test",
            "overrides/plugin-system/*.java tests/PluginSystemTests.java",
            "node tests/personal-gateway-auth.test.mjs",
            "tests/MessageQueuePolicyTests.java",
            "tests/test_apk_update_policy.py",
            "lintDebug assembleDebug",
        )
        for command in required:
            with self.subTest(command=command):
                self.assertIn(command, self.workflow)

    def test_build_still_checks_apk_identity_and_signature(self):
        for phrase in (
            "scripts/apk_update_policy.py",
            "apksigner",
            "zipalign",
            "--installed-version-code 15",
            "NOT-UPDATABLE.apk",
        ):
            with self.subTest(phrase=phrase):
                self.assertIn(phrase, self.workflow)

    def test_push_and_pr_triggers_remain(self):
        self.assertIn("  pull_request:", self.workflow)
        self.assertIn("  push:", self.workflow)
        self.assertIn("    branches: [main]", self.workflow)


if __name__ == "__main__":
    unittest.main()
