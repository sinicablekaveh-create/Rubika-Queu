# Safe same-queue duplicate destination handling (sidecar only)

This incremental fix starts from the isolated sidecar APK in PR #44.
It **does not touch the original app** or its locally saved records.

The bundled original implementation already saves `sending` through an
Android `AtomicFile` checkpoint **before** the API request and blocks future
queue runs if any `sending` or `uncertain` item needs manual review. These
existing crash guards remain unchanged. Unknown outcomes must not be
automatically marked sent or automatically retried.

New behavior: when the next pending row points to a destination that already
has `sent`, `accepted`, `sending`, `uncertain` or `retry_wait` status in
an **earlier row of the same saved queue**, the later row is marked
`skipped`, with a generic explanation. No second Rubika call is made.
Normalizations cover spaces, separators and Iranian phone prefixes.
Groups and contacts are kept in separate namespaces. The system does not
create new recipients, change message contents or run additional searches.

Limitations:
- This checks duplicates **within one queue only**. Separate installations
  have independent data, and running both queues concurrently can still
  duplicate sends. Keep only one sending service active.
- This does **not** assert that previous messages were delivered, merely that
  they already had an attempted or confirmed state.
- Already processed rows are never rewritten or silently resent.
- Existing 978-row sidecar queues are changed **only when the new version
  actually runs**. No access to or backup of phone contents occurs in CI.
- The current debug APK signing key is disposable; a new APK from CI
  generally **cannot update the already-installed sidecar**. Never delete
  the installed app merely to try this build if its local queue matters.

Tests: Python source-overlay checks; pure Java destination normalization
checks; Android build/lint/signature and packaging in GitHub Actions.
No real messaging in CI.
