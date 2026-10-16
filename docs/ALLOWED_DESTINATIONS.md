# Approved destinations: personal Rubika queue

The personal-account message queue is deny-by-default. Nothing in an Excel
import, group finder, search cache or queue snapshot can approve a destination.

From the main screen, open «مقصدهای مجاز». While signed into the intended
account, enter either a group GUID (g0...) or a phone number and explicitly
confirm that sending to this destination is authorized. Each approval is
account-scoped, locally persisted in private Android SharedPreferences and can
be revoked from the same screen. Android backup remains disabled.

## Dispatch safety

- An empty approval list blocks ALL personal-queue sends.
- A group row matches its exact group GUID; a personal row matches its
  normalized phone number, never its display name or later-resolved user GUID.
- The queue rechecks approval before resolving a phone or inspecting a group
  and again immediately before each send call, including retries/resumes.
- A revoked or unknown destination stops the queue without a send. Pending
  items remain pending; the sender does not auto-add or auto-approve recipients.
- Approvals are separated by the signed-in Rubika account.
- This does not alter the independent manually confirmed Bot API test flow.

## Review and tests

Java policy tests cover empty lists, exact matching, removal and malformed
IDs. Python installer tests verify both dispatch guards, UI registration,
idempotence and refusal to patch an unexpected sender. A dedicated GitHub PR
job can run these local tests even while the PR is a draft. No test sends any
Rubika message; live device validation remains a separate explicitly approved
step.

Manual QA: start with no approved destinations, create a group/phone test
queue and verify it stops before any resolution or send. Approve one
destination and verify an unapproved item still stops. Revoke an approved
destination during the queue interval and verify the pre-send check blocks it.
Repeat after app restart and account switch. Use only destinations that
explicitly expect test messages; do not run live sending as part of CI.

No merge, APK release, recipient import, or real-message dispatch is performed
by this change.
