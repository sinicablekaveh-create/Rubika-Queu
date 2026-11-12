"""Regression checks for v1.4.5 persisted retry_wait recovery."""
import unittest
from pathlib import Path
P=Path("app/src/main/java/ir/sinicable/rubikaqueue")
def src(n):return (P/n).read_text(encoding="utf-8")
class Recovery145(unittest.TestCase):
 def test_retry_wait_is_unfinished(self):
  j=src("JobStore.java")
  self.assertIn("boolean hasWork(JSONObject j)",j)
  self.assertIn('"retry_wait".equals(state)',j)
 def test_receiver_does_not_abandon_retry_wait(self):
  r=src("ScheduledQueueReceiver.java")
  self.assertIn("JobStore.hasWork(job)",r)
  self.assertIn("resumable(context)",r)
 def test_user_can_resume_retry_wait(self):
  m=src("MainActivity.java")
  self.assertIn("JobStore.hasWork(old)",m)
  self.assertIn("JobStore.hasWork(job)",m)
 def test_scheduler_preserves_retry_wait(self):
  self.assertIn("JobStore.hasWork(job)",src("ScheduleManager.java"))
 def test_crash_recovery_clears_expired_retry_deadline(self):
  q=src("QueueService.java")
  self.assertIn('job.optLong("next_retry_time",0)<=System.currentTimeMillis()',q)
  self.assertIn('row.put("status","pending");restored=true;',q)
  self.assertIn('if(restored){job.put("next_retry_time",0);JobStore.save(this,job);}',q)
 def test_supervisor_does_not_complete_retry_wait(self):
  self.assertIn("if(!JobStore.hasWork(job)){",src("QueueService.java"))
 def test_unknown_send_not_repeated(self):
  self.assertIn('current.put("status","uncertain")',src("QueueService.java"))
if __name__=="__main__":unittest.main()
