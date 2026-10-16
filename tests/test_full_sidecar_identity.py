"""Separate full build identity check."""
import importlib.util,unittest
from pathlib import Path
p=Path(__file__).resolve().parents[1]/"scripts/verify_full_sidecar_identity.py"
sp=importlib.util.spec_from_file_location("full",p);m=importlib.util.module_from_spec(sp);sp.loader.exec_module(m)
def badge(pkg=m.PKG,activity=m.ACTIVITY,label=m.LABEL):
 return "package: name='"+pkg+"' versionCode='19'\napplication-label:'"+label+"'\nlaunchable-activity: name='"+activity+"'\n"
class Tests(unittest.TestCase):
 def test_valid(self):self.assertTrue(m.verify(badge())["new_install_only"])
 def test_invalid(self):
  for v in ("ir.sinicable.rubikaqueue","ir.sinicable.rubikaqueue.sidecar"):
   with self.assertRaises(ValueError):m.verify(badge(pkg=v))
  with self.assertRaises(ValueError):m.verify(badge(activity="ir.sinicable.rubikaqueue.MainActivity"))
if __name__=="__main__":unittest.main()
