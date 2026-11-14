# Known Limitations — V3

1. The personal-account Rubika protocol is undocumented and may change.
2. The official public API at https://rubika.ir/botapi is for bots and does not document direct personal-account login.
3. Personal-account image upload/send is not implemented.
4. The UI can preview an image, but a queue with an image is blocked.
5. The current native build contains arm64-v8a only.
6. V3 build/signature verification passed, but live login and end-to-end message delivery are unverified.
7. Server OK without a message ID is recorded as accepted, not confirmed delivered.
8. A post-dispatch transport interruption becomes uncertain and is not automatically retried.
9. Historical group-send code exists, but current personal-recipient queue services skip group rows.
10. V3 intentionally avoids automatic queue resume merely because the activity reopened.
11. Session storage depends on Android Keystore.
12. Uninstalling the application can remove local session and queue state.
13. The manual build targets SDK 34 while compiling against platform 35.
14. Historical project classes remain and should not be deleted without a separate cleanup review.
