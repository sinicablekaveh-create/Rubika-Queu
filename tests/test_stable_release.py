"""Regression checks for the separate, pinned permanent-signing release track."""
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
import verify_stable_release as stable

CERT = "1" * 64


def badging(package="ir.sinicable.rubikaqueue", version=18, debuggable=False):
    data = f"package: name='{package}' versionCode='{version}' versionName='1.4.0-stable'\n"
    return data + ("application-debuggable\n" if debuggable else "")


def signer(cert=CERT):
    return f"Signer #1 certificate SHA-256 digest: {cert}\n"


class StableReleaseVerificationTests(unittest.TestCase):
    def test_accepts_new_non_debuggable_version_with_pinned_single_signer(self):
        c = stable.verify(badging(), signer(), CERT)
        self.assertEqual(c["version_code"], 18)
        self.assertEqual(c["certificate_sha256"], CERT)

    def test_rejects_modified_signing_certificate(self):
        with self.assertRaisesRegex(ValueError, "certificate changed"):
            stable.verify(badging(), signer("2" * 64), CERT)

    def test_rejects_debuggable_apk(self):
        with self.assertRaisesRegex(ValueError, "debuggable"):
            stable.verify(badging(debuggable=True), signer(), CERT)

    def test_rejects_wrong_package(self):
        with self.assertRaisesRegex(ValueError, "applicationId"):
            stable.verify(badging(package="app.rbmain.a"), signer(), CERT)

    def test_rejects_old_version(self):
        for code in (14, 15):
            with self.subTest(code=code):
                with self.assertRaisesRegex(ValueError, "Version"):
                    stable.verify(badging(version=code), signer(), CERT)

    def test_rejects_unparseable_fingerprint(self):
        with self.assertRaises(ValueError):
            stable.verify(badging(), signer(), "invalid")

    def test_rejects_multiple_signers(self):
        with self.assertRaisesRegex(ValueError, "exactly one"):
            stable.verify(badging(), signer() + signer("2" * 64), CERT)

    def test_release_workflow_requires_explicit_dispatch_and_secrets(self):
        flow = (ROOT / ".github" / "workflows" / "build-apk.yml").read_text("utf-8")
        for phrase in (
            "RUBIKA_STABLE_KEYSTORE_BASE64",
            "RUBIKA_STABLE_KEYSTORE_PASSWORD",
            "RUBIKA_STABLE_KEY_PASSWORD",
            "RUBIKA_STABLE_KEY_ALIAS",
            "RUBIKA_STABLE_SIGNER_SHA256",
            "scripts/verify_stable_release.py",
            "assembleRelease",
            "inputs.artifact_mode == 'stable'",
        ):
            with self.subTest(phrase=phrase):
                self.assertIn(phrase, flow)

    def test_stable_signing_secrets_not_available_to_pull_request(self):
        flow = (ROOT / ".github" / "workflows" / "build-apk.yml").read_text("utf-8")
        self.assertIn("github.event_name == 'workflow_dispatch'", flow)
        self.assertIn("github.ref == 'refs/heads/main'", flow)


if __name__ == "__main__":
    unittest.main()
