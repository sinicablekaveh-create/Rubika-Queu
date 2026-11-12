import unittest
from pathlib import Path
ROOT=Path("app/src/main/java/ir/sinicable/rubikaqueue")
def src(n): return (ROOT/n).read_text(encoding="utf-8")

class ThreeAccount160(unittest.TestCase):
    def test_three_service_classes_exist(self):
        for n in ("QueueService.java","QueueService2.java","QueueService3.java"):
            self.assertTrue((ROOT/n).exists(),n)

    def test_three_independent_job_files(self):
        j=src("JobStore.java")
        self.assertIn('slot==1?"job.json":"job_"+slot+".json"',j)
        self.assertIn("load(Context c,int slot)",j)
        self.assertIn("save(Context c,int slot,JSONObject j)",j)

    def test_three_independent_vaults(self):
        v=src("Vault.java")
        self.assertIn('"vault_"+slot',v)
        self.assertIn('"rubika_queue_session_v2_slot_"+slot',v)
        self.assertIn("save(Context context,int slot",v)
        self.assertIn("load(Context context,int slot",v)

    def test_queue_gate_allows_parallel_queue_keys(self):
        g=src("OperationGate.java")
        self.assertIn('key.startsWith("queue")',g)
        self.assertIn("if(queueKey(key))",g)
        self.assertIn("for(String x:active)if(!queueKey(x))return false",g)

    def test_each_service_uses_own_slot(self):
        q1=src("QueueService.java");q2=src("QueueService2.java");q3=src("QueueService3.java")
        self.assertIn('OperationGate.acquire("queue1")',q1)
        self.assertIn('OperationGate.acquire("queue2")',q2)
        self.assertIn('OperationGate.acquire("queue3")',q3)
        self.assertIn("JobStore.load(this,2)",q2)
        self.assertIn("Vault.load(this,2)",q2)
        self.assertIn("JobStore.load(this,3)",q3)
        self.assertIn("Vault.load(this,3)",q3)

    def test_multi_account_ui_controls_all_three(self):
        m=src("MultiAccountActivity.java")
        self.assertIn("۳ حساب روبیکا • ۳ صف همزمان",m)
        self.assertIn("for(int slot=1;slot<=3;slot++)buildPanel(slot)",m)
        self.assertIn("startAllSaved()",m)
        self.assertIn("stopAll()",m)

    def test_manifest_registers_three_services(self):
        manifest=Path("app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        for n in (".QueueService",".QueueService2",".QueueService3"):
            self.assertIn('android:name="'+n+'"',manifest)
        self.assertIn(".MultiAccountActivity",manifest)

    def test_rate_limit_not_bypassed(self):
        for n in ("QueueService.java","QueueService2.java","QueueService3.java"):
            q=src(n)
            self.assertIn("Rate limits are server instructions",q)
            self.assertIn("RetryBackoff.delayMs",q)

if __name__=="__main__": unittest.main()
