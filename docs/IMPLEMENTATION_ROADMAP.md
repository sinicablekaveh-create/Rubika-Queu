# Rubika-Queu: online AI, Rubika messaging and Android CI roadmap
Updated: 2026-10-13. This is a *status and execution plan*, not a claim that a backend has been deployed.

## Architecture and current facts

1. **Rubika (independent installed app)**: `app.rbmain.a`, reported version 4.1.1. It is a separate application and its APK signing certificate is *not* a Rubika-Queu update key.
2. **Rubika-Queu (this repository)**: `ir.sinicable.rubikaqueue`, owner-installed experimental `1.3.1-ai-draft-return-test`, versionCode 14. An APK exported from that installation was saved privately to the owner's Google Drive. The app signing certificate SHA-256 is `fdb827fa66b05309a2e7a759bc03d9f22355b54cd74c63e74a5d889823087026`.
3. **Messaging**: the repository contains a personal queue, persistence and retries. The independent optional integration uses the documented Rubika Bot API and the owner's own bot token. Do not infer that the Android Rubika app permits control by another app, or use undocumented/private endpoints.
4. **Online AI**: client overlays, an example authenticated HTTPS gateway Worker and gateway unit tests exist. No always-on production gateway or API entitlement is implied by the code. ChatGPT Plus is not an OpenAI Platform API key or prepaid API usage. Provider credentials stay server-side; all draft suggestions and GitHub actions require confirmation.
5. **Automation**: GitHub Actions already builds, tests and uploads an Android APK. An optional AI proposal workflow can create *draft PRs* when an owner-configured OpenAI API key and GitHub permissions are available; it does not automatically approve, merge or release.

## Milestone 1: APK data-preservation gate (this PR)

Before labeling a CI APK as an update for the installed code 14, check:
- package ID equals `ir.sinicable.rubikaqueue`;
- new versionCode is strictly greater than 14;
- APK signing certificate SHA-256 equals `fdb827fa66b05309a2e7a759bc03d9f22355b54cd74c63e74a5d889823087026`.

Generate a machine-readable report and APK SHA-256 with each build. If any check fails, label the artifact `NOT-UPDATABLE`; never recommend reinstall, app-data clearing or a downgrade. Even if these checks all pass, only label `UPDATE_CANDIDATE_UNTESTED` until an actual non-destructive on-device install and data-retention test succeeds.

**Blocker:** the original signing private key is not available in the connected GitHub tools or current CI runners; a matching APK does *not* provide that key. The legacy v1.1 permanent test certificate and transient CI debug keys do not match the owner's installed code 14.

## Milestone 2: online AI availability

Deploy the existing example gateway to an authorized HTTPS host that stays online independently of the Windows computer. Use the existing `docs/ONLINE_AI_GATEWAY.md`, `docs/AUTOMATED_DEVELOPMENT.md`, and `tests/online-development-worker.test.mjs` as the acceptance baseline. Setup requires the owner's separate cloud account and provider API entitlement, keys in server-side secrets, rate/cost limits, independent draft/dev credentials and explicit per-action consent. Do **not** purchase credits or expose account credentials without additional owner approval.

## Milestone 3: messaging coordination

Verify live messaging only against owner-authorized test destinations. Test queue persistence across process death/restart, retries/backoff on rate errors, cancellation, manual confirmation for uncertain delivery, and the separation of "server acknowledged" from "recipient received". Official Bot API testing requires a separately issued bot token and appropriate chat permissions; no unattended bulk sends.

## Milestone 4: release candidate and automation

After messaging/AI tests and signing recovery, bump versionCode above 14, build in Actions, archive the APK and report, verify signing and update data retention on the owner's phone. Maintain rollback/source checkpoints and only then publish a release. Preserve `main` and existing PRs until code review and CI are complete.

## Current decision

Keep Google Drive backup of the owner's current APK private; it does not include app-internal data. PR changes to CI can be evaluated on a separate branch without touching the installed application. No assertion of end-to-end Rubika integration, hosted AI, automated merge or compatible APK release is justified until their respective external gates pass.
