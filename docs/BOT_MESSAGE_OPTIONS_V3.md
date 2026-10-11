# Bot API message options v3 (experimental)

Documented source: https://rubika.ir/botapi/methods (sendMessage).

This version extends the **independent** official Bot API test screen:
- Optional reply-to via documented string \`reply_to_message_id\`.
- Optional silent message via documented boolean \`disable_notification\`.
- Both are visible in the per-message confirmation dialog; neither is enabled without user choice.
- All existing sendMessage validation, 30-second local cooldown, getMe/getChat/getUpdates, and personal-account queue remain unchanged.
- No unattended messaging, background polling, rate-limit bypass, stored bot token, or automatic retry is introduced.

The v1.1 source ZIP is still the audited build input. CI extracts it, applies the existing v2 overlay, then executes \`scripts/apply-bot-message-options-v3.py\` with guarded source anchors. The version increments to 1.2 (versionCode 12).

Validation: Java policy assertions, Android lintDebug and assembleDebug, apksigner verify, zipalign, APK artifact upload. A real bot token and Android device are not part of automated testing.
