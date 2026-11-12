"""Protocol alignment checks for Rubika personal-account sending v1.4.8."""
import unittest
from pathlib import Path
P=Path("app/src/main/java/ir/sinicable/rubikaqueue")
def src(n): return (P/n).read_text(encoding="utf-8")

class SendCore148(unittest.TestCase):
 def test_personal_client_uses_api_v6_pwa_246(self):
  s=src("RubikaClient.java")
  self.assertIn('"api_version","6"',s)
  self.assertIn('"app_version","2.4.6"',s)
  self.assertIn('"platform","PWA"',s)
  self.assertIn('"package","m.rubika.ir"',s)

 def test_send_rnd_matches_current_rubpy_range(self):
  s=src("RubikaClient.java")
  self.assertIn("MAX_MESSAGE_RND=1000000",s)
  self.assertIn("freshMessageRnd()",s)
  self.assertIn("rnd>=1&&rnd<=MAX_MESSAGE_RND",s)
  j=src("JobStore.java")
  self.assertIn("RubikaClient.freshMessageRnd()",j)
  self.assertNotIn("nextInt(999999999)",j)

 def test_old_pending_rnd_is_migrated_before_dispatch(self):
  q=src("QueueService.java")
  self.assertIn("if(!RubikaClient.validMessageRnd(queuedRnd))",q)
  self.assertIn("current.put(\"rnd\",queuedRnd)",q)
  self.assertIn("شناسه داخلی مورد",q)

 def test_send_payload_is_personal_send_message(self):
  s=src("RubikaClient.java")
  self.assertIn('call("sendMessage",obj("object_guid",to,"text",cleanText,"rnd",randomId),false)',s)
  self.assertIn('String cleanText=text==null?"":text.trim()',s)
  self.assertIn('to.matches("u[A-Za-z0-9_]{11,64}")',s)

 def test_get_user_info_requires_guid(self):
  s=src("RubikaClient.java")
  self.assertIn('call("getUserInfo",obj("user_guid",guid),false)',s)
  self.assertNotIn('call("getUserInfo",new JSONObject(),false)',s)

 def test_pwa_registration_fingerprint_consistent(self):
  s=src("RubikaClient.java")
  self.assertIn('"app_version","PW_2.4.6"',s)
  self.assertIn('"system_version","Windows 10"',s)
  self.assertIn('"device_model","Chrome 102"',s)
  self.assertIn("Chrome/102.0.0.0",s)

 def test_error_diagnostics_consider_protocol_fields(self):
  s=src("RubikaClient.java")
  self.assertIn('response.optString("error_code","")',s)
  self.assertIn('response.optString("error","")',s)
  self.assertIn('response.optString("message","")',s)

 def test_group_send_stays_disabled(self):
  q=src("QueueService.java")
  self.assertIn('"group".equals(current.optString("kind"))',q)
  self.assertIn('"صف گروهی غیرفعال است"',q)

if __name__=="__main__": unittest.main()
