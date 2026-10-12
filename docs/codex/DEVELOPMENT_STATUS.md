# Rubika-Queu development checkpoint — 2026-10-13

## Scope and constraints

One Plus-usage-efficient milestone: prepare read-only Android APK signing inspection when Windows Remote Desktop is offline.

- Repository: `sinicablekaveh-create/Rubika-Queu`
- Baseline `main` commit checked before changes: `1b07700515e7018c989e4f0b86019e4898b88af6`
- Working branch: `diagnostics/windows-apk-signing-identity`; PR #24 remains a **draft** pending device-side validation.
- No destructive device, release, keystore, account-secret, or `main` changes permitted by this milestone.
- Latest previously verified main APK workflow: build #128, run `38283730507`, artifact `11762122453`, v1.3.1 development APK **NOT-UPDATABLE** with its ephemeral CI debug signing key.
- Current installed APK is reported by the owner as unofficial v1.3; its actual signer certificate has not been verified.

## Completed milestone

1. Inspected current PR #24 and selected CI evidence (CI #76 passed before these edits; unrelated Java/Conda template workflows had startup failures).
2. Added optional `-ApkPath` to the Windows signing-audit script. This path verifies certificate SHA-256 using Android SDK `apksigner` without requiring ADB, and does not modify or delete the supplied file.
3. Preserved the existing Android device/ADB inspection path, temporary APK cleanup, and keystore path enumeration.
4. Documented a phone-only preparation route: a trusted export of the **installed** app's base APK can be stored privately and inspected separately in a cloud environment, subject to package/version/provenance checks.
5. No keystore/password/APK was placed in GitHub; the exported-file certificate alone does not provide the private signing key.

## Validation gate

- GitHub CI from the updated PR head must pass; check the latest PR run before review/merge.
- PowerShell runtime and real-device behavior cannot be certified until a compatible Windows/PowerShell environment (and, for ADB mode, the owner-authorized phone) is available.
- An APK exported from the phone must have its package ID and versionCode validated independently before its signer fingerprint is treated as evidence of the installed version.
- Review any code quality findings before removing draft status.

## Unresolved release blocker

A safe in-place update requires the package ID `ir.sinicable.rubikaqueue`, compatible signing identity with the **installed** APK, and a suitable higher versionCode. Original signing private key unavailable in the current GitHub Actions environment. Never uninstall, clear app storage, or label temporary-debug APK artifacts as upgrade-safe.

## Next actions

1. Read PR #24 CI results after this checkpoint; resolve any blocking failures.
2. Obtain verified public signing fingerprint of the app **currently installed** on the owner's Android phone; use trusted private transfer only.
3. Locate and verify ownership of the matching private keystore when Windows becomes available, or identify another documented signing path.
4. Run targeted tests, Android lint and a gated build; then verify APK SHA-256 and certificate. Only after proof of compatibility should a release be considered.

Milestone completed as documentation and source changes only; do not claim an Android update was installed.
