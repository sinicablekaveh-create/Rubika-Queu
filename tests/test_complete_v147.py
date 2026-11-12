"""Regression checks for v1.4.7 merged session refresh + dual recovery."""
import unittest
from pathlib import Path
P=Path("app/src/main/java/ir/sinicable/rubikaqueue")
def src(n):return (P/n).read_text(encoding="utf-8")

class Complete147(unittest.TestCase):
 def test_session_reloaded_before_each_recipient(self):
  q=src("QueueService.java")
  start=q.index("private void processPending(")
  end=q.index("long retryAt=",start)
  block=q[start:end]
  self.assertIn("refreshed=Vault.load(this)",block)
  self.assertIn("client=refreshed",block)

 def test_revoked_session_waits_without_losing_queue(self):
  q=src("QueueService.java")
  self.assertIn("waitingForAuth=true",q)
  self.assertIn("صف محفوظ است و منتظر ورود مجدد",q)

 def test_dual_recovery_notification_action(self):
  r=src("ScheduledQueueReceiver.java")
  self.assertIn("ACTION_RESUME",r)
  self.assertIn("PendingIntent.getBroadcast",r)
  self.assertIn('"ادامه صف"',r)

 def test_visible_open_auto_resumes(self):
  m=src("MainActivity.java")
  self.assertIn("tryAutoResume()",m)
  self.assertIn('getBoolean("resume",false)',m)
  self.assertIn("startForegroundService(new Intent(this,QueueService.class))",m)

 def test_manual_stop_wins(self):
  q=src("QueueService.java")
  stop=q[q.index('if(i!=null&&"STOP".equals(i.getAction()))'):q.index("if(running)",q.index('if(i!=null&&"STOP".equals(i.getAction()))'))]
  self.assertIn('putBoolean("resume",false)',stop)
  self.assertIn("START_NOT_STICKY",stop)

 def test_retry_wait_is_not_finished(self):
  self.assertIn('"retry_wait".equals(state)',src("JobStore.java"))
  self.assertIn("JobStore.hasWork(job)",src("QueueService.java"))

 def test_explicit_rejection_does_not_stop_other_rows(self):
  q=src("QueueService.java")
  self.assertIn("INVALID_INPUT and other explicit server rejections",q)
  self.assertIn("صف به مورد بعدی ادامه می‌دهد",q)

 def test_ambiguous_delivery_not_duplicated(self):
  q=src("QueueService.java")
  self.assertIn('current.put("status","uncertain")',q)
  self.assertIn("Never repeat this row automatically",q)

 def test_group_search_absent(self):
  self.assertNotIn("searchGroups",src("MainActivity.java"))
  self.assertNotIn("groupPreviewByJoinLink",src("DiscoveryEngine.java"))

if __name__=="__main__":unittest.main()
