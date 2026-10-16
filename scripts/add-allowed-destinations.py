#!/usr/bin/env python3
"""Fail-closed, idempotent approval UI and personal-queue send guards."""
from pathlib import Path
import shutil

root = Path("app/src/main/java/ir/sinicable/rubikaqueue")
overrides = Path("overrides/allowed-destinations")
main = root / "MainActivity.java"
queue = root / "QueueService.java"
manifest = Path("app/src/main/AndroidManifest.xml")
targets = (main, queue, manifest)
contents = [p.read_text(encoding="utf-8") for p in targets]

markers = (
    "new Intent(this,AllowedDestinationsActivity.class)",
    "AllowedDestinationStore.requireApproved(this,job.optString(\"owner\"),current)",
    '<activity android:name=".AllowedDestinationsActivity" android:exported="false"/>',
)
state = [marker in content for marker, content in zip(markers, contents)]
if all(state):
    if contents[1].count(markers[1]) != 2:
        raise SystemExit("Incomplete destination guard; refusing inconsistent source")
    for name in ("AllowedDestinationPolicy.java", "AllowedDestinationStore.java",
                 "AllowedDestinationsActivity.java"):
        destination = root / name
        source = overrides / name
        if destination.is_file() and destination.read_bytes() != source.read_bytes():
            raise SystemExit(f"Refusing divergent destination policy: {destination}")
        shutil.copyfile(source, destination)
    print("Approved-destination guard already installed")
    raise SystemExit(0)
if any(state):
    raise SystemExit("Partial destination guard; no files modified")

def replace_one(text, before, after, label):
    if text.count(before) != 1:
        raise SystemExit(f"Unexpected {label} source; no files modified")
    return text.replace(before, after, 1)

main_text = replace_one(
    contents[0],
    "  buildDiscovery();",
    '  buildDiscovery();\n  button(root,"مقصدهای مجاز",()->startActivity(new Intent(this,AllowedDestinationsActivity.class)));',
    "MainActivity button",
)
queue_text = replace_one(
    contents[1],
    'current=job.getJSONArray("recipients").getJSONObject(n);dispatch=false;',
    'current=job.getJSONArray("recipients").getJSONObject(n);dispatch=false;\n'
    '    AllowedDestinationStore.requireApproved(this,job.optString("owner"),current);',
    "queue pre-resolution check",
)
queue_text = replace_one(
    queue_text,
    '    dispatch=true;\n    String messageId;',
    '    AllowedDestinationStore.requireApproved(this,job.optString("owner"),current);\n'
    '    dispatch=true;\n    String messageId;',
    "queue immediate pre-send check",
)
manifest_text = replace_one(
    contents[2],
    '    <activity android:name=".MainActivity"',
    '    <activity android:name=".AllowedDestinationsActivity" android:exported="false"/>\n'
    '    <activity android:name=".MainActivity"',
    "manifest registration",
)
for name in ("AllowedDestinationPolicy.java", "AllowedDestinationStore.java",
             "AllowedDestinationsActivity.java"):
    if not (overrides / name).is_file():
        raise SystemExit(f"Missing approval policy: {name}; no files modified")
    destination = root / name
    if destination.exists() and destination.read_bytes() != (overrides / name).read_bytes():
        raise SystemExit(f"Conflicting approval policy: {name}; no files modified")

# All anchors and overlays were verified before any write.
for path, content in zip(targets, (main_text, queue_text, manifest_text)):
    path.write_text(content, encoding="utf-8")
for name in ("AllowedDestinationPolicy.java", "AllowedDestinationStore.java",
             "AllowedDestinationsActivity.java"):
    shutil.copyfile(overrides / name, root / name)
print("Installed per-account, explicit approved-destination UI and both dispatch guards")
