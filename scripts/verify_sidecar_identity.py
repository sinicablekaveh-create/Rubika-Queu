#!/usr/bin/env python3
"""Validate that a debug APK is a truly separate Android package.

Pure badging verification. No account access, installation, or messaging.
"""
import argparse
import json
import re
import subprocess
from pathlib import Path

ORIGINAL_PACKAGE = "ir.sinicable.rubikaqueue"
SIDECAR_PACKAGE = "ir.sinicable.rubikaqueue.sidecar"
ORIGINAL_CLASS_NAMESPACE = ORIGINAL_PACKAGE
SIDECAR_LABEL = "صف روبیکا • نسخه دوم"


def verify_badging(output):
    package = re.search(r"^package:\s+name='([^']+)'", output, re.M)
    launcher = re.search(r"^launchable-activity:\s*name='([^']+)'", output, re.M)
    label = re.search(r"^application-label:\s*'([^']*)'", output, re.M)
    if package is None or package.group(1) != SIDECAR_PACKAGE:
        raise ValueError("Sidecar APK package identity did not match")
    if package.group(1) == ORIGINAL_PACKAGE:
        raise ValueError("APK would conflict with the existing installation")
    if launcher is None or launcher.group(1) != ORIGINAL_CLASS_NAMESPACE + ".MainActivity":
        raise ValueError("Launcher activity class is missing or incorrectly namespaced")
    if label is None or label.group(1) != SIDECAR_LABEL:
        raise ValueError("Sidecar label was not applied in merged Android manifest")
    return {"package":package.group(1), "launcher":launcher.group(1), "label":label.group(1),
            "original_package":ORIGINAL_PACKAGE, "separate_installation":True,
            "existing_app_data_read_or_migrated":False, "on_device_tested":False}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--aapt", required=True)
    parser.add_argument("--report", type=Path, required=True)
    args=parser.parse_args()
    if not args.apk.is_file() or not args.apk.stat().st_size:
        parser.error("Missing or empty APK")
    badging=subprocess.run([args.aapt, "dump", "badging", str(args.apk)],
                           check=True, capture_output=True, text=True, timeout=90).stdout
    result=verify_badging(badging)
    args.report.write_text(json.dumps(result, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    print("Verified separate install:", result["package"], result["launcher"])


if __name__=="__main__":
    main()
