# Rubika Triple Queue v1.8.0 (C++/JNI)

Rewritten from v1.7.0 based on the user's own v1.6.3 source.
- Three independent personal-account sessions and outbound queues retained.
- New install ID: ir.sinicable.rubikaqueue.cpp180.
- C++17 JNI shared library validates recipient GUID, message length, and random message ID before Java sends.
- Java RubikaClient still handles HTTPS network I/O, authentication and encryption.
- Login fixes: optional nested data.status no longer incorrectly blocks a valid API response when the already-verified outer status is OK.
- Saved queues, consent-based import, rate limits, and no automatic uncertain-send replay retained.
- Android CMake project included; this Termux build packages arm64-v8a only.
- Not Telegram TDLib. Does not include reverse engineering of official Rubika APK binary.
- The official rubika.ir/botapi interface is for bots; personal-account login/send depends on an undocumented protocol.
- No runtime, account login, message send, or end-to-end tests, per user request.
- No bypass of login or platform rate limits.

Build on ARM64 Termux: bash build-fixed-apk.sh
Output APK: /sdcard/Download/Rubika-Triple-Queue-v1.8.0-CPP-DEBUG.apk
Output source ZIP: /sdcard/Download/Rubika-Triple-Queue-v1.8.0-CPP-SOURCE.zip
