#!/usr/bin/env python3
"""Verify a pinned-signature non-debuggable release for the SECOND app only.

Does not read user data, produce a private key, install an APK, or imply an
upgrade from the currently installed CI-debug-signed sidecar.
"""
import argparse
import hashlib
import json
from pathlib import Path
from apk_update_policy import normalize_certificate, parse_badging, parse_signer, run_tool
from verify_sidecar_identity import SIDECAR_PACKAGE, verify_badging

MINIMUM_SIDECAR_VERSION_CODE = 18

def verify(badging, signer_text, expected_certificate):
    # Validates package, human-readable label, and *original Java namespace*
    # launcher, which differs from the final installed Android package id.
    check = verify_badging(badging)
    candidate = parse_badging(badging)
    if candidate["package"] != SIDECAR_PACKAGE or not check["separate_installation"]:
        raise ValueError("Wrong sidecar applicationId")
    if candidate["version_code"] < MINIMUM_SIDECAR_VERSION_CODE:
        raise ValueError("Unexpected sidecar versionCode")
    if "application-debuggable" in badging:
        raise ValueError("A stable APK must not be debuggable")
    if "-sidecar-stable" not in candidate["version_name"]:
        raise ValueError("Not the intended sidecar stable build type")
    candidate["certificate_sha256"] = parse_signer(signer_text)
    if candidate["certificate_sha256"] != normalize_certificate(expected_certificate):
        raise ValueError("Sidecar permanent signing certificate changed")
    return candidate

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--aapt", required=True)
    parser.add_argument("--apksigner", required=True)
    parser.add_argument("--expected-certificate", required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--summary", type=Path)
    args = parser.parse_args()
    if not args.apk.is_file() or args.apk.stat().st_size==0:
        parser.error("Missing or empty APK")
    if not args.expected_certificate.strip():
        parser.error("Pinned sidecar certificate is missing")
    badge=run_tool([args.aapt,"dump","badging",str(args.apk)])
    sign=run_tool([args.apksigner,"verify","--print-certs",str(args.apk)])
    candidate=verify(badge,sign,args.expected_certificate)
    digest=hashlib.sha256(args.apk.read_bytes()).hexdigest()
    report={
       "classification":"SIDECAR_PERMANENT_SIGNER_VERIFIED_NEW_INSTALL_ONLY",
       "candidate":candidate,
       "apk_sha256":digest,
       "original_app_untouched":True,
       "current_disposable_debug_sidecar_update_possible":False,
       "on_device_installed_or_migrated":False,
       "warning":"This signer differs from disposable CI debug signing. Do NOT uninstall an active sidecar with local queue. No in-place update or data migration is proven."
    }
    args.report.write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    summary=("### Permanent-signer second-app track\n"
     "- Pinned certificate / non-debuggable APK verified; no device install tested.\n"
     f"- Android package: {candidate['package']}; code {candidate['version_code']}\n"
     f"- Certificate SHA-256: {candidate['certificate_sha256']}\n"
     "- Cannot update the already installed disposable-debug-signed sidecar.\n"
     "- Keep the current app and queue intact; use a tested migration plan first.\n")
    if args.summary:
        with args.summary.open("a",encoding="utf-8") as out:out.write(summary)
    print(summary)

if __name__=="__main__":main()
