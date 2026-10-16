# Full native RTL test app

This candidate uses a native five-tab dashboard with real local queue counts, not fake preview numbers. All actual CSV/XLSX import, login, queue management, discovery, redacted reports and encrypted queue backup remain in the existing activities and require user interaction. The dashboard never sends a Rubika message.

It uses an entirely separate third Android package `ir.sinicable.rubikaqueue.sidecarfull`. The original app `ir.sinicable.rubikaqueue` and installed debug sidecar `ir.sinicable.rubikaqueue.sidecar` and their private data are not changed. The full app is installed empty. Credentials and the 978 pending recipients do not migrate automatically. Keep both installed applications; never uninstall them to install this APK.

CI checks the installer, verifies no direct send code is inserted, lints and builds Android, checks the launcher/package/label, APK signing, and ZIP alignment. Physical-device installation, sign-in, import and sending are not tested by CI and should not be assumed complete. Debug CI signing changes on each run, so do not use this as a production upgrade track.
