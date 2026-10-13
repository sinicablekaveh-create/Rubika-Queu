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

## Online development, web research and GitHub integration

The development panel uses the same HTTPS **gateway origin** as drafting but a
**separate** `DEV_GATEWAY_TOKEN`. Chat/research, workflow dispatch, and workflow
status are handled respectively by:
`POST /v1/dev/chat`, `POST /v1/dev/propose`, `GET /v1/dev/status`.

The gateway example uses the OpenAI **Responses API** with optional
`web_search` for researching programming questions. Its result is advice:
it does **not** run code on the phone or copy a ChatGPT web session.
Development proposals use a GitHub fine-grained token held on the server,
which dispatches `.github/workflows/ai-development.yml` on `main`; this
workflow can open a **draft PR**, but does not merge or publish automatically.

### Required owner-operated secrets / configuration

Deploy the Worker example to a trusted HTTPS host. Configure secrets
`OPENAI_API_KEY`, `GATEWAY_TOKEN`, `DEV_GATEWAY_TOKEN`, and
`GITHUB_WORKFLOW_TOKEN` in the deployment's secret manager, **never**
in Android source or GitHub commits. Set `GITHUB_REPOSITORY` to
`sinicablekaveh-create/Rubika-Queu`. The GitHub credential should be scoped
to this single repository with **Actions: write** permission only. Configure
`OPENAI_RESEARCH_MODEL` to an available model that supports Responses web
search; check model availability and API quota before use.

Also configure the repository's GitHub Actions secret `OPENAI_API_KEY` for
the read-only Codex proposal workflow, and enable the repository option for
Actions to create pull requests. These two setup steps cannot be completed by
committing source code. See `docs/AUTOMATED_DEVELOPMENT.md`. The workflow must
be reviewed and merged into `main` before the dispatch button can work.

In Android, supply the gateway host and its relevant token. Each remote
research and GitHub proposal operation requires an explicit confirmation.
Replies remain in the assistant view; they never replace drafts or send
Rubika messages automatically.

### What is not connected

This is **not** direct control of `chatgpt.com` or access to ChatGPT saved
chats, account plugins, connected services, or the entire phone. Those
systems require their **own** supported OAuth/connector APIs and independent
authorization. Likewise, no Supabase instance or remote computer is
implicitly provisioned by installing the APK.

Production gateways need per-user authentication, rate and cost limits,
audit logs that omit secrets, replay prevention, and explicit connector
allowlists. A single shared development token is only a reference design.

## Developer validation
The build overlay installer `scripts/add-ai-assistant.py` now copies
`AiOnlineClient.java` along with the existing assistant classes.
`tests/discovery-runtime/src/test/java/.../OnlineClientTest.java` checks
request boundaries, malformed replies, HTTPS-only endpoints, token checks,
and rejection of ChatGPT/provider domains and incomplete model replies. Normal CI uses Java tests,
Gradle `lintDebug`, `assembleDebug`, `apksigner` and `zipalign`.
The existing dedicated APK signing key is still required for an upgrade
of an installed differently signed APK.

## Online AI development chat, web research and GitHub proposals

After deploying the trusted HTTPS gateway, the Android AI assistant also exposes
**گفتگو با AI و تحقیق در وب**, **ثبت درخواست توسعه خودکار در GitHub**, and
**بررسی آخرین وضعیت توسعه در GitHub**. These controls use the same
`onlineGateway` origin entered above and a **separate** `DEV_GATEWAY_TOKEN`
credential entered into the developer input. The user must approve each research
submission and each development workflow dispatch. No message queue action is
attached to these buttons.

Additional server environment configuration:

- `DEV_GATEWAY_TOKEN` (secret): a separate long random access token, at least
  24 characters, unrelated to the drafting `GATEWAY_TOKEN` and the OpenAI key.
  Use a short-lived scoped token and appropriate rate limiting in production.
- `OPENAI_API_KEY` (secret): existing provider API key stored **only** in the
  HTTPS gateway. The developer chat uses the official Responses API with
  `web_search` for up-to-date publicly available information.
- `OPENAI_RESEARCH_MODEL` (optional): model supported by the Responses API
  and the `web_search` tool; the example defaults to `gpt-5.4`.
- `GITHUB_REPOSITORY` (non-secret): must be exactly
  `sinicablekaveh-create/Rubika-Queu`.
- `GITHUB_WORKFLOW_TOKEN` (secret): a GitHub App installation token or
  fine-grained GitHub token scoped only to **Actions: write** on that repository.
  It must stay on the gateway; it is never entered in the Android application.

Before using the **development request** button, review and merge the PR
that adds `.github/workflows/ai-development.yml` to `main`; enable Actions to
create PRs and provision the GitHub Actions secret `OPENAI_API_KEY`.
That Actions workflow generates a *draft* PR only. It cannot merge it, bypass
review, install the APK, or grant access to the phone. It might incur API/CI
costs. Check its run link and CI status before deciding to merge.

Developer gateway endpoints:

| Path | Method | Action |
| --- | --- | --- |
| `/v1/dev/chat` | POST | Chat with an online development adviser. `use_web: true` enables OpenAI web search. |
| `/v1/dev/propose` | POST | Dispatch only the preconfigured `ai-development.yml` on `main` with a bounded manual task. |
| `/v1/dev/status` | GET | Read status of the most recent manually-dispatched development workflow; may be someone else's run if the repository is shared. |

These endpoints **do not** sync ChatGPT account memories, connected ChatGPT
plugins, private files, calendar, Google credentials or phone data. Each additional
integration (GitHub, Firecrawl, Exa, Linear, Canva, Remote Desktop, MCP providers)
requires its own authenticated connection and separate narrowly scoped tool access.
To connect further integrations, use an owner-controlled backend with OAuth or
remote MCP servers and approval policies; do not copy browser session cookies or
API keys to Android. Google/web research uses a supported public web-search tool,
not unrestricted access to a personal Google account.

The gateway example is not deployed by this repository. The backend is a
starting point, not a production identity system: add real per-user authentication,
abuse prevention, budgets, audit logs that avoid sensitive content, and
approval enforcement before exposing it beyond trusted testers.

## Remote Desktop Commander bridge

The optional desktop controls use `POST /v1/desktop/call` on the same owner-operated
HTTPS gateway, authenticated with a **separate** `DESKTOP_GATEWAY_TOKEN` (at least
24 random characters). The gateway forwards only `list_devices`, `list_directory`,
`read_file`, `write_file`, and `start_process` to the official Remote Desktop
Commander MCP endpoint `https://mcp.desktopcommander.app/mcp`. Each operation
requires a separate confirmation in Android. File writes replace the named
file; command execution uses the selected device's shell. Device IDs can be
obtained with the list button.

For standalone Worker deployment, set `DESKTOP_MCP_ACCESS_TOKEN` as a deployment
secret after separately authorizing your gateway as an OAuth client for Remote
Desktop Commander. This is an OAuth
access token for that MCP service, **not** the token of the ChatGPT connector in
this conversation. For a personal Windows server, the separate OAuth helper and
Node adapter in `docs/PERSONAL_GATEWAY_DEPLOYMENT.md` authorize and refresh the
token outside the Worker. The standalone Worker does not implement OAuth login
or token refresh. Do not copy
ChatGPT session credentials or try to extract a connector token. The bridge is
disabled when either desktop secret is absent. The desktop OAuth token and
gateway token stay on the server; Android receives only the separate gateway
token. The Worker must be deployed and the device agent must remain online.

This bridge grants powerful access to the selected computer. Give it only to
trusted users, add real per-user authentication and server-side approvals before
shared use, and keep the gateway private. The example limits request/response
size and available tool names, but a valid desktop gateway token can still
read or write files and run commands directly. The Android confirmation is a
user interface check, not a server-side authorization boundary.
