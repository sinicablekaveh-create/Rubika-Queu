#!/usr/bin/env python3
"""Reject APKs that could overwrite either of the two installed apps."""
import argparse,json,re,subprocess
from pathlib import Path
PKG="ir.sinicable.rubikaqueue.sidecarfull"
ACTIVITY="ir.sinicable.rubikaqueue.FullDashboardActivity"
LABEL="صف روبیکا • نسخه کامل آزمایشی"
def verify(badging):
 def field(pattern):
  m=re.search(pattern,badging,re.M)
  if not m:raise ValueError("Missing badging value: "+pattern)
  return m.group(1)
 package=field(r"^package:\s+name='([^']+)'")
 activity=field(r"^launchable-activity:\s*name='([^']+)'")
 label=field(r"^application-label:\s*'([^']*)'")
 if (package,activity,label)!=(PKG,ACTIVITY,LABEL):
  raise ValueError("Unexpected full APK identity")
 return {"package":package,"launcher":activity,"label":label,"original_and_sidecar_untouched":True,"new_install_only":True,"device_tested":False}
if __name__=="__main__":
 p=argparse.ArgumentParser()
 p.add_argument("--apk",type=Path,required=True);p.add_argument("--aapt",required=True);p.add_argument("--report",type=Path,required=True)
 a=p.parse_args()
 if not a.apk.is_file() or a.apk.stat().st_size==0: p.error("APK missing")
 data=subprocess.run([a.aapt,"dump","badging",str(a.apk)],check=True,capture_output=True,text=True,timeout=90).stdout
 a.report.write_text(json.dumps(verify(data),ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
 print("Verified full APK package",PKG)
