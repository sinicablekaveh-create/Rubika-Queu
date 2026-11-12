"""Offline invariants for Rubika Queue 1.4.3 resilient supervisor."""
import unittest
from pathlib import Path
ROOT=Path("app/src/main/java/ir/sinicable/rubikaqueue")
def read(name): return (ROOT/name).read_text(encoding="utf-8")

class QueueResilience143(unittest.TestCase):
    def test_supervisor_retries_unexpected_failures(self):
        s=read("QueueService.java")
        self.assertIn("while(!cancel)",s)
        self.assertIn("supervisorRetry++",s)
        self.assertIn("sleepInterruptibly(delay)",s)
        self.assertIn("START_STICKY",s)

    def test_invalid_input_does_not_stop_queue(self):
        s=read("QueueService.java")
        self.assertIn("other explicit server rejections",s)
        self.assertIn('current.put("status","skipped")',s)
        self.assertIn('checkpoint(job,"مورد "+(n+1)+" توسط سرور رد شد ("+c+")؛ صف به مورد بعدی ادامه می‌دهد")',s)

    def test_ambiguous_send_is_not_retried(self):
        s=read("QueueService.java")
        self.assertIn('current.put("status","uncertain")',s)
        self.assertIn("Never repeat this row automatically",s)
        self.assertIn("صف ادامه دارد",s)

    def test_pre_dispatch_failures_retry(self):
        s=read("QueueService.java")
        self.assertIn('current.put("status","retry_wait")',s)
        self.assertIn('job.put("next_retry_time"',s)
        self.assertIn("Before dispatch, automatic retry is safe",s)

    def test_rate_limit_backs_off(self):
        s=read("QueueService.java")
        self.assertIn("RetryBackoff.delayMs",s)
        self.assertIn("محدودیت موقت روبیکا",s)
        self.assertIn("rate(c)",s)

    def test_only_user_action_auth_or_completion_are_terminal(self):
        s=read("QueueService.java")
        self.assertIn('"INVALID_AUTH".equals(c)',s)
        self.assertIn('putBoolean("resume",false)',s)
        self.assertIn('if(!JobStore.hasWork(job)){',s)

    def test_timeout_saves_for_resume(self):
        s=read("QueueService.java")
        self.assertIn("onTimeout(int startId,int fgsType)",s)
        self.assertIn('putBoolean("resume",true)',s)

    def test_group_discovery_remains_removed(self):
        self.assertNotIn("searchGroups",read("MainActivity.java"))
        self.assertNotIn("groupPreviewByJoinLink",read("DiscoveryEngine.java"))

if __name__=="__main__": unittest.main()
