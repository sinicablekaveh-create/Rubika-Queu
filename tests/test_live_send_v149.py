"""Regression checks for v1.4.9 live personal-send diagnostics."""
import unittest
from pathlib import Path
P=Path("app/src/main/java/ir/sinicable/rubikaqueue")
def src(n): return (P/n).read_text(encoding="utf-8")

class LiveSend149(unittest.TestCase):
 def test_session_check_uses_own_guid(self):
  s=src("RubikaClient.java")
  self.assertIn("verifySession()",s)
  self.assertIn('me()',s)
  self.assertIn('guid.equals(returned)',s)

 def test_no_send_account_probe_exists(self):
  m=src("MainActivity.java")
  self.assertIn("بررسی اتصال حساب روبیکا (بدون ارسال)",m)
  self.assertIn("verifyAccount()",m)
  self.assertIn("live.verifySession()",m)
  block=m[m.index("void verifyAccount()"):m.index("void testSelf()")]
  self.assertNotIn(".send(",block)

 def test_self_test_uses_current_send_core(self):
  m=src("MainActivity.java")
  block=m[m.index("void testSelf()"):m.index("void logout()")]
  self.assertIn("RubikaClient.freshMessageRnd()",block)
  self.assertIn("live.send(live.guid",block)
  self.assertNotIn("nextInt(1000000000)",block)

 def test_self_test_reloads_vault_session(self):
  m=src("MainActivity.java")
  block=m[m.index("void testSelf()"):m.index("void logout()")]
  self.assertIn("Vault.load(this)",block)
  self.assertIn("live.verifySession()",block)

 def test_network_guard_before_live_probe(self):
  m=src("MainActivity.java")
  self.assertIn("if(!NetworkState.online(this))",m)

 def test_core_still_uses_v6_personal_envelope(self):
  s=src("RubikaClient.java")
  self.assertIn('"api_version","6"',s)
  self.assertIn('"platform","PWA"',s)
  self.assertIn('"package","m.rubika.ir"',s)

 def test_rate_limits_not_bypassed(self):
  q=src("QueueService.java")
  self.assertIn("RetryBackoff.delayMs",q)
  self.assertIn("محدودیت موقت روبیکا",q)

if __name__=="__main__": unittest.main()
