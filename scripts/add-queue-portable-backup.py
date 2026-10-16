#!/usr/bin/env python3
"""Attach user-confirmed password-encrypted queue backup to the existing app."""
from pathlib import Path
import shutil

base=Path("app/src/main/java/ir/sinicable/rubikaqueue")
main_path=base/"MainActivity.java"
manifest_path=Path("app/src/main/AndroidManifest.xml")
main=main_path.read_text(encoding="utf-8")
manifest=manifest_path.read_text(encoding="utf-8")
marker='button(q,"پشتیبان رمزگذاری‌شده صف / بازیابی"'
if marker in main:
    if main.count(marker)!=1 or 'android:name=".QueueBackupActivity"' not in manifest:
        raise SystemExit("Partial or ambiguous queue backup install")
    print("Backup UI already present")
    raise SystemExit(0)
anchor='button(q,"تشخیص علت کارنکردن صف (بدون ارسال)",()->showQueueDiagnostics());'
if main.count(anchor)!=1:
    raise SystemExit("Diagnostic UI missing or ambiguous: refusing to alter main app")
activity_anchor='<activity android:name=".OfficialBotActivity" android:exported="false"/>'
if manifest.count(activity_anchor)!=1:
    raise SystemExit("Android manifest anchor missing: refusing to alter main app")
new_main=main.replace(anchor,anchor+'\n  button(q,"پشتیبان رمزگذاری‌شده صف / بازیابی",()->startActivity(new Intent(this,QueueBackupActivity.class)));',1)
new_manifest=manifest.replace(activity_anchor,'<activity android:name=".QueueBackupActivity" android:exported="false"/>\n    '+activity_anchor,1)
sources=Path("overrides/queue-backup")
for name in ("QueueBackupCrypto.java","QueueBackupSchema.java","QueueBackupActivity.java"):
    if not (sources/name).is_file():raise SystemExit("Missing backup implementation "+name)
if new_main.count(marker)!=1 or "client.send(" in new_main[main.index(anchor):main.index(anchor)+240]:
    raise SystemExit("Unexpected backup UI edit")
main_path.write_text(new_main,encoding="utf-8")
manifest_path.write_text(new_manifest,encoding="utf-8")
for name in ("QueueBackupCrypto.java","QueueBackupSchema.java","QueueBackupActivity.java"):
    shutil.copyfile(sources/name,base/name)
print("Installed encrypted backup UI: no automatic sends or data migrations")
