# Rubika-Queu native RTL dashboard — review-only

Source design: https://www.figma.com/design/xQqAXnBjRjLEX9hAOShy24

The Android implementation uses **native `Activity`, `LinearLayout`, `ScrollView`, `EditText`, `CheckBox`, and `AlertDialog` widgets**. It does not embed a WebView, HTML renderer, browser session, Rubika code, or messaging automation. The dashboard has five tabs: home, queue preview, public discovery, reports, and settings. Copy is Persian and views use RTL.

## Integration strategy

The application builds its Java source from an audited ZIP during GitHub Actions. After existing overlays finish, `scripts/add-native-dashboard.py` copies `overrides/native-ui/NativeDashboardActivity.java` into the extracted app tree and changes exactly one launcher intent filter from `MainActivity` to `NativeDashboardActivity`. The legacy `MainActivity` remains present, unmodified, and can be opened from the dashboard for actual, user-controlled queue operations. Existing dedicated discovery and ChatGPT-report Activities can also be opened. Network, sending, and session code are neither copied nor altered by this feature.

The discovery-on-queue-start switch deliberately reads and writes the existing `discovery/auto_on_queue_start` preference. Other dashboard forms, counters, status indicators, search results and manual-review items are labeled **demo**; they never start a queue or modify user message state. Queue preview requires a nonempty draft and interval within 30–3600 seconds; its result is a local alert only.

## Manual device test plan

1. On a **test device** with discardable app data, install a properly signed test candidate. Never uninstall the owner's production app to make a mismatched APK install.
2. Confirm the five bottom tabs, Persian RTL flow, scrolling, visible labels, text fields, and 48dp+ touch targets. Rotate, background, resume, and verify typed draft and selected page survive rotation where supported.
3. From Home/Queue, open the original MainActivity; from Search, open AiDiscoveryActivity; from Reports, open DevelopmentReportActivity. Ensure Android back returns to the dashboard.
4. Toggle the shared auto-discovery preference in the new Search/Settings tabs; verify the legacy Groups/Contacts screen sees the same setting. Never initiate queue delivery without separate user approval.
5. Verify dashboard preview, fake search, and fake manual review make **no outbound Rubika messages and no private-contact reads**.
6. Run queue policy unit tests, installer tests, lint and Android build in CI. Validate the APK `applicationId` and monotonically increasing `versionCode`. For any in-place update, compare the actual installed signing certificate and run a non-destructive on-device upgrade test before publishing.

## Release restrictions

Candidate source version: `versionCode=17` (proposed), previous source `16`, known owner-installed baseline `15`; package remains `ir.sinicable.rubikaqueue`. Original keystore and signing configuration are **not modified**. PR CI does not receive signing secrets, so its debug artifact is **not proof of updatability**. Do not merge or publish an APK without separate owner approval.