# Test Matrix — V3

## Static/build checks

- clean Java compilation
- D8 dex compilation
- C++ native library compilation
- APK packaging
- APK signature v2/v3 verification
- package/version validation
- native library presence in APK
- cleartext disabled
- sanitized source ZIP excludes keys/tokens/sessions
- all three service classes compile

## Local logic tests to add

- phone normalization
- XLSX/CSV parsing bounds
- duplicate filtering
- 1000-recipient limit
- JobStore state transitions
- sending recovery to uncertain
- uncertain never automatically dispatches
- rate-limit retry timing
- slot-specific vault separation
- slot-specific job separation
- consent starts false
- Start disabled with consent false
- import resets consent
- manual recipient mutation resets consent
- Start All skips unchecked slots

## Authorized runtime checks

Use only user-controlled accounts and recipients who agreed to receive the test message.

- request login code
- complete login
- verify restored session
- send one text message to one consented recipient
- record whether a message ID is returned
- pause/resume one queue
- run three account queues concurrently
- restart the application and inspect recovery
- install V3 over V2 using the same package/signing certificate

Do not mark runtime checks passed until they are actually observed.
