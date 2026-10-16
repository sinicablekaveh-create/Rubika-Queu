#!/usr/bin/env python3
"""Read-only encrypted snapshot of a paused, debuggable Rubika sidecar queue.

This tool never installs, sends messages, accesses Vault or restores data.
Requires Android Platform Tools and 'cryptography' on an authorized PC.
"""
import argparse
from collections import Counter
from datetime import datetime, timezone
import getpass
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

try:
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
    from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
    from cryptography.hazmat.primitives import hashes
except ImportError:
    AESGCM = PBKDF2HMAC = hashes = None

PACKAGE = "ir.sinicable.rubikaqueue.sidecar"
MAGIC = b"RQBK1\x00"
AAD = b"RubikaQueue:paused-sidecar:job.json:v1"
MAX_BYTES = 16 * 1024 * 1024
PBKDF_ITERATIONS = 600_000
STATES = frozenset(("pending", "sending", "uncertain", "sent", "accepted",
                    "skipped", "failed", "retry_wait"))
SECRET_KEYS = frozenset(("privatekey", "password", "authtoken", "auth",
                         "session", "sessionkey", "accesstoken", "refreshtoken",
                         "apikey", "otp", "keystore", "verificationcode"))

def crypto_required():
    if AESGCM is None:
        raise RuntimeError("On your trusted PC: python -m pip install cryptography")

def validate(raw):
    if not raw or len(raw) > MAX_BYTES:
        raise ValueError("Missing or excessively large job snapshot")
    job = json.loads(raw.decode("utf-8"))
    if not isinstance(job, dict) or not isinstance(job.get("owner"), str) or not job["owner"]:
        raise ValueError("Invalid owner metadata")
    if not isinstance(job.get("message"), str):
        raise ValueError("Invalid queue message")
    rows = job.get("recipients")
    if not isinstance(rows, list) or not 1 <= len(rows) <= 50000:
        raise ValueError("Invalid queue length")
    def check_secrets(v):
        if isinstance(v, dict):
            for k, item in v.items():
                key = re.sub(r"[^a-z0-9]", "", str(k).lower())
                if key in SECRET_KEYS:
                    raise ValueError("Refusing to back up authentication-like fields")
                check_secrets(item)
        elif isinstance(v, list):
            for item in v:
                check_secrets(item)
    check_secrets(job)
    counts = Counter()
    for row in rows:
        if not isinstance(row, dict) or row.get("status") not in STATES:
            raise ValueError("Unknown or invalid recipient state")
        counts[row["status"]] += 1
    return {"rows": len(rows), "states": dict(sorted(counts.items())),
            "manual_review": counts["sending"] > 0 or counts["uncertain"] > 0}

def key(password, salt):
    crypto_required()
    return PBKDF2HMAC(algorithm=hashes.SHA256(), length=32, salt=salt,
                     iterations=PBKDF_ITERATIONS).derive(password.encode("utf-8"))

def encrypt(raw, password):
    validate(raw)
    if len(password) < 12:
        raise ValueError("Password must have at least 12 characters")
    salt, nonce = os.urandom(16), os.urandom(12)
    return MAGIC + salt + nonce + AESGCM(key(password, salt)).encrypt(nonce, raw, AAD)

def decrypt(blob, password):
    if not blob.startswith(MAGIC) or not 60 <= len(blob) <= MAX_BYTES + 4096:
        raise ValueError("Unknown or oversized backup")
    i = len(MAGIC)
    return AESGCM(key(password, blob[i:i+16])).decrypt(
        blob[i+16:i+28], blob[i+28:], AAD)

def read_debug_queue(serial=None):
    if shutil.which("adb") is None:
        raise RuntimeError("ADB not installed on this trusted computer")
    cmd = ["adb"] + (["-s", serial] if serial else []) + [
        "exec-out", "run-as", PACKAGE, "cat", "files/job.json"]
    process = subprocess.run(cmd, capture_output=True, check=False, timeout=40)
    if process.returncode != 0:
        raise RuntimeError("Authorized ADB could not read the debuggable sidecar job.json")
    return process.stdout

def protected_path(requested):
    p = requested.expanduser().resolve()
    repo = Path(__file__).resolve().parents[1]
    if p == repo or repo in p.parents:
        raise ValueError("Never write private queue data into the source repository")
    if any((parent / ".git").exists() for parent in [p.parent, *p.parents]):
        raise ValueError("Never write private queue data into any Git repository")
    if p.exists():
        raise FileExistsError("Backup exists; refusing overwrite")
    p.parent.mkdir(parents=True, exist_ok=True)
    if os.name != "nt":
        p.parent.chmod(0o700)
    return p

def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    actions = parser.add_mutually_exclusive_group()
    actions.add_argument("--verify", type=Path, help="Check a saved encrypted backup offline")
    actions.add_argument("--output", type=Path, help="Private backup destination outside Git")
    parser.add_argument("--serial", help="ADB serial when multiple devices are connected")
    args = parser.parse_args(argv)
    crypto_required()
    if args.verify:
        info = validate(decrypt(args.verify.read_bytes(), getpass.getpass("Backup password: ")))
        print("Backup authenticated; counts only:", info)
        return 0

    print('First STOP the installed sidecar queue; its diagnosis must say "صف فعال: خیر".')
    print("Do NOT resume the installed source queue after this migration snapshot.")
    if input("Type PAUSED to confirm the sender was stopped: ").strip() != "PAUSED":
        raise RuntimeError("Source queue not confirmed paused")
    raw = read_debug_queue(args.serial)
    info = validate(raw)
    print("Snapshot recipient counts (no numbers or messages):", info)
    print("The source includes private recipients and message text; it will be encrypted.")
    password = getpass.getpass("Choose a private backup password (12+ characters): ")
    if password != getpass.getpass("Repeat the password: "):
        raise ValueError("Passwords did not match")
    blob = encrypt(raw, password)
    if decrypt(blob, password) != raw:
        raise RuntimeError("Encrypted backup verification failed")
    filename = "sidecar-" + datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ") + ".rqbk"
    destination = protected_path(args.output or
        (Path.home() / ".rubikaqueue-private" / "queue-backups" / filename))
    fd = os.open(destination, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    try:
        with os.fdopen(fd, "wb") as stream:
            stream.write(blob)
            stream.flush()
            os.fsync(stream.fileno())
    except BaseException:
        destination.unlink(missing_ok=True)
        raise
    print("Encrypted queue snapshot:", destination)
    print('Check it with: python scripts/backup_sidecar_queue.py --verify "BACKUP_PATH"')
    print("Neither installed app was changed. This tool does NOT restore the queue.")
    return 0

if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as e:
        # No secrets, phone numbers, destination names or raw adb output.
        print("Backup stopped:", type(e).__name__, str(e), file=sys.stderr)
        sys.exit(1)
