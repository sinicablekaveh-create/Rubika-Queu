# New signing track: Rubika-Queu 1.4.0 (versionCode 18)

This is a **new installation/signing track**, not an in-place update to the
previously installed code 15 app. The owner's decision is to **stop attempting
to recover the old signing key**. Do not reinstall or clear app data from the
existing phone before independently preserving whatever user data matters.

The application ID remains `ir.sinicable.rubikaqueue`. Android rejects an
in-place update signed by a new, unrelated certificate. Two versions with the
same application ID but different certificates cannot coexist in one Android
user profile. After switching to the new track, later releases can be installed
as upgrades only when signed by **this same permanently retained private key**
and have strictly increasing `versionCode`.

## What CI already does

- Pull request and normal push runs build only an isolated **debug test APK**
  with a throwaway signing key. Never offer such an APK as an updatable release.
- Stable release is a **manual workflow_dispatch** of
  `.github/workflows/build-apk.yml` with `artifact_mode=stable`, on `main`
  only, after the source, policies and tests pass.
- Stable release refuses to proceed unless all four signing secrets **and**
  the independently pinned public certificate SHA-256 repository variable are
  configured. There is no fallback to a debug, legacy test or recovered key.
- The job builds `assembleRelease`, verifies its signature, applicationId,
  versionCode, non-debuggable flag and zip alignment, and compares its actual
  signer to `RUBIKA_STABLE_SIGNER_SHA256`. Only after those checks does it
  upload `Rubika-Queu-v1.4.0-NEW-INSTALL-STABLE.apk` and
  `stable-release-report.json`.
- The resulting workflow artifact is **not** a GitHub Release, a phone install,
  evidence of retained local queue data, or proof of a future update test.
- Preserve the same private key and pinned SHA-256 fingerprint for all future
  releases. Increase versionCode monotonically; record the published highest
  signed version code outside the repository and verify it before another run.

## One-time key creation on a trusted owner-controlled machine

Do this **offline on the owner's trusted Windows or Linux machine**, not in
GitHub Actions, a pull request, a chat transcript or a public CI artifact.

1. Install JDK 17+ and use `keytool` from the JDK. Generate a new, unique
   PKCS#12 key. Run this from a **private folder outside Git repos**; the
   command prompts for a keystore password:

   ```powershell
   keytool -genkeypair -alias rubika-stable-v2 -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 10000 -storetype PKCS12 -keystore rubika-stable-v2.p12 -dname "CN=Rubika Queu Release, OU=Android, O=Owner, C=IR"
   ```

   Do not reuse the expired/legacy/test keys. Protect the PKCS#12 password,
   and keep **two offline backups** of the PKCS#12 file plus the password.
   Do not publish the file, upload it to this chat, or commit it to GitHub.

2. Show the **public** certificate fingerprint (not the key):

   ```powershell
   keytool -list -v -storetype PKCS12 -keystore rubika-stable-v2.p12 -alias rubika-stable-v2
   ```

   Record the `SHA256:` fingerprint as 64 lowercase hex characters **without
   colons**. It is public information and is pinned in GitHub as an Actions
   **repository variable**, `RUBIKA_STABLE_SIGNER_SHA256`.

3. Under GitHub repo **Settings → Secrets and variables → Actions → Secrets**,
   configure these **repository secrets** (never paste values into issues/PRs):

   - `RUBIKA_STABLE_KEYSTORE_BASE64`: Base64 of the **complete PKCS#12 file**.
     To produce this locally without printing it to a public terminal log,
     execute in PowerShell: 
     `[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path '.\\rubika-stable-v2.p12')))`.
     Paste the result only into the GitHub secret field.
   - `RUBIKA_STABLE_KEYSTORE_PASSWORD`: local PKCS#12 store password.
   - `RUBIKA_STABLE_KEY_PASSWORD`: PKCS#12 key password (usually the
     same store password).
   - `RUBIKA_STABLE_KEY_ALIAS`: `rubika-stable-v2`.

4. In **Actions → Build Rubika Queu Test APK → Run workflow**, select
   **Branch: main**, **artifact_mode: stable**. The build is refused if any
   input is missing, the signature differs from the pinned fingerprint, the
   release is debuggable, or the Android package/version is wrong.
5. Download the verified **new-signer** APK artifact. Install only after a
   deliberate backup/migration plan for any currently installed app. The
   existing manifest sets `android:allowBackup="false"`; simply uninstalling
   can permanently remove local queues, encrypted sessions and history.
6. For the **next** version, retain the same keystore and signer fingerprint,
   increase versionCode above the highest previously signed release, then test
   a real on-device in-place upgrade on a **new-track installation**.

## Missing configuration

GitHub's connected repository API in this workflow **cannot set Actions
secrets/variables**. This must be done by the repository owner in the GitHub
settings UI. Do not weaken the signing checks merely to produce an APK.
