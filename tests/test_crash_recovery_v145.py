import unittest
from pathlib import Path
ROOT=Path("app/src/main/java/ir/sinicable/rubikaqueue")
s=(ROOT/"QueueService.java").read_text()
class CrashRecoveryTests(unittest.TestCase):
 def test_sending_recovers_to_uncertain(self):
  self.assertIn('if("sending".equals(row.optString("status")))',s)
  self.assertIn('row.put("status","uncertain")',s)
  self.assertIn('recoveredInFlight=true',s)
 def test_inflight_recovery_saved(self):
  self.assertIn('if(recoveredInFlight)checkpoint(job',s)
 def test_network_guard_in_inner_loop(self):
  start=s.index('private void processPending(')
  end=s.index('long retryAt=',start)
  self.assertIn('if(!NetworkState.online(this))',s[start:end])
 def test_vault_errors_wait_for_relogin(self):
  self.assertIn('try{client=Vault.load(this);}catch(Exception vaultProblem)',s)
  self.assertIn('waitingForAuth=true',s)
 def test_finally_resets_state(self):
  self.assertIn('}finally{\n   waitingForAuth=false;\n   running=false;',s)
 def test_no_group_search(self):
  self.assertNotIn('searchGroups',(ROOT/"MainActivity.java").read_text())
if __name__=="__main__":unittest.main()
