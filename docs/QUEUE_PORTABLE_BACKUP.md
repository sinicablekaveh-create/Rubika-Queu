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
