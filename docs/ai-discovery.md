# AI group discovery

Open **کشف هوشمند گروه‌ها و تاریخچه** from the main screen. Sign in using the existing account flow. Enter a topic and search; an empty server field runs the original discovery engine without AI. To expand the topic, enter a user-owned HTTPS Ollama server and installed model. Confirm sending the query. Only that query is sent to `/api/generate`; no session, contacts, group data or messages are sent to AI. AI failure falls back to the original phrase.

Results come from the existing discovery engine, not generated group identities. Ranking normalizes Persian/Arabic letters and scores exact, phrase and token matches. Scores do not prove group authenticity or permission to send. This screen cannot enqueue messages or join groups. Existing queue, discovery exports, messaging, Bot API and authentication remain available.

The independent `ai-discovery.db` stores up to 1,000 identified groups and 100 recent searches per account. Local results and history work without an AI server. Data can be deleted for the signed-in account. Android app backup remains disabled. Cached results can be stale and are displayed as local results.

## Build

Follow the existing CI extraction and overlays, then run `python3 scripts/add-ai-discovery.py`. The installer is repeatable and guards its integration anchor. It copies the tracked discovery overlay into generated Java sources. Run the ranking tests, existing CI regression checks, `lintDebug`, then `assembleDebug`. Debug builds use a fresh debug identity unless the original signing key is supplied; they cannot be assumed to update previously signed APKs.

## HTTPS server contract

POST `/api/generate` with JSON fields `model`, `stream: false`, `format: "json"`, and `prompt`. Return HTTP 200 with `{"response":"{\"terms\":[\"related topic\"]}"}`. At most four short expansions are used, with the original query retained. Redirects are rejected; platform TLS and hostname verification remain enabled. Responses are capped at 64 KiB. No server credentials are persisted by this feature.
