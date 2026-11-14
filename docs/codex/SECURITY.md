# Security and Data Handling — V3

## Never commit or upload

- Rubika session/authentication material
- OTP codes
- account passwords
- imported recipient lists
- queue files containing recipient data
- SQLite history databases
- Android signing keystores
- signing passwords
- GitHub tokens
- private runtime logs containing account information

## Session storage

Vault.java encrypts serialized session data using AES/GCM. Key material is managed by Android Keystore. Each account slot uses independent storage.

## Network

- HTTPS only
- cleartext traffic disabled
- standard certificate validation
- endpoint hostname allow-list
- redirects disabled

Do not install trust-all TLS managers, disable hostname verification, accept HTTP fallback, or follow arbitrary redirects.

## Message safety

Before send:
- account must be authenticated
- queue must belong to that account
- recipient list must exist
- explicit recipient consent must be checked
- message must pass validation
- recipient must resolve
- request random ID must be valid

After dispatch:
- ambiguous interruption becomes uncertain
- uncertain is never automatically resent
- manual review is required

## Rate limits

Rate-limit responses must pause/back off. Do not build evasion or bypass behavior.

## Protocol warning

The project contains an unofficial personal-account protocol implementation. The public documented API at https://rubika.ir/botapi is a bot API and must not be treated as an equivalent personal-account protocol.
