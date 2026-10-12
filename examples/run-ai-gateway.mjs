// Run the Worker example on a personal server, behind an HTTPS reverse proxy.
// Bind only to loopback. Never expose this HTTP port directly to the Internet.
import { createServer } from 'node:http';
import { readFile, writeFile, rename } from 'node:fs/promises';
import { Readable } from 'node:stream';

const source = await readFile(new URL('./online-ai-gateway-worker.js', import.meta.url), 'utf8');
const worker = (await import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'))).default;
const port = Number(process.env.AI_GATEWAY_PORT || 8787);
if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('Invalid AI_GATEWAY_PORT');

let refreshInFlight;
async function desktopToken() {
  if (!process.env.DESKTOP_OAUTH_FILE) return process.env.DESKTOP_MCP_ACCESS_TOKEN || '';
  if (refreshInFlight) return refreshInFlight;
  refreshInFlight = (async () => {
    const file = process.env.DESKTOP_OAUTH_FILE;
    const tokens = JSON.parse(await readFile(file, 'utf8'));
    if (tokens.access_token && Number(tokens.expires_at) > Date.now() + 60000)
      return tokens.access_token;
    if (!tokens.refresh_token || !tokens.client_id || !tokens.token_endpoint)
      throw new Error('Desktop OAuth authorization is incomplete');
    const body = new URLSearchParams({
      grant_type: 'refresh_token', refresh_token: tokens.refresh_token,
      client_id: tokens.client_id, resource: 'https://mcp.desktopcommander.app'
    });
    const reply = await fetch(tokens.token_endpoint, {
      method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body, signal: AbortSignal.timeout(15000), redirect: 'error'
    });
    if (!reply.ok) throw new Error('Desktop OAuth refresh failed');
    const next = await reply.json();
    if (!next.access_token) throw new Error('Desktop OAuth refresh returned no access token');
    const updated = {
      ...tokens, access_token: next.access_token,
      refresh_token: next.refresh_token || tokens.refresh_token,
      expires_at: Date.now() + Math.max(60, Number(next.expires_in) || 3600) * 1000
    };
    const temporary = file + '.tmp';
    await writeFile(temporary, JSON.stringify(updated), { mode: 0o600 });
    await rename(temporary, file);
    return updated.access_token;
  })();
  try { return await refreshInFlight; }
  finally { refreshInFlight = undefined; }
}

createServer(async (incoming, outgoing) => {
  try {
    const chunks = [];
    let bytes = 0;
    for await (const chunk of incoming) {
      bytes += chunk.length;
      if (bytes > 20000) {
        outgoing.writeHead(413).end('Request too large');
        return;
      }
      chunks.push(chunk);
    }
    const url = new URL(incoming.url || '/', 'https://gateway.invalid');
    if (url.origin !== 'https://gateway.invalid') {
      outgoing.writeHead(400).end('Invalid request target');
      return;
    }
    const request = new Request(url, {
      method: incoming.method, headers: incoming.headers,
      ...(['GET', 'HEAD'].includes(incoming.method) ? {} : { body: Buffer.concat(chunks) })
    });
    const env = { ...process.env };
    if (url.pathname.startsWith('/v1/desktop/'))
      env.DESKTOP_MCP_ACCESS_TOKEN = await desktopToken();
    const response = await worker.fetch(request, env);
    outgoing.writeHead(response.status, Object.fromEntries(response.headers));
    if (response.body) Readable.fromWeb(response.body).pipe(outgoing);
    else outgoing.end();
  } catch (_) {
    if (!outgoing.headersSent) outgoing.writeHead(502, { 'Content-Type': 'text/plain', 'Cache-Control': 'no-store' });
    outgoing.end('Gateway request failed');
  }
}).listen(port, '127.0.0.1', () => {
  process.stdout.write(`AI gateway listening on 127.0.0.1:${port}\n`);
});
