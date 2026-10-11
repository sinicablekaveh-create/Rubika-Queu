# Online AI drafting inside Rubika-Queu

## Scope and status
This optional feature adds in-app HTTPS model inference through an **owner-operated
OpenAI-compatible gateway**, alongside the existing ChatGPT browser handoff
and Ollama modes. It is not a connection to the user's ChatGPT subscription,
saved memories, account chats, GitHub apps, plugins, or ChatGPT UI. Neither
the Android app nor this feature can remotely rewrite or install APK code.
Only the current, manually entered draft is transmitted, after a confirmation.

## Setup
1. Deploy `examples/online-ai-gateway-worker.js` on your own trusted HTTPS
   Cloudflare Worker-style environment or adapt it to a secured backend.
2. Set deployment secrets `OPENAI_API_KEY` (provider's secret) and
   `GATEWAY_TOKEN` (an unrelated, long random gateway access token of
   at least 24 characters). Set `OPENAI_MODEL` to an allowed model supported
   by Chat Completions. Configure cost caps, provider usage limits, and rate
   limiting for the gateway; do not expose an unauthenticated public endpoint.
3. In the app, open **دستیار AI برای بهبود متن** and enter the gateway origin
   such as `https://example.workers.dev`, model name matching the gateway's
   allowed model, and the gateway token (not the provider API key).
4. Enter or edit the draft, tap **ویرایش آنلاین متن با هوش مصنوعی**, review the
   consent dialog and read the suggestion inside the app. You may copy it or
   explicitly return it to the composer; a second confirmation is required
   to replace the original text. Nothing is sent to Rubika automatically.

This is a **user-supplied backend** design. The repository has no deployed
online AI service, no API entitlement, no embedded provider key, and does not
claim that a ChatGPT subscription pays for API calls. Provider API billing
is separate from a ChatGPT plan.

## Security model
- TLS certificate and hostname checks are performed by Android; plain HTTP
  and redirects are disallowed.
- Direct provider domains (`openai.com` and subdomains) and `chatgpt.com`
  and its subdomains are rejected as Android gateway endpoints. Neither the
  ChatGPT website nor a provider API host is a user-owned gateway. Provider
  credentials remain on the server.
- Gateway token is entered per session, never committed, logged or stored by
  the app; it can be stolen from a compromised device, so use short-lived
  scoped credentials in production. The example Worker uses a shared token
  and is only a basic integration reference, not a production authentication
  system.
- The app sends only a bounded draft and fixed drafting instructions. No
  Rubika session, contacts, queue data, messages, local discovery cache,
  or file attachments are serialized into the request.
- The gateway and the selected model provider receive the draft. Avoid
  submitting sensitive text without reviewing their privacy policy.
- Response is limited to 64 KiB. Validated suggestion length is at most
  4,200 characters; HTTP redirects, non-200 responses, and truncated outputs
  fail closed. No autonomous application changes or message sends are allowed.

## Developer validation
The build overlay installer `scripts/add-ai-assistant.py` now copies
`AiOnlineClient.java` along with the existing assistant classes.
`tests/discovery-runtime/src/test/java/.../OnlineClientTest.java` checks
request boundaries, malformed replies, HTTPS-only endpoints, token checks,
and rejection of ChatGPT/provider domains and incomplete model replies. Normal CI uses Java tests,
Gradle `lintDebug`, `assembleDebug`, `apksigner` and `zipalign`.
The existing dedicated APK signing key is still required for an upgrade
of an installed differently signed APK.
