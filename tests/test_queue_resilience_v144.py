"""Offline invariants for Rubika Queue 1.4.4 network/auth resilient supervisor."""
import unittest
from pathlib import Path
ROOT=Path("app/src/main/java/ir/sinicable/rubikaqueue")
def read(name): return (ROOT/name).read_text(encoding="utf-8")

class QueueResilience144(unittest.TestCase):
    def test_offline_wait_does_not_consume_row_retry(self):
        s=read("QueueService.java")
        block=s[s.index("if(!NetworkState.online(this))"):s.index("if(job.optLong",s.index("if(!NetworkState.online(this))"))]
        self.assertIn("sleepInterruptibly(15000L)",block)
        self.assertNotIn("row_retry_count",block)
        self.assertIn("بدون مصرف retry",block)

    def test_auth_recovery_keeps_queue_alive(self):
        s=read("QueueService.java")
        self.assertIn("waitingForAuth=true",s)
        self.assertIn("منتظر ورود مجدد",s)
        self.assertIn("continue;",s)
        ui=read("MainActivity.java")
        self.assertIn("authRecovery=QueueService.waitingForAuth",ui)
        self.assertIn("اگر صف منتظر ورود بود به‌صورت خودکار ادامه می‌دهد",ui)

    def test_row_transient_failure_has_ceiling(self):
        s=read("QueueService.java")
        self.assertIn('current.optInt("row_retry_count",0)+1',s)
        self.assertIn("if(rowRetry>=5)",s)
        self.assertIn('current.put("status","skipped")',s)
        self.assertIn("صف به مورد بعدی ادامه می‌دهد",s)

    def test_server_rejection_continues(self):
        s=read("QueueService.java")
        self.assertIn("INVALID_INPUT and other explicit server rejections",s)
        self.assertIn('current.put("server_code",c)',s)
        self.assertIn("صف به مورد بعدی ادامه می‌دهد",s)

    def test_ambiguous_dispatch_never_retries_same_row(self):
        s=read("QueueService.java")
        self.assertIn('current.put("status","uncertain")',s)
        self.assertIn("Never repeat this row automatically",s)

    def test_network_permission_present(self):
        manifest=Path("app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn("android.permission.ACCESS_NETWORK_STATE",manifest)
        n=read("NetworkState.java")
        self.assertIn("NET_CAPABILITY_VALIDATED",n)

    def test_group_search_still_removed(self):
        self.assertNotIn("searchGroups",read("MainActivity.java"))
        self.assertNotIn("groupPreviewByJoinLink",read("DiscoveryEngine.java"))

if __name__=="__main__": unittest.main()
