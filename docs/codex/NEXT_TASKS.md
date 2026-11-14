# Recommended Next Tasks — V3

## P0 — correctness

1. Audit QueueService, QueueService2, and QueueService3 for exact behavior parity.
2. Audit current authentication response parsing without weakening validation.
3. Add deterministic tests for queue state transitions, duplicate guards, rate-limit scheduling, sending-to-uncertain recovery, consent gating, and account-slot ownership.
4. Verify source packaging excludes credentials and signing material.

## P1 — maintainability

5. Refactor duplicated queue implementation into shared logic while preserving three Android service entry points.
6. Replace scattered state strings with a typed queue-state model.
7. Centralize version metadata so Gradle and build-fixed-apk.sh cannot drift.
8. Generate a reproducible release report with package/version, signer certificate digest, APK SHA-256, and included ABIs.

## P2 — user-facing improvements

9. Add a history/review screen for sent, accepted, uncertain, and skipped rows.
10. Add recipient-list editing before Start.
11. Reset consent after every recipient-list mutation.
12. Improve foreground-service status and recovery explanations.
13. Add additional Android ABIs only when a real target device requires them.

## Out of scope until independently verified

- personal-account photo/file upload
- bypassing rate limits
- automatic retry of uncertain messages
- disabling TLS verification
- bypassing login/OTP
- substituting the official Bot API for personal-account transport
