# Free AI drafting assistant (v1.3)

## What it does
The assistant is opened **manually** from the "File and message text" screen.
It receives only the text currently visible in the message composer; no phone
numbers, Excel contacts, account credentials, bot tokens, stored messages, or
queue state are provided. It cannot send a Rubika message or change the queue.

Two explicit modes:
1. **ChatGPT handoff:** copies a short drafting request to the Android clipboard,
   then opens https://chatgpt.com/ in a browser. The user pastes and sends it
   in ChatGPT. This is NOT a direct ChatGPT API or in-app ChatGPT integration;
   ChatGPT Free plan usage is subject to limits, and copying to clipboard has
   the normal Android privacy risks. No OpenAI key required.
2. **User-owned Ollama over HTTPS:** the user supplies a HTTPS base URL
   (with a valid TLS certificate) for an Ollama-compatible server and a
   model name; after a separate confirmation, the app sends only draft text
   to POST /api/chat with stream=false and displays the generated suggestion.
   This does not incur per-token commercial API fees, but the user provides
   server hardware, network, and any HTTPS gateway. A default Ollama HTTP
   service is NOT directly accessible because the app rejects plaintext HTTP.
   See https://github.com/ollama/ollama/blob/main/docs/api/introduction.mdx.

Security:
- No automated message sends, no contact syncing, no queue access.
- No third-party keys stored or shipped in the APK.
- HTTPS required; explicit user confirmation before network request.
- Response size and prompt length are bounded; redirects are disabled.
- The assistant offers draft suggestions, not autonomous actions.
- Never publish a personal Ollama endpoint to the public internet without
  authentication, access controls, and valid TLS.

Build:
- Source ZIP remains unchanged and auditable; overlay is applied during CI
  using scripts/add-ai-assistant.py after the Bot API overlays.
- Validation: AiPromptPolicyTests; existing Java tests; lintDebug;
  assembleDebug; apksigner; zipalign.
- Version 1.3-ai-assistant-test (versionCode 13).

OpenAI API requests have separate billing; Sites does not grant a free,
unlimited GPT API. No user account logins are impersonated.

## Optional OpenAI-compatible API gateway
The opt-in third mode uses a user-selected HTTPS gateway and OpenAI-compatible
chat-completions endpoint, plus a model ID and optional bearer token. It accepts
host roots (appends /v1/chat/completions) and /v1 base paths (appends
/chat/completions). No credentials, queue entries, contacts or Rubika sessions
are used. Key fields do not save view state. A separate confirmation is shown
for each request; there is no unattended AI access or Rubika sending.
A ChatGPT subscription does not supply an OpenAI API key. External providers
may charge fees or retain submitted drafts. Redirects are disabled; TLS is
required; response bytes and draft lengths are bounded and checked.
Test via existing AiPromptPolicyTests and CI lintDebug/assembleDebug.
