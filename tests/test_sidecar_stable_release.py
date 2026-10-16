"""Offline safety tests for a permanently signed *second* Android app."""
import sys
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"scripts"))
import verify_sidecar_stable as stable

CERT="a"*64

def badging(package="ir.sinicable.rubikaqueue.sidecar",version=18,label="صف روبیکا • نسخه دوم",
            launcher="ir.sinicable.rubikaqueue.MainActivity",debuggable=False,
            version_name="1.4.0-new-signing-track-sidecar-stable"):
    a=(f"package: name='{package}' versionCode='{version}' versionName='{version_name}'\n"
       f"application-label:'{label}'\n"
       f"launchable-activity: name='{launcher}' label='{label}' icon=''\n")
    return a+("application-debuggable\n" if debuggable else "")

def signer(value=CERT):
    return "Signer #1 certificate SHA-256 digest: "+value+"\n"

class SidecarStableSigningTests(unittest.TestCase):
    def test_accepts_pinned_nondebuggable_sidecar_only(self):
        candidate=stable.verify(badging(),signer(),CERT)
        self.assertEqual(candidate["package"],"ir.sinicable.rubikaqueue.sidecar")
        self.assertEqual(candidate["version_code"],18)
    def test_rejects_certificate_mismatch(self):
        with self.assertRaisesRegex(ValueError,"certificate changed"):
            stable.verify(badging(),signer("b"*64),CERT)
    def test_refuses_primary_app_or_other_app(self):
        for package in ("ir.sinicable.rubikaqueue","com.example.sidecar"):
            with self.subTest(package=package), self.assertRaises(ValueError):
                stable.verify(badging(package=package),signer(),CERT)
    def test_refuses_debug_or_wrong_launcher(self):
        for sample in (badging(debuggable=True),badging(launcher="ir.sinicable.rubikaqueue.sidecar.MainActivity")):
            with self.assertRaises(ValueError):
                stable.verify(sample,signer(),CERT)
    def test_refuses_wrong_label_and_build_type_and_old_version(self):
        for sample in (badging(label="صف پیام روبیکا"),badging(version_name="1.4-debug"),badging(version=17)):
            with self.assertRaises(ValueError):
                stable.verify(sample,signer(),CERT)
    def test_isolated_build_config_and_owner_only_release(self):
        gradle=(ROOT/"app/build.gradle").read_text("utf-8")
        flow=(ROOT/".github/workflows/build-apk.yml").read_text("utf-8")
        self.assertIn("applicationIdSuffix '.sidecar'",gradle)
        self.assertIn("sidecarStable {",gradle)
        self.assertIn("signingConfig = sidecarKeyFile.exists()",gradle)
        self.assertIn("RUBIKA_SIDECAR_KEYSTORE_PASSWORD",gradle)
        self.assertIn("lintSidecarStable assembleSidecarStable",flow)
        self.assertIn("scripts/verify_sidecar_stable.py",flow)
        self.assertIn("RUBIKA_SIDECAR_SIGNER_SHA256",flow)
        self.assertIn("inputs.artifact_mode == 'sidecar_stable' && github.ref == 'refs/heads/main'",flow)
        self.assertIn("ci-sidecar-disposable.p12",flow)
        self.assertIn("trap 'rm -f signing/ci-sidecar-disposable.p12",flow)
        self.assertIn("test_stable_release.py",flow)
        self.assertIn("assembleRelease",flow)
        manifest=(ROOT/"app/src/sidecarStable/AndroidManifest.xml").read_text("utf-8")
        self.assertIn("صف روبیکا • نسخه دوم",manifest)
    def test_no_private_material_committed(self):
        # Repository carries only build recipes, never private keystore bytes.
        flow=(ROOT/".github/workflows/build-apk.yml").read_text("utf-8")
        self.assertNotIn("RUBIKA_SIDECAR_KEYSTORE_BASE64: ",flow)

if __name__=="__main__":unittest.main()
