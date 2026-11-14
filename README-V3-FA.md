# Rubika Triple Queue v3.0.0 — C++/JNI HTTPS

- Independent personal-account installation with three separate account vaults and queues.
- Real Android CheckBox per queue for recipient consent: initially unchecked, START disabled until ticked.
- Checkbox resets whenever a new spreadsheet is imported or manual recipient added.
- Starting all saved queues requires each queue's respective checkbox tick.
- After reopening, saved queues are not automatically restarted by the UI without the user selecting consent + START.
- User-triggered local export of privacy-safe diagnostics with counts only; no phone numbers, passwords, sessions, text or tokens.
- CSV template exported locally through the Android document picker.
- The personal-account send/receive transport is still unofficial. Official rubika.ir/botapi supports bots, not direct personal-account login.
- Native C++ validates outbound message parameters; actual HTTPS transport is Java.
- Sending images via personal accounts is blocked pending a verified provider transport.
- APK target: keep under 10MB; a small build is not artificially padded to look more complete.
- Builds and cryptographic signing can be verified, but login and delivery require user-controlled runtime verification.
