# Automatic discovery when the queue starts

The main **گروه‌ها و شماره تماس‌ها** screen has an enabled-by-default option to run the existing read-only group/public-business-contact discovery pass when the message queue starts. The pass uses the saved search terms and the existing group/contact checkboxes. Turn off **شروع خودکار جست‌وجوی گروه و تماس عمومی با شروع صف** to disable it.

## Execution order

The queue service holds the existing operation gate, so the new integration runs the existing discovery engine on the queue worker before delivery begins. This prevents simultaneous API calls and avoids changing the queue contents, recipient order, message body, or send interval. The queue foreground notification reports discovery progress and retains the queue's stop action. Stopping the queue interrupts discovery and preserves results already saved. A non-rate-related discovery failure is recorded in the queue status and does not prevent the existing queue from continuing. An explicit Rubika rate-limit or authentication error stops the queue, so the normal sender cannot immediately follow a rate rejection with another request.

Search configuration and output remain in the existing `discovery` preferences and `DiscoveryStore`. If an Excel output document was selected earlier, the completed discovery pass refreshes it. The finder searches account groups, public Rubika search results, and configured public pages. It does not read private member lists or extract private contacts, join groups, or send messages.

## Manual test

1. Install a test build only on a test device whose app data can be discarded; this PR does not include an APK.
2. Sign in and prepare a small queue whose sending is authorized. Set discovery terms to one short test phrase and leave the automatic option enabled.
3. Start the queue. Confirm the notification reports discovery progress first, the app's discovery result count updates, and queue delivery starts after the discovery pass completes.
4. Repeat with the automatic option disabled. Confirm the queue starts without the discovery status changing.
5. Enable automatic discovery, start the queue, then tap **توقف** in the queue notification during discovery. Confirm no subsequent queue item is dispatched and saved findings remain visible.
6. Simulate an unavailable discovery source. Confirm an error is recorded and the original queue continues.

CI runs the pure-Java preference policy tests and applies the integration installer to the audited Android source bundle before lint and APK assembly. Those checks do not replace a device test against Rubika or an in-place signing-key upgrade test.
