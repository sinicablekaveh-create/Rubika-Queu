import unittest
from pathlib import Path
ROOT=Path(".")
def src(p): return (ROOT/p).read_text(encoding="utf-8")

class SideBySide161(unittest.TestCase):
    def test_new_application_id(self):
        g=src("app/build.gradle")
        self.assertIn("applicationId 'ir.sinicable.rubikaqueue.triple'",g)
        self.assertNotIn("applicationIdSuffix '.fixed'",g)

    def test_manual_build_uses_new_package(self):
        b=src("build-fixed-apk.sh")
        self.assertIn('package="ir.sinicable.rubikaqueue.triple"',b)
        self.assertIn('1.6.1-three-account-side-by-side',b)

    def test_new_icon_is_distinct(self):
        icon=src("app/src/main/res/drawable/icon.xml")
        self.assertIn("#312E81",icon)
        self.assertIn("#C4B5FD",icon)
        self.assertIn("#67E8F9",icon)

    def test_three_account_feature_preserved(self):
        m=src("app/src/main/java/ir/sinicable/rubikaqueue/MultiAccountActivity.java")
        self.assertIn("۳ حساب روبیکا • ۳ صف همزمان",m)
        self.assertIn("for(int slot=1;slot<=3;slot++)buildPanel(slot)",m)

if __name__=="__main__": unittest.main()
