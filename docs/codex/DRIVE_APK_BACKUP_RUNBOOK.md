# Codex handoff: APK artifacts -> private Google Drive backups

Scope: HooshikaDev PR #38 and Rubika-Queu PR #52. This file is a **backup runbook**; it must not trigger releases, phone installations, PR merges, messages, or queue modifications.

## Verified source artifacts (2026-10-17)

| Project | Successful run | Expected artifact | Google Drive target folder ID |
| --- | --- | --- | --- |
| HooshikaDev | sinicablekaveh-create/hooshikadev- Actions run 38985967862 | hooshikadev-debug-apk | `1uzM-me9v-lQUKEtPvV34IZbxtLoTKG5a` |
| Rubika-Queu | sinicablekaveh-create/Rubika-Queu Actions run 38986146204 | Rubika-Queu-v1.4.0-sidecar-test-apk | `1c9ES_MD1EXDPLU42gWS9mZ24MnIhax9n` |

Drive folders already exist. Hooshika is a debug build; Rubika is a separately installed sidecar debug build with a disposable signing key. **Neither is a guaranteed in-place-updatable release APK.**

## Fast, safe workflow for Codex

1. Fetch current PR heads and check GitHub Actions results; use only successfully completed builds. Prefer the latest tested run if it supersedes the listed examples.
2. Download the exact named artifact ZIP via authenticated GitHub access (e.g. `gh run download RUN_ID -R OWNER/REPO -n ARTIFACT_NAME -D DIR`), without mixing build branches. Inspect the ZIP and verify it contains the expected APK; avoid using unchecked filenames.
3. Calculate and record SHA-256 of each APK; verify Android `applicationId`, `versionCode`, and signer SHA-256 using Android SDK `aapt` and `apksigner` if available. If these are unavailable, label metadata verification NOT DONE.
4. Use an **already authorized** Google Drive connector with an authenticated file reference, or a trusted owner-controlled device with an existing authorized Google Drive upload client to upload the APK to the respective folder ID. Never assume GitHub Actions has Drive access. Never print or commit tokens, private keys, keystores, passwords or OAuth refresh credentials.
5. Name artifacts clearly, e.g. `HooshikaDev-PR38-TEST-debug-code14-<shortSHA>.apk` and `Rubika-Queu-PR52-TEST-sidecar-NOT-UPDATABLE-<shortSHA>.apk`. Include a small manifest noting repo, commit SHA, run URL, APK SHA-256, application ID, versionCode, signature fingerprint (public only), test status, and a clear warning if the APK cannot update any existing install.
6. Read back Drive file metadata after each upload and return the **real observed** Drive URL. Do not claim an upload succeeded until verified.
7. Do not remove or overwrite existing Drive backups. Never copy local on-phone queue data or credentials without a separately authorized backup workflow.
8. If Google Drive authorization or file-reference transport is unavailable in Codex, complete local validation and provide the owner the exact one-time connection action; do not invent links and do not block coding or CI tasks while waiting.

## Updatable release requirement

Preserve the installed Rubika queue (~978 items). A sidecar package ID keeps testing separate, but disposable CI debug signing cannot support reliable subsequent updates. Future independent app releases require an owner-controlled durable signing key, a permanently fixed application ID, strictly increasing versionCode and an on-device upgrade test. Do not ask for the legacy signer recovery. Hooshika's normal signed-release workflow verifies a pinned certificate; do not confuse its normal debug workflow with that signed release.

## Codex acceptance

Report in simple Persian: actual CI outcome; files uploaded and verified Drive URLs; SHA-256; APK package/signature update compatibility; any missing authorization; which tasks remain. Never claim install or testing on Galaxy A17 unless completed via an authorized connected device.
