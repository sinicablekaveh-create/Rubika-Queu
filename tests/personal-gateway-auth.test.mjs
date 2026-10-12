// Regression: unauthenticated desktop requests must never touch OAuth storage.
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { createServer } from 'node:net';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const adapter = fileURLToPath(new URL('../examples/run-ai-gateway.mjs', import.meta.url));
const probe = createServer();
await new Promise((resolve, reject) => probe.once('error', reject).listen(0, '127.0.0.1', resolve));
const port = probe.address().port;
await new Promise(resolve => probe.close(resolve));

const child = spawn(process.execPath, [adapter], {
  stdio: ['ignore', 'pipe', 'pipe'],
  env: {
    ...process.env,
    AI_GATEWAY_PORT: String(port),
    DESKTOP_GATEWAY_TOKEN: 'desktop-gateway-token-0123456789',
    // Missing file: an early OAuth access makes this test return 502 rather than 401.
    DESKTOP_OAUTH_FILE: join(tmpdir(), 'nonexistent-desktop-oauth-' + process.pid + '.json')
  }
});
let errors = '';
child.stderr.on('data', chunk => { errors += String(chunk); });
const origin = 'http://127.0.0.1:' + port;
const request = (path, bearer, method = 'POST') => fetch(origin + path, {
  method,
  headers: bearer ? { Authorization: 'Bearer ' + bearer } : {},
  ...(method === 'POST'
    ? { body: JSON.stringify({ tool: 'list_devices', arguments: {} }),
        headers: { ...(bearer ? { Authorization: 'Bearer ' + bearer } : {}),
          'Content-Type': 'application/json' } }
    : {})
});

try {
  let unauthorized;
  for (let attempt = 0; attempt < 60; attempt++) {
    if (child.exitCode !== null) throw new Error('Gateway exited: ' + errors);
    try {
      unauthorized = await request('/v1/desktop/call');
      break;
    } catch (_) {
      await new Promise(resolve => setTimeout(resolve, 50));
    }
  }
  assert.ok(unauthorized, 'Gateway did not start: ' + errors);
  assert.equal(unauthorized.status, 401);
  assert.equal(unauthorized.headers.get('Cache-Control'), 'no-store');
  assert.equal((await request('/v1/desktop/call', 'wrong')).status, 401);

  const valid = 'desktop-gateway-token-0123456789';
  assert.equal((await request('/v1/desktop/unknown', valid)).status, 404);
  assert.equal((await request('/v1/desktop/call', valid, 'GET')).status, 405);
  // The authorized route may access the (intentionally missing) OAuth file.
  assert.equal((await request('/v1/desktop/call', valid)).status, 502);
  console.log('Personal desktop gateway authorization regression passed');
} finally {
  child.kill();
}
