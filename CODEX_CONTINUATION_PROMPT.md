# Codex Continuation Prompt — Rubika Triple Queue v3.0.0

Continue development from the current V3 baseline.

Read these files before editing:
1. AGENTS.md
2. README-CODEX.md
3. docs/codex/ARCHITECTURE.md
4. docs/codex/BUILD_AND_RELEASE.md
5. docs/codex/KNOWN_LIMITATIONS.md
6. docs/codex/NEXT_TASKS.md
7. docs/codex/TEST_MATRIX.md
8. docs/codex/SECURITY.md

## Current baseline

- Android package: ir.sinicable.rubikaqueue.cpp180
- Version: 3.0.0, versionCode 50
- Three independent personal-account slots and three independent queues
- Java Android Views UI with Persian RTL labels
- C++17/JNI module: librubikasendcore.so
- HTTPS transport in Java using HttpsURLConnection
- Recipient sources: XLSX, CSV, or manual Iranian mobile numbers
- Each account has an explicit recipient-consent CheckBox
- Consent starts unchecked; Start is disabled until checked
- Changing recipients clears consent
- Text sending exists in RubikaClient.send()
- Personal-account image sending is intentionally blocked
- The official https://rubika.ir/botapi documentation is for bots, not personal-account login
- Latest V3 APK build and APK signature v2/v3 verification succeeded
- Live personal-account login and message delivery have not been verified for V3

## Non-negotiable behavior

- Do not bypass recipient consent, authentication, OTP, TLS validation, or server rate limits.
- Never automatically retry a message that became uncertain after network dispatch.
- Do not commit passwords, OTPs, account sessions, recipient lists, signing keys, or private runtime logs.
- Do not claim recipient delivery merely because a server returned OK.
- Do not use the Bot API as a substitute for personal-account image or login transport.
- Preserve independent account stores and queue files for all three slots.
- Manual number entry prepares a draft only; it must never send immediately.
- Show proposed destructive deletions or reorganizations before applying them.

## First action for Codex

Perform a static review and a clean build. Report concrete defects before changing behavior. Keep changes minimal and preserve the V3 consent flow.

If authentication, queue recovery, send semantics, persistence, consent, or packaging changes, update the corresponding docs/codex file in the same commit.
