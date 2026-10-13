# Free online AI for Rubika-Queu (Cloudflare Workers AI)

Status: prepared in source; NOT DEPLOYED. This is a **single-owner, opt-in Persian drafting** prototype. Windows/Remote Desktop and an OpenAI API key are not needed.

## Included

- Worker: `free-ai-worker/index.mjs` (authenticated `POST /v1/chat/completions`, `GET /health`).
- Cloudflare binding: `free-ai-worker/wrangler.jsonc` with `AI` binding and supported `@cf/meta/llama-3.1-8b-instruct-fast` model.
- Validation: `node --test tests/free-ai-worker.test.mjs` (8 mocked test cases, no paid model calls).
- It returns the OpenAI-compatible completion response expected by the existing Android `AiOnlineClient` and does **not** send Rubika messages or initiate coding tasks.

## Free plan and cost safeguards

Cloudflare Workers Free currently allows up to 100,000 requests/day; Workers AI Free currently allows up to 10,000 neurons/day. The Workers Free plan **stops/fails requests** after the free inference allowance rather than purchasing more neurons. Limits, availability and terms can change. Do NOT upgrade to Workers Paid, enable prepaid AI Gateway credits, or enable paid models without new approval. This free model needs no separate OpenAI API key or ChatGPT Plus API credits.

Official references:
- https://developers.cloudflare.com/workers-ai/platform/pricing/
- https://developers.cloudflare.com/workers/platform/limits/
- https://developers.cloudflare.com/workers-ai/configuration/bindings/
- https://developers.cloudflare.com/workers/ci-cd/builds/git-integration/github-integration/
- https://developers.cloudflare.com/workers/ci-cd/builds/advanced-setups/

## Phone-only setup after review and merge

1. The owner signs in to https://dash.cloudflare.com/ and chooses **Workers Free**. Account setup/GitHub authorization is a user action; a GitHub admin grant does not provide Cloudflare access.
2. In **Workers & Pages**, connect the GitHub repository `sinicablekaveh-create/Rubika-Queu`. The Cloudflare Workers Builds root directory is `free-ai-worker`; the deploy command is `npx wrangler deploy`. Deploy production from the `main` branch *only after PR review and merge*. Do not deploy production secrets to preview branches.
3. Confirm the Worker has AI binding `AI` from its Wrangler configuration.
4. In the deployed Worker's **Settings → Variables and Secrets**, create `GATEWAY_TOKEN` as a **secret** with at least 24 random characters, never as repository code or a message in ChatGPT. No `OPENAI_API_KEY` is needed. Rotate the token if disclosed.
5. Visit your Worker origin `/health` (example: `https://rubika-queu-free-ai.<your-subdomain>.workers.dev/health`). HTTP 200 and `status: ok` check only the Worker routing, *not* model inference.
6. In the existing Rubika-Queu Android assistant **دستیار AI برای بهبود متن**, enter the **HTTPS origin only**, model label `rubika-free`, and the owner-only `GATEWAY_TOKEN` for this session. The gateway pins its own model regardless of the Android label.
7. Enter a nonsensitive Persian draft, approve the consent prompt, review the suggestion, and approve any return to the composer. No Rubika message is sent automatically.

## Security and boundaries

- Never store or send Rubika credentials, account sessions, recipient lists, secrets or sensitive customer messages to this model. The chosen provider processes submitted drafts.
- The basic bearer token is for a private prototype, not production multi-user authentication. Add server-side user identity, rate limits and auditing before public access.
- Requests are authenticated, size bounded, and never cached. Unknown development and desktop endpoints return HTTP 404. No cloud actions are exposed to a client solely because GitHub is connected.
- Current installed Rubika-Queu is versionCode 14 signed by SHA-256 `fdb827fa66b05309a2e7a759bc03d9f22355b54cd74c63e74a5d889823087026`. This hosting change does not provide a matching signing private key. Do **not** uninstall, clear user data or install a NOT-UPDATABLE debug APK.
- No deployment URL, cloud account activation, or end-to-end Android/live messaging result is claimed until the owner connects Cloudflare and tests it.

## Next checkpoint

Review this PR and CI. Connect the Cloudflare account from the phone and deploy from reviewed source; verify `/health`, authorization failures, and one consented nonsensitive inference. Leave API-enabled automatic code proposals and Rubika automation out of the free-first milestone.
