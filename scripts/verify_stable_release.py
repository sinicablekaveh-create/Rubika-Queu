#!/usr/bin/env python3
"""Fail-closed verification for the new, separately signed Rubika-Queu release track.

No legacy installed-key compatibility is claimed. This verifier never installs
an APK, reads private keys, or uploads user data. Only a pinned, non-debug
release signed by the approved *new* certificate may be published.
"""
import argparse
import hashlib
import json
from pathlib import Path

from apk_update_policy import normalize_certificate, parse_badging, parse_signer, run_tool

EXPECTED_PACKAGE = "ir.sinicable.rubikaqueue"
MINIMUM_VERSION_CODE = 15


def verify(badging, signer_text, expected_certificate):
    candidate = parse_badging(badging)
    candidate["certificate_sha256"] = parse_signer(signer_text)
    pinned = normalize_certificate(expected_certificate)
    if candidate["package"] != EXPECTED_PACKAGE:
        raise ValueError("Unexpected Android applicationId")
    if candidate["version_code"] <= MINIMUM_VERSION_CODE:
        raise ValueError("Version must exceed installed v1 versionCode 15")
    if "application-debuggable" in badging:
        raise ValueError("Refusing a debuggable APK as a stable release")
    if candidate["certificate_sha256"] != pinned:
        raise ValueError(
            "New stable signing certificate changed! "
            "Do NOT publish: in-place upgrades on the new track would break."
        )
    return candidate


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--aapt", required=True)
    parser.add_argument("--apksigner", required=True)
    parser.add_argument("--expected-certificate", required=True,
                        help="Pinned public SHA-256 of the new certificate")
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--summary", type=Path)
    args = parser.parse_args()
    if not args.apk.is_file() or args.apk.stat().st_size == 0:
        parser.error("Missing or empty APK")
    if not args.expected_certificate.strip():
        parser.error("RUBIKA_STABLE_SIGNER_SHA256 must be configured first")
    badging = run_tool([args.aapt, "dump", "badging", str(args.apk)])
    signer = run_tool([args.apksigner, "verify", "--print-certs", str(args.apk)])
    candidate = verify(badging, signer, args.expected_certificate)
    digest = hashlib.sha256()
    with args.apk.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    candidate["apk_sha256"] = digest.hexdigest()
    report = {
        "classification": "NEW_INSTALL_TRACK_STABLE_SIGNER_VERIFIED",
        "candidate": candidate,
        "legacy_installed_app_in_place_upgrade_possible": False,
        "on_device_install_tested": False,
        "warning": "A different signer cannot update the old installed app in place. Back up user data before any uninstall. Keep the new signing key for every future upgrade.",
    }
    args.report.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    summary = (
        "### Rubika-Queu new signing track\n"
        "- Result: NEW_INSTALL_TRACK_STABLE_SIGNER_VERIFIED (no device test)\n"
        f"- Package: {candidate['package']}, versionCode {candidate['version_code']}\n"
        f"- Pinned signer SHA-256: {candidate['certificate_sha256']}\n"
        f"- APK SHA-256: {candidate['apk_sha256']}\n"
        "- This cannot update the app signed by the older certificate.\n"
    )
    if args.summary:
        with args.summary.open("a", encoding="utf-8") as stream:
            stream.write(summary)
    print(summary)


if __name__ == "__main__":
    main()
