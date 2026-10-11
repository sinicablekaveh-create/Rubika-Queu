# Documented Rubika Bot API v2 (manual read-only checks)

Source: https://rubika.ir/botapi and https://rubika.ir/botapi/methods

This is a narrow enhancement to the *separate* Official Bot API test screen:
- `getChat` reads metadata for a chat explicitly entered by the operator.
- `getUpdates` performs one **manual** POST request for up to ten events, no background poller; the UI displays only the number of updates, not message contents.
- The user interface enforces a 30-second cooldown for the read-only update preview.
- The existing `getMe` and manually confirmed `sendMessage` remain unchanged.
- No Bot API token is written to disk. Do not log token-bearing URLs or server responses.
- Neither the personal-account queue nor its retry, discovery, storage, or sending logic is modified.

## Source-bundle integration
The checked-in Java source lives in `source/Rubika-Queu-v1.1-personal-queue-source.zip`.
GitHub Actions extracts that ZIP, then checks and applies `patches/official-bot-v2.patch` using `git apply --check` and `git apply`. This avoids replacing the newer ZIP and preserves unrelated QueueService changes.

## Verification
- `javac` + `java` compile/run `tests/BotUpdatesPolicyTests.java` (9 assertions).
- Existing RetryBackoff Java test remains a prerequisite.
- Android `lintDebug` and `assembleDebug`, then signature/zipalign verification, are CI gates.
- Device networking and a live Bot API token are **not** tested automatically.
- Never merge/release if CI fails.
