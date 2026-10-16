# Portable queue backup: design and limits

- Password-encrypted binary document via Android Storage Access Framework.
- AES-256-GCM with per-backup random salt and nonce, PBKDF2-HMAC-SHA256
  with 210,000 iterations. Backup password is not stored in Android preferences,
  repository, GitHub CI, or the backup file.
- Versioned portable JSON envelope contains **only allowlisted queue state**:
  owner GUID, message, recipient names/addresses/statuses, original random
  idempotency numbers, server message IDs, send timestamps and review records.
  It never includes Rubika session tokens/private keys from `Vault`.
- Import refuses to overwrite any saved queue, requires an active login
  matching the backed-up owner, and requires an explicit confirmation.
  Scheduled starts and retry timers are cleared; `retry_wait` becomes
  `uncertain`, requiring manual review. No message is sent by import.
- Max input size 8 MiB plaintext, max 8 MiB+1 KiB encrypted file.
- The existing sent-history SQLite database and discovery data are **not**
  included. Thus this is a queue-state snapshot, not a full app clone.
- **CRITICAL**: This new code is NOT installed on the owner's current
  disposable-debug-signed sidecar, and a new stable-signed APK cannot
  update that installation. Do not uninstall or clear data to access this
  future feature. The current version's CSV status report is insufficient
  to recover full job state. Validating a non-destructive route from the
  current build is still a separate prerequisite.
- Compatible only with future builds that include this feature and have
  a proper stable signer. If the current queue is still active, prefer
  retaining its installation rather than risking local data loss.


## Conditional recovery of the ALREADY installed disposable-debug sidecar

There may be one non-destructive way to recover a current debug-built app's
**queue state**, without owning its old signing key: Android's `run-as`
debuggable-app facility. This only works if the installed package really is
debuggable and the phone explicitly authorizes an owner-controlled PC over
USB debugging. This is **untested on the actual phone**. It does not recover
the encrypted login session or sent-history SQLite database.

**DO NOT uninstall, clear app data, or try to upgrade the current sidecar.**

With the queue manually stopped on the phone, JDK 17+ and Android's official
ADB platform-tools installed on a trusted Windows PC and USB debugging
authorized, from a *local clone* of the repository run:

```powershell
.\tools\sidecar-recovery\backup-sidecar.ps1 -OutputPath "$HOME\Desktop\RubikaQueue-secure-backup.rqbak"
```

The script asks for explicit confirmation that the queue was stopped and for
a new password. It reads **only** `files/job.json` from
`ir.sinicable.rubikaqueue.sidecar` through ADB `run-as`; verifies two
identical reads; creates the same encrypted backup envelope supported by the
future Android import UI; never places plaintext queue records on the
computer's disk, terminal, GitHub, or chat. It never invokes messaging or
modifies the phone. It refuses to overwrite a backup path and fails safely
when `run-as` access is not available.

A successful backup is not yet proof that restore and Android installation
work on the user's exact device. The future app must import this archive on
an *empty installation* of the same account before any send is resumed. Test
the restore on a spare device/profile first if possible. Migration across a
different signing certificate still requires uninstalling the installed
same-package app, which must never happen before independent backup
verification and the user's explicit approval. Keep a separate backup of
the original Excel source and any CSV history reports, where available.
