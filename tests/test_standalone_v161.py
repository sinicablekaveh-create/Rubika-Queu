import unittest
from pathlib import Path

ROOT=Path(".")
def src(n): return (ROOT/"app/src/main/java/ir/sinicable/rubikaqueue"/n).read_text(encoding="utf-8")

class Standalone161(unittest.TestCase):
    def test_build_uses_distinct_package(self):
        b=(ROOT/"build-fixed-apk.sh").read_text(encoding="utf-8")
        self.assertIn('package="ir.sinicable.rubikaqueue.triple"',b)
        self.assertNotIn('package="ir.sinicable.rubikaqueue.fixed"',b)

    def test_three_queue_core_is_present(self):
        for n in ("QueueService.java","QueueService2.java","QueueService3.java"):
            self.assertIn("RetryBackoff.delayMs",src(n))
            self.assertIn("INVALID_INPUT",src(n))
            self.assertIn('current.put("status","uncertain")',src(n))

    def test_three_account_screen_present(self):
        m=src("MultiAccountActivity.java")
        self.assertIn("۳ حساب روبیکا • ۳ صف همزمان",m)
        self.assertIn("startAllSaved()",m)
        self.assertIn("stopAll()",m)

    def test_new_launcher_identity(self):
        manifest=(ROOT/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn('android:label="سه صف روبیکا"',manifest)
        self.assertIn('android:icon="@mipmap/ic_launcher"',manifest)
        fg=(ROOT/"app/src/main/res/drawable/triple_icon_foreground.xml").read_text(encoding="utf-8")
        self.assertIn("#21C7A8",fg)
        self.assertIn("#4DA3FF",fg)
        self.assertIn("#FFB347",fg)

if __name__=="__main__": unittest.main()
