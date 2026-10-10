# Rubika-Queu Android APK build

This repository currently contains the Android CI scaffold. The real Android Java sources from `Rubika-Queue-v0.8-integrated.zip` must be copied to `app/src/main/java/ir/sinicable/rubikaqueue/` before an APK can be produced. CI explicitly rejects a missing source tree to prevent distributing a nonfunctional empty application.

The APK is signed with the **same existing development test key** (PKCS#12, alias `test`) if provided through GitHub Actions secret `RUBIKA_TEST_KEYSTORE_BASE64`. Do not commit `signing/test-signing.p12` publicly. Retaining the same key, applicationId and incrementing versionCode are required for in-place updates. Optional secrets `RUBIKA_TEST_KEYSTORE_PASSWORD` and `RUBIKA_TEST_KEY_PASSWORD` can override the test key password.

For an end-to-end source import, use the ZIP archive provided in ChatGPT `Rubika-Queu-v0.8-android-gradle-source.zip`. The GitHub workflow will build, verify signature and upload the APK artifact after source and signing secret are supplied.
