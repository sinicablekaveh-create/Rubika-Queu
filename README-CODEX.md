# Rubika Triple Queue v3.0.0 — Codex Handoff

This repository snapshot is prepared for continued development with Codex.

## What exists now

The application provides three independent personal-account slots and three independent outbound message queues. Each slot has its own local encrypted session, recipient draft, message text, delay, foreground service, queue file, and consent checkbox.

The V3 UI requires explicit recipient consent before a queue can start. Each checkbox begins unchecked. Importing a new recipient file or adding a manual recipient clears the checkbox again.

The current APK is ARM64 and uses Java for Android/network logic plus C++17/JNI for outbound parameter validation.

## Important protocol note

The public Rubika documentation at https://rubika.ir/botapi is a Bot API. The personal-account client in this project is unofficial and may change without notice.

## Build status

The V3 source compiled, the native library built, APK packaging completed, and APK signature v2/v3 verification passed.

This does not prove that personal-account login or message delivery currently works against Rubika servers.

Start with CODEX_CONTINUATION_PROMPT.md and docs/codex/FILE_MAP.md.
