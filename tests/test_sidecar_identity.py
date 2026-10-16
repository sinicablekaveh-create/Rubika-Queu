"""Side-by-side install must never replace or access the original app data."""
import importlib.util
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
module=importlib.util.spec_from_file_location("sidecar",ROOT/"scripts/verify_sidecar_identity.py")
sidecar=importlib.util.module_from_spec(module)
module.loader.exec_module(sidecar)

class SidecarBuildTests(unittest.TestCase):
    def test_debug_variant_is_distinct_but_release_identity_is_unchanged(self):
        gradle=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("applicationId 'ir.sinicable.rubikaqueue'",gradle)
        self.assertIn("namespace 'ir.sinicable.rubikaqueue'",gradle)
        self.assertIn("applicationIdSuffix '.sidecar'",gradle)
        self.assertIn("versionNameSuffix '-sidecar-test'",gradle)
        # Both the disposable debug and the permanently signed sidecar have their\n        # own suffix; the original release build type has no suffix.\n        self.assertEqual(gradle.count("applicationIdSuffix '.sidecar'"),2)\n        release=gradle.split("        release {",1)[1].split("        sidecarStable {",1)[0]\n        self.assertNotIn("applicationIdSuffix",release)

    def test_debug_only_label_icon_and_no_backup(self):
        original=(ROOT/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        overlay=(ROOT/"app/src/debug/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn('android:allowBackup="false"',original)
        self.assertNotIn("نسخه دوم",original)
        self.assertIn("صف روبیکا • نسخه دوم",overlay)
        self.assertIn('tools:replace="android:label"',overlay)
        self.assertNotIn("android:sharedUserId",original+overlay)
        icon=(ROOT/"app/src/debug/res/drawable/icon.xml").read_text(encoding="utf-8")
        self.assertIn("#3543A5",icon)

    def test_valid_merged_apk_identity_and_launcher(self):
        badging=("package: name='ir.sinicable.rubikaqueue.sidecar' versionCode='18' versionName='1.4.0-new-signing-track-sidecar-test'\n"
                 "application-label:'صف روبیکا • نسخه دوم'\n"
                 "launchable-activity: name='ir.sinicable.rubikaqueue.MainActivity' label='صف روبیکا • نسخه دوم' icon=''\n")
        self.assertTrue(sidecar.verify_badging(badging)["separate_installation"])
        for changed in (
            badging.replace("ir.sinicable.rubikaqueue.sidecar","ir.sinicable.rubikaqueue"),
            badging.replace("صف روبیکا • نسخه دوم","صف پیام روبیکا"),
            badging.replace("ir.sinicable.rubikaqueue.MainActivity","ir.sinicable.rubikaqueue.sidecar.MainActivity"),
        ):
            with self.assertRaises(ValueError):
                sidecar.verify_badging(changed)

    def test_ci_verifies_identity_and_releases_are_main_only(self):
        workflow=(ROOT/".github/workflows/build-apk.yml").read_text(encoding="utf-8")
        self.assertIn("scripts/verify_sidecar_identity.py",workflow)
        self.assertIn("SIDECAR-NEW-INSTALL-TEST.apk",workflow)
        self.assertIn("github.ref == 'refs/heads/main'",workflow)
        self.assertIn("test_queue_runtime_launch.py",workflow)

if __name__=="__main__":
    unittest.main()
