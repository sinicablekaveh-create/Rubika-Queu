import unittest
from pathlib import Path

ROOT=Path(".")
SRC=ROOT/"app/src/main/java/ir/sinicable/rubikaqueue"
def src(n): return (SRC/n).read_text(encoding="utf-8")

class DeepAudit162(unittest.TestCase):
    def test_multi_account_is_launcher(self):
        m=(ROOT/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        block=m[m.index('<activity android:name=".MultiAccountActivity"'):m.index('<activity android:name=".AiAssistantActivity"')]
        self.assertIn('android:exported="true"',block)
        self.assertIn('android.intent.action.MAIN',block)
        self.assertIn('android.intent.category.LAUNCHER',block)
        main=m[m.index('<activity android:name=".MainActivity"'):m.index('<activity android:name=".HooshikaInboxActivity"')]
        self.assertNotIn('LAUNCHER',main)

    def test_notification_permission_prompt_present(self):
        m=src("MultiAccountActivity.java")
        self.assertIn("POST_NOTIFICATIONS",m)
        self.assertIn("requestPermissions",m)
        self.assertIn("notification_prompted",m)

    def test_background_reliability_controls_present(self):
        m=src("MultiAccountActivity.java")
        self.assertIn("isIgnoringBatteryOptimizations",m)
        self.assertIn("ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS",m)
        self.assertIn("ACTION_APP_NOTIFICATION_SETTINGS",m)
        self.assertIn("Never sleeping apps",m)

    def test_all_queue_notifications_open_multi_account(self):
        for n in ("QueueService.java","QueueService2.java","QueueService3.java"):
            q=src(n)
            self.assertIn("new Intent(this,MultiAccountActivity.class)",q)

    def test_standalone_package_unchanged(self):
        b=(ROOT/"build-fixed-apk.sh").read_text(encoding="utf-8")
        self.assertIn('package="ir.sinicable.rubikaqueue.triple"',b)

if __name__=="__main__": unittest.main()
