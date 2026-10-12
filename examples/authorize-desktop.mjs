// One-time, owner-run OAuth authorization for the local gateway process.
// Set DESKTOP_OAUTH_FILE to a private path outside the repository before running.
import { createServer } from 'node:http';
import { createHash, randomBytes } from 'node:crypto';
import { writeFile } from 'node:fs/promises';

const file = process.env.DESKTOP_OAUTH_FILE;
if (!file) throw new Error('Set DESKTOP_OAUTH_FILE to a private path outside the repository');
const origin = 'https://mcp.desktopcommander.app';
const metadata = await (await fetch(origin + '/.well-known/oauth-authorization-server', {
  signal: AbortSignal.timeout(15000), redirect: 'error'
})).json();
for (const endpoint of [metadata.authorization_endpoint, metadata.token_endpoint,
  metadata.registration_endpoint]) {
  if (typeof endpoint !== 'string' || !endpoint.startsWith(origin + '/'))
    throw new Error('Unexpected Desktop Commander OAuth endpoint');
}
const verifier = randomBytes(32).toString('base64url');
const challenge = createHash('sha256').update(verifier).digest('base64url');
const state = randomBytes(24).toString('base64url');
const callback = 'http://127.0.0.1:8765/callback';
const registration = await fetch(metadata.registration_endpoint, {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    client_name: 'Rubika AI Gateway (owner operated)',
    redirect_uris: [callback],
    grant_types: ['authorization_code', 'refresh_token'],
    response_types: ['code'], token_endpoint_auth_method: 'none'
  }), signal: AbortSignal.timeout(15000), redirect: 'error'
});
if (!registration.ok) throw new Error('Desktop Commander OAuth registration failed');
const client = await registration.json();
if (!client.client_id) throw new Error('OAuth registration returned no client ID');
const authorize = new URL(metadata.authorization_endpoint);
for (const [key, value] of Object.entries({
  response_type: 'code', client_id: client.client_id, redirect_uri: callback,
  code_challenge: challenge, code_challenge_method: 'S256', state,
  scope: 'mcp:tools desktop:auth', resource: origin
})) authorize.searchParams.set(key, value);

const server = createServer(async (request, response) => {
  try {
    const url = new URL(request.url, callback);
    if (url.pathname !== '/callback' || url.searchParams.get('state') !== state ||
        !url.searchParams.get('code')) {
      response.writeHead(400).end('Authorization failed');
      return;
    }
    const exchange = await fetch(metadata.token_endpoint, {
      method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'authorization_code', code: url.searchParams.get('code'),
        redirect_uri: callback, client_id: client.client_id,
        code_verifier: verifier, resource: origin
      }), signal: AbortSignal.timeout(15000), redirect: 'error'
    });
    if (!exchange.ok) throw new Error('Desktop Commander token exchange failed');
    const tokens = await exchange.json();
    if (!tokens.access_token || !tokens.refresh_token)
      throw new Error('OAuth response did not include renewable credentials');
    await writeFile(file, JSON.stringify({
      client_id: client.client_id, token_endpoint: metadata.token_endpoint,
      access_token: tokens.access_token, refresh_token: tokens.refresh_token,
      expires_at: Date.now() + Math.max(60, Number(tokens.expires_in) || 3600) * 1000
    }), { flag: 'wx', mode: 0o600 });
    response.writeHead(200, { 'Content-Type': 'text/plain' }).end('Desktop Commander connected. You may close this tab.');
    process.stdout.write('Desktop Commander OAuth authorization saved.\n');
  } catch (error) {
    response.writeHead(500).end('Authorization failed; check the gateway terminal.');
    process.stderr.write(error.message + '\n');
  } finally { server.close(); }
});
server.listen(8765, '127.0.0.1', () => {
  process.stdout.write('Open this URL in a browser on this computer and approve access:\n' + authorize + '\n');
});
setTimeout(() => server.close(), 300000).unref();
