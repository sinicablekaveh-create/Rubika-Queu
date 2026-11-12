# Rubika v1.6.3 build verification — 2026-11-12

- Source: original v1.6.3 QUEUE-DEEP archive SHA-256 `d010b86c6eb59f7eb85974818feb0304d0053d9a0988ca371a39095a576bcf42`, with recovered corrections from `Rubika-v163-CODE-CHANGES.diff` and portable build-script root.
- Android build: PASSED on authorized Android Termux device, using `javac`, `aapt2`, `d8`, `zipalign`, `apksigner` and Android 35 platform jar.
- APK: `Rubika-Triple-Queue-v1.6.3-ORIGINAL-ID-DEBUG.apk` (Android Download directory).
- APK SHA-256: `8bb534624aa073326b600d14d928d4583b16e0b5305e1e5041ed24964b2e2081`.
- Application ID: `ir.sinicable.rubikaqueue`; versionCode 34, versionName `1.6.3-queue-deep-reliability`.
- APK structure: aapt2 dump badging PASS; zipalign verification PASS; apksigner V2/V3 PASS.
- Signing: local diagnostic key; this is not a production release or a guaranteed in-place update to an APK signed with another key.
- APK installation on device and live provider login/media/send: NOT TESTED.
- GitHub Actions Gradle test/lint/assemble: requested through branch PR; report separately from local manual build.
- Neither bearer tokens nor keystores are included in the source.
