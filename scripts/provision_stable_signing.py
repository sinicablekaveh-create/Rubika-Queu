#!/usr/bin/env python3
"""Owner-side, one-time provisioning for the NEW permanent Rubika-Queu signing key.

Run on the owner's trusted Windows/Linux computer, not inside CI or ChatGPT.
Never recovers old keys, uploads the PKCS#12 file as an artifact, or stores it
in the Git repository. GitHub CLI encrypts each secret before transmission.
"""
import argparse
import base64
import getpass
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

REPOSITORY = "sinicablekaveh-create/Rubika-Queu"
ALIAS = "rubika-stable-v2"
KEY_FILENAME = "rubika-stable-v2.p12"
PIN_NAME = "RUBIKA_STABLE_SIGNER_SHA256"
KEY_SECRETS = (
    "RUBIKA_STABLE_KEYSTORE_BASE64",
    "RUBIKA_STABLE_KEYSTORE_PASSWORD",
    "RUBIKA_STABLE_KEY_PASSWORD",
    "RUBIKA_STABLE_KEY_ALIAS",
)
SOURCE_ROOT = Path(__file__).resolve().parents[1]


def normalize_fingerprint(value):
    digest = value.strip().replace(":", "").lower()
    if not re.fullmatch(r"[a-f0-9]{64}", digest):
        raise ValueError("Expected a 64-digit public SHA-256 certificate fingerprint")
    return digest


def extract_fingerprint(output):
    values = re.findall(r"(?mi)^\s*SHA256:\s*([a-f0-9:]{64,95})\s*$", output)
    if len(values) != 1:
        raise ValueError("Could not uniquely read the new key certificate SHA256")
    return normalize_fingerprint(values[0])


def validate_key_location(path):
    candidate = path.expanduser().resolve()
    if candidate.is_relative_to(SOURCE_ROOT):
        raise ValueError("Private keys must NEVER be created inside the Git repository")
    if candidate.suffix.lower() != ".p12":
        raise ValueError("Signing keystore filename must have .p12 extension")
    return candidate


def command(argv, *, input_text=None, env=None):
    """Never include a password or key material in argv or printed diagnostics."""
    proc = subprocess.run(
        argv, input=input_text, text=True, capture_output=True,
        env=env, check=False,
    )
    if proc.returncode != 0:
        # Do not surface tool output from failed secret submissions.
        raise RuntimeError("Command failed: " + " ".join(argv[:3])
                           + ". Check GitHub authentication and repository permissions.")
    return proc.stdout


def list_gh_values(kind, repo):
    fields = "name,value" if kind == "variable" else "name"
    data = command(["gh", kind, "list", "-R", repo, "--json", fields])
    values = json.loads(data)
    if not isinstance(values, list):
        raise RuntimeError("Unexpected GitHub CLI JSON response")
    return {record["name"]: record.get("value") for record in values}


def generate_key(path, password_env):
    path.parent.mkdir(parents=True, exist_ok=True)
    if os.name != "nt":
        path.parent.chmod(0o700)
    if path.exists():
        if not path.is_file():
            raise ValueError("Signing path exists but is not a file")
        return False
    command([
        "keytool", "-genkeypair", "-alias", ALIAS, "-keyalg", "RSA",
        "-keysize", "3072", "-sigalg", "SHA256withRSA",
        "-validity", "10000", "-storetype", "PKCS12", "-keystore", str(path),
        "-dname", "CN=Rubika Queu Stable Release, OU=Android, O=Owner, C=IR",
        "-storepass:env", "RUBIKA_STABLE_LOCAL_PASSWORD",
        "-keypass:env", "RUBIKA_STABLE_LOCAL_PASSWORD", "-noprompt",
    ], env=password_env)
    if not path.is_file() or path.stat().st_size == 0:
        raise RuntimeError("Keytool did not produce a valid PKCS12 file")
    if os.name != "nt":
        path.chmod(0o600)
    return True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", default=REPOSITORY,
                        help="Authenticated GitHub repository")
    parser.add_argument("--key-file", type=Path,
                        default=Path.home() / ".rubikaqueue-private" / KEY_FILENAME)
    args = parser.parse_args()
    if args.repo != REPOSITORY:
        parser.error("Refusing to write signing secrets to an unexpected repository")
    path = validate_key_location(args.key_file)
    if not shutil.which("gh") or not shutil.which("keytool"):
        parser.error("Install/authenticate GitHub CLI (gh) and install Java keytool (JDK 17+) first")
    command(["gh", "auth", "status"])
    configured_vars = list_gh_values("variable", args.repo)
    configured_secrets = list_gh_values("secret", args.repo)
    existing_pin = configured_vars.get(PIN_NAME)
    any_signer_secrets = any(name in configured_secrets for name in KEY_SECRETS)
    if existing_pin:
        existing_pin = normalize_fingerprint(existing_pin)
        if not path.is_file():
            parser.error("A stable certificate is already pinned on GitHub. Restore its "
                         "original private .p12 file from your offline backup; DO NOT generate a different key.")
    elif any_signer_secrets:
        parser.error("Stable signing secrets exist without a public certificate pin. "
                     "Resolve the partial configuration privately before generating or rotating any key.")

    password = getpass.getpass("Password for the NEW stable PKCS12 key (will not echo): ")
    if len(password) < 12:
        parser.error("Choose a new password of at least 12 characters")
    env = os.environ.copy()
    env["RUBIKA_STABLE_LOCAL_PASSWORD"] = password
    created = generate_key(path, env)
    details = command([
        "keytool", "-list", "-v", "-storetype", "PKCS12",
        "-keystore", str(path), "-alias", ALIAS,
        "-storepass:env", "RUBIKA_STABLE_LOCAL_PASSWORD",
    ], env=env)
    cert_pin = extract_fingerprint(details)
    if existing_pin and cert_pin != existing_pin:
        raise RuntimeError("STOP: the local key differs from the pinned GitHub signer; refusing rotation")

    print("New-track certificate SHA-256 (PUBLIC):", cert_pin)
    print("Private signing key is stored locally at:", path)
    if created:
        print("A new key was created. Make TWO secure OFFLINE backups of this key and password.")
    else:
        print("Reusing the existing local key: it was NOT overwritten.")
    if all(name in configured_secrets for name in KEY_SECRETS) and existing_pin == cert_pin:
        print("GitHub signing configuration already exists and certificate pin matches.")
        return

    acknowledgement = input("After making OFFLINE backups, type BACKED UP to continue: ").strip()
    if acknowledgement != "BACKED UP":
        raise RuntimeError("Signing secrets were NOT uploaded; local key remains available for backup")

    # Pin first, then secrets. A failed partial submission is safe to resume
    # using this SAME local .p12; a different public signer is always rejected.
    command(["gh", "variable", "set", PIN_NAME, "-R", args.repo, "--body", cert_pin])
    materials = {
        KEY_SECRETS[0]: base64.b64encode(path.read_bytes()).decode("ascii"),
        KEY_SECRETS[1]: password,
        KEY_SECRETS[2]: password,
        KEY_SECRETS[3]: ALIAS,
    }
    for name in KEY_SECRETS:
        # gh secret set reads the value from standard input. No private data in argv.
        command(["gh", "secret", "set", name, "-R", args.repo],
                input_text=materials[name])
        print("Provisioned secret:", name)

    final_pin = list_gh_values("variable", args.repo).get(PIN_NAME)
    final_secrets = list_gh_values("secret", args.repo)
    if normalize_fingerprint(final_pin or "") != cert_pin or not all(
        name in final_secrets for name in KEY_SECRETS
    ):
        raise RuntimeError("Provisioning not verified; do NOT launch stable APK build")
    print("Verified: stable public certificate pin and all four secret NAMES are present.")
    print("Run Actions > Build Rubika Queu Test APK > main > artifact_mode=stable.")
    print("The workflow independently checks actual APK signer and fails closed.")
    del materials
    password = ""


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, RuntimeError, subprocess.SubprocessError) as exc:
        print("Provisioning stopped:", exc, file=sys.stderr)
        sys.exit(1)
