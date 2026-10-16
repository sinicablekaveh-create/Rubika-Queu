# Rubika-Queu side-by-side test installation

This branch builds a **debug-only** secondary Android package:
`ir.sinicable.rubikaqueue.sidecar`. It is not an update for
`ir.sinicable.rubikaqueue`; it is a separate app with a blue icon and a
distinct home-screen name: «صف روبیکا • نسخه دوم».

- The existing queue app, local queue records, saved sessions, and schedules
  remain untouched. **Do not uninstall or clear data of the old app.**
- Android creates a fresh private data sandbox for the sidecar. The old 969
  pending recipients are **not** automatically present or transferred.
- Start with the sidecar in a fresh empty state. Do not import recipients or
  enable schedules until separately verified with the account owner.
- The sidecar includes the queue runtime/authentication error improvements from
  PR #43, and excludes the old approved-destinations feature removed by PR #42.
- Android class namespace stays `ir.sinicable.rubikaqueue`; the *installed app
  identity* changes to `ir.sinicable.rubikaqueue.sidecar` only for debug.
- CI checks the real APK package, launcher component, label, signature and
  alignment. No Rubika message is sent during building or testing.
- **Debug signing keys in GitHub Actions are disposable.** This APK is suitable
  for a clean test installation alongside the old app, but later CI builds
  will generally not update this sidecar in place. Production upgrades need a
  separately managed persistent signing key and a dedicated release strategy.

To install on Android: download the verified sidecar APK, open it in Files,
install it without removing the original app, and open the icon labeled
«صف روبیکا • نسخه دوم». Confirm both icons still exist. If the sidecar
reports `INVALID_AUTH`, sign in through the standard login screen; never
share your verification code or stored session keys.
