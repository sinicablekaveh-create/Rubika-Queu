"""Verify crash-safe in-queue deduplication is inserted before any send."""
from pathlib import Path
import shutil,subprocess,tempfile,unittest,zipfile
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"source/Rubika-Queu-v1.1-personal-queue-source.zip"
SRC="app/src/main/java/ir/sinicable/rubikaqueue"
class QueueDuplicateSafetyTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory(prefix="dup-guard-")
  self.addCleanup(self.tmp.cleanup)
  self.work=Path(self.tmp.name)
  with zipfile.ZipFile(SOURCE) as z:
   relative=f"{SRC}/QueueService.java"
   dest=self.work/relative
   dest.parent.mkdir(parents=True,exist_ok=True)
   dest.write_bytes(z.read(relative))
  (self.work/"scripts").mkdir()
  shutil.copy(ROOT/"scripts/add-queue-duplicate-guard.py",self.work/"scripts/add-queue-duplicate-guard.py")
  dest=self.work/"overrides/queue-safety/QueueDuplicateGuard.java"
  dest.parent.mkdir(parents=True)
  shutil.copy(ROOT/"overrides/queue-safety/QueueDuplicateGuard.java",dest)
  self.service=self.work/SRC/"QueueService.java"
 def run_patch(self):
  return subprocess.run(["python3","scripts/add-queue-duplicate-guard.py"],cwd=self.work,text=True,capture_output=True)
 def test_guard_precedes_lookup_and_send_without_new_network_calls(self):
  result=self.run_patch()
  self.assertEqual(result.returncode,0,result.stderr)
  source=self.service.read_text(encoding="utf-8")
  self.assertEqual(source.count("client.send("),1)
  self.assertLess(source.index("QueueDuplicateGuard.blocksRepeat("),source.index("client.resolve("))
  self.assertLess(source.index("QueueDuplicateGuard.blocksRepeat("),source.index("client.send("))
  self.assertIn('current.put("status","skipped")',source)
  self.assertIn('current=null;continue;',source)
  self.assertIn('checkpoint(job,"مورد "+(n+1)+" مقصد تکراری بود؛ ارسال نشد")',source)
  self.assertIn('current.put("status","sending");checkpoint(job',source)
  self.assertIn('if(JobStore.uncertain(job))',source)
 def test_idempotent_and_refuses_unknown_source(self):
  self.assertEqual(self.run_patch().returncode,0)
  before=self.service.read_bytes()
  self.assertEqual(self.run_patch().returncode,0)
  self.assertEqual(self.service.read_bytes(),before)
  self.service.write_text("bad sender",encoding="utf-8")
  self.assertNotEqual(self.run_patch().returncode,0)
 def test_offline_only_unrelated_source_and_no_unapproved_dispatch(self):
  source=(ROOT/"overrides/queue-safety/QueueDuplicateGuard.java").read_text(encoding="utf-8")
  self.assertNotIn("RubikaClient",source)
  self.assertNotIn("send(",source)
  self.assertNotIn("android.content",source)
if __name__=="__main__":unittest.main()
