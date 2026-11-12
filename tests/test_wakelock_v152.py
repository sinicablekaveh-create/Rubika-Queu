import unittest
from pathlib import Path
ROOT=Path("app/src/main/java/ir/sinicable/rubikaqueue")
def src(n): return (ROOT/n).read_text(encoding="utf-8")

class WakeLock152(unittest.TestCase):
    def test_no_unbounded_wakelock_acquire(self):
        q=src("QueueService.java")
        self.assertNotIn("wake.acquire();",q)
        self.assertIn("wake.acquire(120000L)",q)
        self.assertIn("wake.setReferenceCounted(false)",q)

    def test_network_work_acquires_and_finally_releases(self):
        q=src("QueueService.java")
        self.assertIn("acquireCpuForNetwork();",q)
        self.assertIn("}finally{\n    releaseCpu();",q)

    def test_vault_failure_inside_queue_waits_for_relogin(self):
        q=src("QueueService.java")
        self.assertIn("try{refreshed=Vault.load(this);}catch(Exception vaultProblem)",q)
        self.assertIn("نشست ورود هنگام ادامه صف قابل خواندن نبود",q)
        self.assertIn("waitingForAuth=true",q)

    def test_duplicate_wait_notification_removed(self):
        q=src("QueueService.java")
        phrase='checkpoint(job,"انتظار برای نوبت بعد: "+((wait+999)/1000)+" ثانیه");'
        self.assertEqual(q.count(phrase),1)

    def test_rate_limit_logic_still_present(self):
        q=src("QueueService.java")
        self.assertIn("Rate limits are server instructions",q)
        self.assertIn("RetryBackoff.delayMs",q)

if __name__=="__main__": unittest.main()
