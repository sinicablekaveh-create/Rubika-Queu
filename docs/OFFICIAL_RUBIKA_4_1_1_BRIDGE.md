# Official Rubika 4.1.1 integration

The Drive artifact `Rubika_4.1.1_411.apk` was inspected from the connected
library on 2026-10-15.

| Property | Observed value |
| --- | --- |
| Package | `app.rbmain.a` |
| Version | `4.1.1` / version code `411` |
| Launcher activity | `org.rbmain.ui.LaunchActivity` |
| SHA-256 | `be59b7d62ed1419186108d84b066a1d39e14a34aad6c1058beed9eb447f3e012` |

The queue app does not copy or unpack the official APK into its own binary.
Android treats it as a separately signed package, so merging its compiled
classes, native libraries, resources, or private data would be invalid and
would not preserve either application's installation or signing identity.

During CI, `scripts/add-official-rubika-bridge.py` adds a small, source-level
handoff to the extracted queue source. The app can:

- launch the installed official Rubika app after the user taps the button; and
- share the current, user-entered text draft to the official app for review.

Sharing opens a draft only. The user must choose the destination and confirm
any action in Rubika. The bridge never reads Rubika's session, database,
contacts, messages, APK files, private APIs, or login state. If package
`app.rbmain.a` is absent, the queue app reports that condition and continues to
work independently.

The 4.1.1 APK remains in the connected Drive library as the separately
installable artifact. A device test with that APK installed is still required
to confirm the handoff on the owner's Android version; CI can only compile and
test the bridge.
