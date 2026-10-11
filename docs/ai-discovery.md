# AI group discovery

Open **کشف هوشمند گروه‌ها و تاریخچه** from the main screen. Sign in using the existing account flow. Enter a topic and press **جست‌وجوی خودکار، رتبه‌بندی و ذخیره**. Comma-, Persian-comma-, or semicolon-separated topics are split into up to eight focused searches locally, so a phrase list such as `برق صنعتی، سینی کابل، نردبان کابل` works without an AI server. An empty server field runs the existing discovery engine without AI. To expand the topic, enter a user-owned HTTPS Ollama server and installed model. Confirm sending the query. Only that query is sent to `/api/generate`; no session, contacts, group data or messages are sent to AI. AI failure falls back to the original phrase.

Results come from the existing discovery engine, not generated group identities. Ranking normalizes Persian/Arabic letters and punctuation, matches complete words/phrases rather than arbitrary substrings, and uses the best match across entered topics. AI-suggested terms contribute to the result ranking at lower priority than the terms explicitly entered by the user; the displayed score is ordinal, not a percentage. Unrelated keywords or a known GUID alone no longer count as topical relevance. Scores do not prove group authenticity or permission to send. This screen cannot enqueue messages or join groups. Existing queue, discovery exports, messaging, Bot API and authentication remain available.

The independent `ai-discovery.db` stores up to 1,000 identified groups and 100 recent searches per account. When a group is observed first by public link and later by GUID (or vice versa), the cache reconciles both identities rather than adding another row. Records with different known GUIDs are never fused merely because they share a public link. The in-progress search and progress count survive Activity recreation such as screen rotation, as do the typed fields. Local results and history work without an AI server. Data can be deleted for the signed-in account. Android app backup remains disabled. Cached results can be stale and are displayed as local results.

## Build

Follow the existing CI extraction and overlays, then run `python3 scripts/add-ai-discovery.py`. The installer is repeatable and guards its integration anchor. It copies the tracked discovery overlay into generated Java sources. Run the ranking tests, existing CI regression checks, `lintDebug`, then `assembleDebug`. Debug builds use a fresh debug identity unless the original signing key is supplied; they cannot be assumed to update previously signed APKs.

## HTTPS server contract

POST `/api/generate` with JSON fields `model`, `stream: false`, `format: "json"`, and `prompt`. Return HTTP 200 with `{"response":"{\"terms\":[\"related topic\"]}"}`. At most four short expansions are used, with the original query retained. Redirects are rejected; platform TLS and hostname verification remain enabled. Responses are capped at 64 KiB. No server credentials are persisted by this feature.

## Behavior and boundaries

Searches are started explicitly by the user; the application automatically explores the focused terms, ranks the real results and saves them under the logged-in account. It does not run unattended periodic discovery, join groups, scrape private contacts, or send messages. Stopping a search keeps previously stored local data. A full search that completes is cached; partial results are not saved. Groups without a discoverable GUID or link are not persisted. The source data and result quality depend on the existing discovery engine and its accessible sources.

## Search relevance v2

Typed topics are normalized and deduplicated by their Persian/Arabic spelling and punctuation before search. The existing user-triggered flow still limits typed phrases to eight and retains a maximum of 12 distinct combined phrases when an optional AI server is used. Up to four **valid distinct** Ollama suggestions are included; invalid, repeated or malformed items do not use a suggestion slot. User-topic matches rank above matches found only through AI suggestions. The results screen explicitly labels direct, suggested and unmatched results, along with a count of results having a topical match. AI phrases expand recall, but all group identities continue to come from the real discovery engine.

## Relevance v2 review fixes

- Search de-duplicates equivalent Persian/Arabic spellings but preserves punctuation variants when the underlying discovery engine compares raw text. A valid 81–300 character single user topic is not dropped by the 80-character AI suggestion cap.
- The original query echoed by Ollama never consumes one of the 12 search slots; up to four distinct suggestions can be searched with eight typed topics.
- Schema v2 upgrades the existing local cache without deleting rows and saves accepted AI search suggestions alongside each account's search history. Local results use the requested query's saved suggestions or the latest saved query if the input is blank, retaining their previous relevance labels without contacting AI again.
- When a public link is associated with two different known GUIDs, a new link-only observation is ambiguous and is not assigned to either group's saved record. Search history still records the attempt.
