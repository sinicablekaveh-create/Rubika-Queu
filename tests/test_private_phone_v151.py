import unittest
from pathlib import Path
s=(Path("app/src/main/java/ir/sinicable/rubikaqueue/RubikaClient.java")).read_text()
class Privacy151(unittest.TestCase):
 def test_hidden_phone_requires_matching_contact(self):
  self.assertIn('visiblePhone.isEmpty()&&id.equals(findContactByPhone(number))',s)
 def test_visible_phone_must_match(self):
  self.assertIn('if(number.equals(visiblePhone))return id;',s)
 def test_guid_must_match(self):
  self.assertIn('if(u==null||!id.equals(u.optString("user_guid")))',s)
 def test_fail_closed(self):
  self.assertIn('throw new IOException("تطبیق شماره و حساب ممکن نشد؛ ارسال متوقف شد")',s)
if __name__=="__main__":unittest.main()
