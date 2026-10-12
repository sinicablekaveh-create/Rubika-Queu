# Personal HTTPS gateway on the connected Windows computer

The Android app needs a public HTTPS origin. GitHub stores source and runs
workflows; it does not keep this gateway process running. The connected Windows
computer can host the gateway after it has a domain, a valid TLS certificate,
and an HTTPS reverse proxy. The example binds HTTP to `127.0.0.1:8787` only.

## Before starting

1. Review and merge the gateway and development-workflow changes to `main`.
2. Point a domain such as `ai.example.com` to the computer's reachable public
   address. Configure the router/firewall to accept HTTPS on port 443. If the
   computer cannot accept inbound connections, use an owner-controlled HTTPS
   tunnel with a stable hostname instead.
3. Install a TLS reverse proxy (for example, Caddy) and route that hostname to
   `127.0.0.1:8787`. Example Caddyfile:

   ```caddyfile
   ai.example.com {
       reverse_proxy 127.0.0.1:8787
   }
   ```

4. Install Node.js 20 or newer and clone this repository on the computer.
5. Configure `OPENAI_API_KEY`, `GATEWAY_TOKEN`, `DEV_GATEWAY_TOKEN`,
   `DESKTOP_GATEWAY_TOKEN`, `GITHUB_WORKFLOW_TOKEN`, and `GITHUB_REPOSITORY`
   in the service account's protected environment. The three gateway tokens
   must be different random values of at least 24 characters. Set
   `GITHUB_REPOSITORY=sinicablekaveh-create/Rubika-Queu`. Give the GitHub
   token only the repository Actions permissions needed to dispatch and read
   workflow runs. Never commit or send these values in chat.

## Authorize Remote Desktop Commander

The ChatGPT connection does not authorize a separate server. On the Windows
computer, create a private folder outside the repository, set
`DESKTOP_OAUTH_FILE` to a file path in it, and run:

```powershell
node examples/authorize-desktop.mjs
```

Open the URL printed by the script in a browser **on that computer**, sign in,
and approve access. The script uses OAuth authorization code with PKCE and a
loopback callback, then saves renewable credentials to the chosen private file.
Restrict that file to the gateway service account. The script refuses to
overwrite an existing token file; move or delete an old file deliberately
before reauthorizing. If the OAuth provider does not issue a refresh token,
authorization will fail rather than leave an expiring connection.

## Start and test

With the same `DESKTOP_OAUTH_FILE` and server secrets available to the process,
run `node examples/run-ai-gateway.mjs`. Keep it running under a Windows service
manager. The Node adapter refreshes the Remote Desktop OAuth access token when
needed and forwards requests to the gateway Worker code. It does not expose the
local HTTP port beyond loopback.

From a separate computer, check that the HTTPS hostname responds. An
unauthenticated `POST /v1/desktop/call` should return HTTP 401. In Android,
enter the HTTPS origin and the separate desktop gateway token, then use
**فهرست دستگاه‌ها**. Select the returned device ID before reading or writing
files or running commands. Every operation asks for confirmation.

The Android build must include the assistant overlay from this PR. GitHub
Actions must be operational before relying on its APK build result. The AI
development workflow also needs the Actions `OPENAI_API_KEY` secret and the
repository setting that allows Actions to create pull requests.

The bridge is intended for one trusted owner. A shared static gateway token
permits broad file and command access; Android's confirmation dialog cannot
stop a direct HTTP client holding that token. Do not share the token or expose
this example as a public multi-user service without server-side identity,
approval, rate limits, and audit controls.
