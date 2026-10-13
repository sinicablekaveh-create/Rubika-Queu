#!/usr/bin/env python3
"""Report whether a built APK *could* update an owner's known installed APK.

No install actions, keystores or credentials are accessed by this utility.
A positive classification is not proof of an on-device successful update.
"""
import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path


def normalize_certificate(value):
    raw = value.replace(':', '').strip().lower()
    if not re.fullmatch(r'[0-9a-f]{64}', raw):
        raise ValueError('Expected a 64-character certificate SHA-256 digest')
    return raw


def parse_badging(text):
    line = next((line for line in text.splitlines() if line.startswith('package: ')), None)
    if line is None:
        raise ValueError('Cannot find APK package metadata')
    def attr(name):
        match = re.search(r"(?:^|\s)" + re.escape(name) + r"='([^']*)'", line)
        if not match:
            raise ValueError('Missing APK attribute: ' + name)
        return match.group(1)
    code = attr('versionCode')
    if not code.isdecimal():
        raise ValueError('Invalid APK versionCode')
    return {'package': attr('name'), 'version_code': int(code), 'version_name': attr('versionName')}


def parse_signer(text):
    matches = re.findall(r'^Signer #\d+ certificate SHA-256 digest: ([a-fA-F0-9]{64})\s*$', text, re.M)
    if len(matches) != 1:
        raise ValueError('Expected exactly one APK signer SHA-256 digest')
    return normalize_certificate(matches[0])


def classify(candidate, installed):
    checks = {
        'package_matches': candidate['package'] == installed['package'],
        'version_increases': candidate['version_code'] > installed['version_code'],
        'certificate_matches': normalize_certificate(candidate['certificate_sha256']) == normalize_certificate(installed['certificate_sha256']),
    }
    status = 'UPDATE_CANDIDATE_UNTESTED' if all(checks.values()) else 'NOT_UPDATABLE'
    return status, checks


def run_tool(args):
    return subprocess.run(args, check=True, capture_output=True, text=True, timeout=90).stdout


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--aapt', required=True)
    parser.add_argument('--apksigner', required=True)
    parser.add_argument('--installed-package', required=True)
    parser.add_argument('--installed-version-code', type=int, required=True)
    parser.add_argument('--installed-certificate', required=True)
    parser.add_argument('--report', type=Path, required=True)
    parser.add_argument('--summary', type=Path)
    args = parser.parse_args()
    if args.installed_version_code < 1:
        parser.error('Installed version code must be positive')
    if not args.apk.is_file() or args.apk.stat().st_size == 0:
        parser.error('Missing or empty APK')
    installed = {
        'package': args.installed_package,
        'version_code': args.installed_version_code,
        'certificate_sha256': normalize_certificate(args.installed_certificate),
    }
    candidate = parse_badging(run_tool([args.aapt, 'dump', 'badging', str(args.apk)]))
    candidate['certificate_sha256'] = parse_signer(run_tool([args.apksigner, 'verify', '--print-certs', str(args.apk)]))
    digest = hashlib.sha256()
    with args.apk.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    candidate['apk_sha256'] = digest.hexdigest()
    status, checks = classify(candidate, installed)
    report = {
        'classification': status,
        'candidate': candidate,
        'installed': installed,
        'checks': checks,
        'on_device_update_tested': False,
        'warning': 'Never uninstall or clear app data. Matching certificate and higher version code are necessary but on-device update and data retention remain untested.',
    }
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    summary = (
        '### Rubika-Queu installed-APK update policy\n'
        f'- Result: {status} (no on-device update was tested)\n'
        f'- Installed: {installed["package"]}, code {installed["version_code"]}, certificate {installed["certificate_sha256"]}\n'
        f'- Built: {candidate["package"]}, code {candidate["version_code"]}, certificate {candidate["certificate_sha256"]}\n'
        f'- Package match: {checks["package_matches"]}, increased version: {checks["version_increases"]}, signature match: {checks["certificate_matches"]}\n'
        f'- Built APK SHA-256: {candidate["apk_sha256"]}\n'
        '- Do not install on the existing phone unless signature/version compatibility is confirmed and an in-place update has been carefully tested.\n'
    )
    if args.summary:
        with args.summary.open('a', encoding='utf-8') as out:
            out.write(summary)
    print(summary)


if __name__ == '__main__':
    main()
