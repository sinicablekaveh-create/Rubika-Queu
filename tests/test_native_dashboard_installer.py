"""Regression tests for isolated RTL launcher integration and no-send boundaries."""
import pathlib
import shutil
import subprocess
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
SCRIPT = ROOT / 'scripts/add-native-dashboard.py'
SRC = ROOT / 'overrides/native-ui/NativeDashboardActivity.java'
ANDROID = '{http://schemas.android.com/apk/res/android}'

MANIFEST = '''<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <application android:label="صف پیام روبیکا" android:supportsRtl="true" android:allowBackup="false">
    <activity android:name=".OfficialBotActivity" android:exported="false" />
    <activity android:name=".MainActivity" android:exported="true">
      <intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
    </activity>
    <service android:name=".QueueService" android:exported="false" />
  </application>
</manifest>'''


class NativeUiInstallerTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = pathlib.Path(self.tmp.name)
        manifest = self.root / 'app/src/main/AndroidManifest.xml'
        manifest.parent.mkdir(parents=True)
        manifest.write_text(MANIFEST, encoding='utf8')
        java = self.root / 'app/src/main/java/ir/sinicable/rubikaqueue'
        java.mkdir(parents=True)
        (java / 'MainActivity.java').write_text('class MainActivity { /* original */ }\n', encoding='utf8')
        (java / 'QueueService.java').write_text('class QueueService { /* untouched */ }\n', encoding='utf8')
        override = self.root / 'overrides/native-ui/NativeDashboardActivity.java'
        override.parent.mkdir(parents=True)
        shutil.copyfile(SRC, override)
        policy = self.root / 'overrides/native-ui/NativeDashboardPreviewPolicy.java'
        shutil.copyfile(ROOT / 'overrides/native-ui/NativeDashboardPreviewPolicy.java', policy)

    def run_installer(self, expect_success=True):
        result = subprocess.run([sys.executable, str(SCRIPT)], cwd=self.root, text=True, capture_output=True)
        self.assertEqual(result.returncode == 0, expect_success, result.stdout + result.stderr)
        return result

    def assert_launcher(self):
        tree = ET.parse(self.root / 'app/src/main/AndroidManifest.xml')
        app = tree.getroot().find('application')
        acts = {a.get(ANDROID + 'name'): a for a in app.findall('activity')}
        self.assertIn('.NativeDashboardActivity', acts)
        self.assertEqual(acts['.MainActivity'].get(ANDROID + 'exported'), 'false')
        self.assertEqual(acts['.NativeDashboardActivity'].get(ANDROID + 'exported'), 'true')
        launch = [(name, f) for name, a in acts.items() for f in a.findall('intent-filter')
                  if any(e.get(ANDROID + 'name') == 'android.intent.action.MAIN' for e in f.findall('action'))]
        self.assertEqual(len(launch), 1)
        self.assertEqual(launch[0][0], '.NativeDashboardActivity')
        self.assertEqual(app.get(ANDROID + 'allowBackup'), 'false')

    def test_local_preview_policy_is_installed_with_activity(self):
        self.run_installer()
        policy = self.root / 'app/src/main/java/ir/sinicable/rubikaqueue/NativeDashboardPreviewPolicy.java'
        self.assertTrue(policy.is_file(), 'Pure-Java preview policy should be copied by installer')
        self.assertEqual(policy.read_bytes(), (ROOT / 'overrides/native-ui/NativeDashboardPreviewPolicy.java').read_bytes())

    def test_installer_preserves_existing_engine_and_legacy_entry(self):
        self.run_installer()
        self.assert_launcher()
        java = self.root / 'app/src/main/java/ir/sinicable/rubikaqueue'
        self.assertEqual((java / 'MainActivity.java').read_text(), 'class MainActivity { /* original */ }\n')
        self.assertEqual((java / 'QueueService.java').read_text(), 'class QueueService { /* untouched */ }\n')
        self.assertEqual((java / 'NativeDashboardActivity.java').read_bytes(), SRC.read_bytes())

    def test_idempotence_and_no_extra_launcher(self):
        self.run_installer()
        one = (self.root / 'app/src/main/AndroidManifest.xml').read_bytes()
        self.run_installer()
        self.assert_launcher()
        self.assertEqual(one, (self.root / 'app/src/main/AndroidManifest.xml').read_bytes())

    def test_missing_audited_source_fails_closed(self):
        (self.root / 'app/src/main/java/ir/sinicable/rubikaqueue/MainActivity.java').unlink()
        self.run_installer(expect_success=False)
        self.assertNotIn('NativeDashboardActivity', (self.root / 'app/src/main/AndroidManifest.xml').read_text())

    def test_unexpected_second_launcher_fails_closed(self):
        manifest = self.root / 'app/src/main/AndroidManifest.xml'
        original = manifest.read_text()
        manifest.write_text(original.replace('</application>', '<activity android:name=".Other" android:exported="true"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter></activity></application>'))
        self.run_installer(expect_success=False)
        self.assertNotIn('NativeDashboardActivity', manifest.read_text())

    def test_no_network_queue_send_or_private_data_api_in_dashboard(self):
        s = SRC.read_text(encoding='utf8')
        self.assertIn('setLayoutDirection(View.LAYOUT_DIRECTION_RTL)', s)
        self.assertIn('auto_on_queue_start', s)
        self.assertIn('open(MainActivity.class)', s)
        self.assertNotIn('RubikaClient', s)
        self.assertNotIn('QueueService.class', s)
        self.assertNotIn('sendMessage(', s)
        self.assertNotIn('getContentResolver(', s)
        self.assertNotIn('HttpURLConnection', s)


if __name__ == '__main__':
    unittest.main()
