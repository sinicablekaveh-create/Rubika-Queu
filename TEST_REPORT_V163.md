# Rubika v1.6.3 test report — 2026-11-12

- Source correction applied against byte-identical original QUEUE-DEEP source.
- Targeted Python recovery safety tests: 4 PASS.
- Targeted Python queue-deep tests: 5 PASS.
- Three-account structural tests: 8 PASS.
- Direct JVM Excel CSV checks: 10 PASS.
- Direct JVM queue-safety checks: 10 PASS.
- Full historical Python test suite: 185 tests executed; 14 failures and 3 errors. Legacy expectations include the previous `.triple` package and alternate icon; those conflict with preserving the original application ID and icon, and other failures must be reviewed separately. Do not treat this suite as green.
- Manual Android compile/package/sign checks: PASS; Gradle tests, lint, emulator/device installation, personal-account auth, and live message delivery: NOT VERIFIED.
- No messages were sent in the course of these tests.
