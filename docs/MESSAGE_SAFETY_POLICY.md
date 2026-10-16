# Message preflight and restart review

This change hardens the existing personal-account queue before it sends:

- Queue preview rejects an empty draft or empty recipient set and validates the configured 30–3600 second interval.
- Only rows explicitly marked `pending` can be dispatched. Previously confirmed (`sent`, `accepted`, or `sent_manual`), skipped, and retry-wait rows are never selected as fresh sends.
- Rows persisted as `sending` or `uncertain` remain blocked for manual review after app/service interruption. The review action only changes the local report; it never sends a message.
- The existing documented Bot API stays separate from the personal-account queue. Its one-message confirmation, cooldown, and `reply_to_message_id` / `disable_notification` fields remain unchanged.

## Verification

Run `MessageQueuePolicyTests` and `test_message_policy_installer.py` in CI. These use no Rubika account, credentials, network requests, or message sends. Android lint/build and device upgrade validation remain separate checks.

The app ID remains `ir.sinicable.rubikaqueue`; versionCode increases to 16. The CI signing key is not assumed to match the installed app. Do not treat an APK as upgradeable until its certificate is verified against the owner's original key and a non-destructive device upgrade check succeeds.
