# Architecture — V3

## Application identity

- Namespace: ir.sinicable.rubikaqueue
- Application ID: ir.sinicable.rubikaqueue.cpp180
- Version: 3.0.0
- Version code: 50
- Min SDK: 26
- Target SDK: 34
- Compile SDK: 35
- Current native ABI: arm64-v8a

## UI

MultiAccountActivity.java owns three account panels. Each panel contains login controls, XLSX/CSV import, manual recipient entry, message text, interval, image preview, recipient-consent CheckBox, Start/Pause/Stop, and queue status.

V3 consent behavior:
- unchecked by default
- Start disabled while unchecked
- importing recipients clears consent
- adding a manual recipient clears consent
- Start All requires the corresponding checkbox for each slot
- reopening the UI does not automatically start saved queues

## Personal-account client

RubikaClient.java handles endpoint discovery, login requests, session verification, recipient resolution, text sending, response parsing, and rate-limit classification.

The personal-account transport is unofficial. It is not the public Rubika Bot API.

## Session storage

Vault.java encrypts serialized sessions with AES/GCM using Android Keystore. Slot 1 preserves its legacy storage names; slots 2 and 3 use independent aliases and preferences.

## Queue persistence

JobStore.java uses AtomicFile.

Queue files:
- slot 1: job.json
- slot 2: job_2.json
- slot 3: job_3.json

Important row states:
pending, retry_wait, sending, sent, accepted, uncertain, sent_manual, skipped.

## Queue execution

QueueService.java, QueueService2.java, and QueueService3.java run one foreground service per account.

Important semantics:
1. reload the account session
2. ensure queue owner matches the session
3. verify stored consent
4. resolve the recipient
5. persist sending before dispatch
6. send with a stable bounded random request ID
7. classify explicit server errors
8. if a transport interruption occurs after dispatch, mark uncertain
9. never automatically resend uncertain
10. respect rate limits and Retry-After

## Native layer

native_send_core.cpp and NativeSendCore.java provide C++17/JNI validation for recipient GUID shape, message length, and request random ID. HTTPS transport remains Java.

## Import

ExcelReader.java and Phones.java support XLSX, bounded UTF-8 CSV/TSV, manual Iranian mobile numbers, duplicate removal, and a maximum of 1000 unique recipients.

## Network policy

Cleartext traffic is disabled. HttpsURLConnection uses system trust anchors, redirects are disabled, and endpoint validation restricts hosts to approved Rubika/IranLMS domains.
