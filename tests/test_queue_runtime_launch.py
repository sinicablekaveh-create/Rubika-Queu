"""Ensure users get a queue startup reason without sending messages or leaking recipients."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "source/Rubika-Queu-v1.1-personal-queue-source.zip"


class QueueRuntimeLaunchTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="queue-runtime-launch-")
        self.addCleanup(self.temp.cleanup)
        self.work = Path(self.temp.name)
        with zipfile.ZipFile(SOURCE) as z:
            for n in ("MainActivity.java", "QueueService.java"):
                path = "app/src/main/java/ir/sinicable/rubikaqueue/" + n
                dest = self.work / path
                dest.parent.mkdir(parents=True, exist_ok=True)
                dest.write_bytes(z.read(path))
        shutil.copy(ROOT / "scripts/repair-queue-runtime-launch.py", self.work / "fix.py")
        self.java = self.work / "app/src/main/java/ir/sinicable/rubikaqueue"

    def install(self):
        return subprocess.run(["python3", "fix.py"], cwd=self.work,
                              text=True, capture_output=True)

    def test_queue_launch_no_longer_blocked_by_denied_notifications(self):
        result=self.install()
        self.assertEqual(result.returncode,0,result.stderr)
        main=(self.java/"MainActivity.java").read_text(encoding="utf-8")
        launch=main.split(" void launch(){",1)[1].split(" @Override public void onRequestPermissionsResult",1)[0]
        self.assertIn("startForegroundService",launch)
        self.assertNotIn("requestPermissions(",launch)
        self.assertIn("catch(Exception e)",launch)
        self.assertIn("JobStore.save(this,job)",launch)
        self.assertIn("تشخیص علت کارنکردن صف",main)
        diagnosis=main.split("void showQueueDiagnostics()",1)[1].split("void resolveUnknown()",1)[0]
        self.assertIn("JobStore.uncertain(job)",diagnosis)
        self.assertIn("زمان‌بندی آینده",diagnosis)
        self.assertNotIn('append(client.phone)',diagnosis)
        self.assertNotIn('getString("message")',diagnosis)

    def test_startup_failure_is_visible_in_persisted_job_without_dispatch(self):
        self.assertEqual(self.install().returncode,0)
        service=(self.java/"QueueService.java").read_text(encoding="utf-8")
        self.assertIn('persistQueueStartError("صف شروع نشد',service)
        self.assertIn('persistQueueStartError("خطا در راه‌اندازی صف:',service)
        self.assertIn('job.put("note",reason)',service)
        self.assertIn('JobStore.save(this,job)',service)
        self.assertEqual(service.count("client.send("),1)

    def test_overlay_idempotent_and_rejects_unexpected_source(self):
        self.assertEqual(self.install().returncode,0)
        before=(self.java/"MainActivity.java").read_bytes()
        self.assertEqual(self.install().returncode,0)
        self.assertEqual((self.java/"MainActivity.java").read_bytes(),before)
        service=self.java/"QueueService.java"
        service.write_text("unsupported sender",encoding="utf-8")
        self.assertNotEqual(self.install().returncode,0)


if __name__=="__main__":
    unittest.main()
