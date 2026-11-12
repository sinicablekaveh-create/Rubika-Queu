"""Safety and protocol regression checks for v1.5.0 safe host failover."""
import unittest
from pathlib import Path
P=Path("app/src/main/java/ir/sinicable/rubikaqueue")
def src(n): return (P/n).read_text(encoding="utf-8")

class SafeFailover150(unittest.TestCase):
 def test_discovery_keeps_preferred_host_pool(self):
  s=src("RubikaClient.java")
  self.assertIn("apiEndpoints",s)
  self.assertIn('d.optString("default_api","")',s)
  self.assertIn('d.optJSONArray("default_apis")',s)
  self.assertIn("apiEndpoints.contains",s)

 def test_failover_only_read_operations(self):
  s=src("RubikaClient.java")
  b=s[s.index("private boolean safeFailoverMethod"):s.index("private boolean hostSpecificFailure")]
  self.assertIn('"getUserInfo".equals(method)',b)
  self.assertIn('"getContacts".equals(method)',b)
  self.assertNotIn("sendMessage",b)
  self.assertNotIn("sendCode",b)
  self.assertNotIn("signIn",b)
  self.assertNotIn("addAddressBook",b)

 def test_explicit_api_and_rate_errors_never_switch_hosts(self):
  s=src("RubikaClient.java")
  b=s[s.index("private boolean hostSpecificFailure"):s.index("public static void validateEndpoint")]
  self.assertIn("e instanceof ApiError",b)
  self.assertIn("return false",b)
  self.assertIn('status==429',s)

 def test_http_5xx_is_host_specific(self):
  s=src("RubikaClient.java")
  self.assertIn("HostTransportError",s)
  self.assertIn("status>=500&&status<=599",s)

 def test_send_message_remains_single_call_path(self):
  s=src("RubikaClient.java")
  send=s[s.index("public String send("):s.index("public JSONObject session()")]
  self.assertEqual(send.count('call("sendMessage"'),1)
  self.assertIn("freshMessageRnd",s)

 def test_contacts_only_fallback_after_add_address_book_empty(self):
  s=src("RubikaClient.java")
  resolve=s[s.index("public String resolve("):s.index("private static void collectIds")]
  add=resolve.index('call("addAddressBook"')
  fallback=resolve.index("findContactByPhone(number)")
  self.assertLess(add,fallback)
  self.assertIn('call("getContacts"',resolve)
  self.assertIn('"start_id"',resolve)
  self.assertIn('"next_start_id"',resolve)
  self.assertIn('"has_continue"',resolve)

 def test_resolved_guid_is_verified_against_phone(self):
  s=src("RubikaClient.java")
  self.assertIn("verifyResolved",s)
  self.assertIn('call("getUserInfo",obj("user_guid",id),false)',s)
  self.assertIn("Phones.normalize",s)

 def test_group_send_still_disabled(self):
  q=src("QueueService.java")
  self.assertIn('"صف گروهی غیرفعال است"',q)

if __name__=="__main__": unittest.main()
