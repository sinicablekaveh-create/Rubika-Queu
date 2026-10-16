"""Regression tests for full dashboard without sending any message."""
from pathlib import Path
import shutil,subprocess,sys,tempfile,unittest
ROOT=Path(__file__).resolve().parents[1]
class FullDashboardTests(unittest.TestCase):
 def setUp(self):
  t=tempfile.TemporaryDirectory();self.addCleanup(t.cleanup);self.root=Path(t.name)
  d=self.root/"app/src/main/java/ir/sinicable/rubikaqueue";d.mkdir(parents=True)
  (d/"MainActivity.java").write_text('button(q,"تشخیص علت کارنکردن صف (بدون ارسال)",()->showQueueDiagnostics());',encoding="utf-8")
  m=self.root/"app/src/main/AndroidManifest.xml";m.parent.mkdir(parents=True,exist_ok=True)
  m.write_text('<manifest><application></application></manifest>',encoding="utf-8")
  src=self.root/"overrides/full-dashboard/FullDashboardActivity.java";src.parent.mkdir(parents=True)
  shutil.copy(ROOT/"overrides/full-dashboard/FullDashboardActivity.java",src)
  p=self.root/"scripts/add-full-dashboard.py";p.parent.mkdir(parents=True)
  shutil.copy(ROOT/"scripts/add-full-dashboard.py",p)
 def install(self):
  return subprocess.run([sys.executable,"scripts/add-full-dashboard.py"],cwd=self.root,capture_output=True,text=True)
 def test_overlay_idempotence(self):
  self.assertEqual(self.install().returncode,0)
  d=self.root/"app/src/main/java/ir/sinicable/rubikaqueue"
  main=(d/"MainActivity.java").read_text("utf-8")
  self.assertIn("FullDashboardActivity.class",main)
  self.assertEqual(self.install().returncode,0)
  self.assertEqual((d/"MainActivity.java").read_text("utf-8"),main)
  self.assertIn('android:name=".FullDashboardActivity"',(self.root/"app/src/main/AndroidManifest.xml").read_text("utf-8"))
 def test_fail_closed(self):
  p=self.root/"app/src/main/java/ir/sinicable/rubikaqueue/MainActivity.java"
  p.write_text("wrong source",encoding="utf-8")
  self.assertNotEqual(self.install().returncode,0)
 def test_no_network_or_send_in_dashboard(self):
  code=(ROOT/"overrides/full-dashboard/FullDashboardActivity.java").read_text("utf-8")
  for expected in ("JobStore.load(this)","optJSONArray","QueueService.running","MODE_PRIVATE","LAYOUT_DIRECTION_RTL"):
   self.assertIn(expected,code)
  for forbidden in ("client.send(","startForegroundService(","RubikaClient","گروه عمومی نمونه"):
   self.assertNotIn(forbidden,code)
 def test_full_build_type_and_workflow(self):
  gradle=(ROOT/"app/build.gradle").read_text("utf-8")
  workflow=(ROOT/".github/workflows/build-apk.yml").read_text("utf-8")
  assert "applicationIdSuffix '.sidecarfull'" in gradle
  for needed in ("scripts/add-full-dashboard.py","lintSidecarFull assembleSidecarFull","verify_full_sidecar_identity.py"):
   self.assertIn(needed,workflow)
if __name__=="__main__": unittest.main()
