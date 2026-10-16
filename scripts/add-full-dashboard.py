#!/usr/bin/env python3
"""Add safe real-data dashboard to extracted audited Android sources."""
from pathlib import Path
import shutil
root=Path("app/src/main/java/ir/sinicable/rubikaqueue")
main=root/"MainActivity.java"
manifest=Path("app/src/main/AndroidManifest.xml")
source=Path("overrides/full-dashboard/FullDashboardActivity.java")
dest=root/"FullDashboardActivity.java"
if not all(p.is_file() for p in (main,manifest,source)):
 raise SystemExit("Audited sources, manifest or dashboard missing")
code=main.read_text("utf-8")
xml=manifest.read_text("utf-8")
marker='button(q,"تشخیص علت کارنکردن صف (بدون ارسال)",()->showQueueDiagnostics());'
entry='button(q,"داشبورد کامل و وضعیت واقعی صف",()->startActivity(new Intent(this,FullDashboardActivity.class)));'
act='<activity android:name=".FullDashboardActivity" android:exported="false"/>'
if entry in code or act in xml:
 if entry in code and act in xml and dest.is_file() and dest.read_bytes()==source.read_bytes():
  print("Already installed");raise SystemExit(0)
 raise SystemExit("Partial overlay; refusing")
if code.count(marker)!=1 or xml.count("</application>")!=1 or dest.exists():
 raise SystemExit("Unexpected source: fail closed")
main.write_text(code.replace(marker,entry+"\n  "+marker,1),"utf-8")
manifest.write_text(xml.replace("</application>","  "+act+"\n  </application>",1),"utf-8")
shutil.copyfile(source,dest)
print("Full RTL dashboard installed, sender not modified")
