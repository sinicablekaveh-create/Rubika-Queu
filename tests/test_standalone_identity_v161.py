import unittest
from pathlib import Path

ROOT=Path(".")
MAN=(ROOT/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
BUILD=(ROOT/"build-fixed-apk.sh").read_text(encoding="utf-8")
GRADLE=(ROOT/"app/build.gradle").read_text(encoding="utf-8")

class Standalone161(unittest.TestCase):
    def test_new_install_identity_is_distinct(self):
        self.assertIn("ir.sinicable.rubikaqueue.triple",BUILD)
        self.assertIn("ir.sinicable.rubikaqueue.triple",GRADLE)
        self.assertNotIn('package="ir.sinicable.rubikaqueue.fixed"',BUILD)

    def test_new_launcher_name(self):
        self.assertIn('android:label="سه صف روبیکا"',MAN)

    def test_adaptive_launcher_icon(self):
        self.assertIn('android:icon="@mipmap/ic_launcher"',MAN)
        self.assertIn('android:roundIcon="@mipmap/ic_launcher_round"',MAN)
        self.assertTrue((ROOT/"app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml").exists())
        self.assertTrue((ROOT/"app/src/main/res/drawable/triple_icon_foreground.xml").exists())

    def test_old_notification_icon_also_changed(self):
        icon=(ROOT/"app/src/main/res/drawable/icon.xml").read_text(encoding="utf-8")
        self.assertIn("#21C7A8",icon)
        self.assertIn("#4DA3FF",icon)
        self.assertIn("#FFB347",icon)

if __name__=="__main__": unittest.main()
