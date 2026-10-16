# Development report handoff to ChatGPT

Version `1.3.2` / code `15` adds a dedicated development report screen reached from the existing **فایل و متن پیام** (main queue screen).

## Workflow

1. Tap **گزارش توسعه برای ChatGPT**.
2. Describe one desired change or defect. Do not paste passwords, tokens, recipients, or private messages.
3. Tap **ساخت گزارش توسعه**. The app reads only public package metadata and your description.
4. Review and edit the complete text. Sharing occurs only after tapping **اشتراک گزارش** and confirming. Android's chooser requires you to select a recipient application. Or copy the report, open ChatGPT in a browser, and paste it manually.
5. Use the GitHub repository link in the report for ChatGPT/Codex to inspect existing project source. The phone sends no source archive and has no automatic repository write access.

Sharing via Android `ACTION_SEND` depends on installed apps and does not guarantee ChatGPT appears as a target. The browser option never automatically posts a message. No personal Rubika data, message drafts, queue contents, authenticated sessions, or developer gateway tokens are serialized into the generated text.

## Upgrade conditions

The app's package remains `ir.sinicable.rubikaqueue`. VersionCode increases to 15. To update an installed 1.3.1/code14 app without removing its data, the owner must provide the private signing key matching installed certificate SHA-256 `fdb827fa66b05309a2e7a759bc03d9f22355b54cd74c63e74a5d889823087026`. Never upload or commit the private key into source or chat. Build and verify a candidate via private CI secrets and `scripts/apk_update_policy.py`, then test a device in-place update. Without that key, no artifact should be represented as upgradeable.

The feature requires no new Android runtime permissions. It does not communicate with ChatGPT API without user action.


## Owner-keystore release path

The main-branch build workflow accepts a distinct owner-installed signing key through GitHub Actions secrets only:

- `RUBIKA_INSTALLED_KEYSTORE_BASE64`: PKCS#12 keystore matching the installed v1.3.1 signature.
- `RUBIKA_INSTALLED_KEYSTORE_PASSWORD`: keystore password.
- `RUBIKA_INSTALLED_KEY_PASSWORD`: private-key password.
- `RUBIKA_INSTALLED_KEY_ALIAS`: actual alias in that keystore.

The new keystore takes precedence over the older v1.1 test key if both are configured. The APK signature is checked against the expected installed certificate SHA-256. Debug builds from PRs do not receive secrets, so they remain `NOT_UPDATABLE`. The workflow produces a diagnostic JSON report and does not silently reinterpret a test-signed APK as an update.

A matching certificate does not guarantee on-device upgrade succeeds; perform a backup and controlled `adb install -r` on an authorized test device before distribution. **Never uninstall the existing application solely to test this feature.**
