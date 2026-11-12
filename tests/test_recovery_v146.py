"""Regression checks for v1.4.6 recovery notification and visible resume."""
import unittest
from pathlib import Path
P=Path("app/src/main/java/ir/sinicable/rubikaqueue")
def src(n):return (P/n).read_text(encoding="utf-8")

class Recovery146(unittest.TestCase):
 def test_notification_action_exists(self):
  s=src("ScheduledQueueReceiver.java")
  self.assertIn('ACTION_RESUME="ir.sinicable.rubikaqueue.action.RESUME_QUEUE"',s)
  self.assertIn('"ادامه صف"',s)
  self.assertIn("PendingIntent.getBroadcast",s)

 def test_boot_failure_posts_recovery_notification(self):
  s=src("ScheduledQueueReceiver.java")
  self.assertIn("startOrNotify",s)
  self.assertIn("notifyRecovery(context",s)
  self.assertIn("ACTION_BOOT_COMPLETED",s)

 def test_notification_action_starts_queue(self):
  s=src("ScheduledQueueReceiver.java")
  block=s[s.index("if(ACTION_RESUME.equals(action))"):s.index("if(Intent.ACTION_BOOT_COMPLETED",s.index("if(ACTION_RESUME.equals(action))"))]
  self.assertIn("startForegroundService",block)
  self.assertIn("JobStore.hasWork",s)

 def test_visible_activity_auto_resumes_saved_queue(self):
  s=src("MainActivity.java")
  self.assertIn("tryAutoResume()",s)
  self.assertIn('getBoolean("resume",false)',s)
  self.assertIn("JobStore.hasWork(job)",s)
  self.assertIn("startForegroundService(new Intent(this,QueueService.class))",s)

 def test_manual_stop_still_disables_resume(self):
  s=src("QueueService.java")
  self.assertIn('putBoolean("resume",false)',s)
  self.assertIn('"STOP".equals(i.getAction())',s)

 def test_timeout_surfaces_recovery(self):
  s=src("QueueService.java")
  self.assertIn("onTimeout(int startId,int fgsType)",s)
  self.assertIn("ScheduledQueueReceiver.notifyRecovery",s)

 def test_retry_wait_recovery_still_present(self):
  s=src("QueueService.java")
  self.assertIn('row.put("status","pending");restored=true;',s)
  self.assertIn("JobStore.hasWork(job)",s)

if __name__=="__main__":unittest.main()
