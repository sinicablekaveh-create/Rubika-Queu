"""Guardrails: validate real release assembly using a disposable CI certificate.

The test-only release must never be uploaded or substitute for owner signing.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = (ROOT / ".github/workflows/build-apk.yml").read_text(encoding="utf-8")
GRADLE = (ROOT / "app/build.gradle").read_text(encoding="utf-8")


class ReleaseSmokeTestPolicy(unittest.TestCase):
    def test_gradle_uses_explicit_disposable_signing_override(self):
        self.assertIn("RUBIKA_STABLE_KEYSTORE_PATH", GRADLE)
        self.assertIn("signing/stable-release.p12", GRADLE)
        self.assertIn("stableKeyFile.exists()", GRADLE)

    def test_real_ci_release_assembly_and_identity_verification(self):
        self.assertIn("Verify throwaway key can sign a release APK", WORKFLOW)
        self.assertIn("ci-test-release.p12", WORKFLOW)
        self.assertIn("RUBIKA_STABLE_KEYSTORE_PATH: signing/ci-test-release.p12", WORKFLOW)
        self.assertIn("lintRelease assembleRelease", WORKFLOW)
        self.assertIn("scripts/verify_stable_release.py", WORKFLOW)
        self.assertIn("ci-release-smoke-report.json", WORKFLOW)

    def test_no_permanent_signing_secret_in_test_only_run(self):
        self.assertIn("if: github.event_name != 'workflow_dispatch' || inputs.artifact_mode != 'stable'", WORKFLOW)
        self.assertIn("trap 'rm -f signing/ci-test-release.p12", WORKFLOW)
        self.assertIn("RUBIKA_STABLE_KEYSTORE_PASSWORD: ci-throwaway-only-not-a-secret", WORKFLOW)

    def test_release_smoke_precedes_owner_secret_restoration(self):
        self.assertLess(
            WORKFLOW.index("Verify throwaway key can sign a release APK"),
            WORKFLOW.index("Restore explicitly provisioned stable release key"),
        )
        self.assertIn("Upload pinned new-signer APK (manual dispatch only)", WORKFLOW)

    def test_policy_runs_in_main_ci(self):
        self.assertIn("test_stable_release_smoke.py", WORKFLOW)


if __name__ == "__main__":
    unittest.main()
