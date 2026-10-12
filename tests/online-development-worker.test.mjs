import fs from 'node:fs';
import assert from 'node:assert/strict';

const code = fs.readFileSync('examples/online-ai-gateway-worker.js', 'utf8');
const worker = (await import('data:text/javascript;base64,' +
    Buffer.from(code, 'utf8').toString('base64'))).default;
const origin = 'https://gateway.example';
const token = 'development-access-token-0123456789';
const env = {
  OPENAI_API_KEY: 'provider-secret-test',
  OPENAI_RESEARCH_MODEL: 'gpt-5.4',
  DEV_GATEWAY_TOKEN: token,
  GITHUB_REPOSITORY: 'sinicablekaveh-create/Rubika-Queu',
  GITHUB_WORKFLOW_TOKEN: 'github-actions-token-test'
};
const desktopEnv = {
  ...env,
  DESKTOP_GATEWAY_TOKEN: 'desktop-gateway-token-0123456789',
  DESKTOP_MCP_ACCESS_TOKEN: 'desktop-oauth-test'
};
function request(path, data, bearer = token, method = 'POST') {
  const headers = { Authorization: 'Bearer ' + bearer };
  if (method === 'POST') headers['Content-Type'] = 'application/json';
  return new Request(origin + path, {
    method, headers,
    ...(method === 'POST' ? { body: JSON.stringify(data) } : {})
  });
}
async function expectStatus(path, body, status, bearer) {
  const response = await worker.fetch(request(path, body, bearer), env);
  assert.equal(response.status, status);
  return response;
}
assert.equal((await expectStatus('/v1/dev/chat', {prompt:'sample'}, 401, 'bad')).status, 401);
assert.equal((await expectStatus('/v1/dev/chat', {prompt:'x'}, 400)).status, 400);
assert.equal((await expectStatus('/v1/dev/propose', {task:'tiny'}, 400)).status, 400);
assert.equal((await worker.fetch(request('/v1/dev/chat', null, token, 'GET'),env)).status,405);
const originalFetch = globalThis.fetch;
let requests = [];
globalThis.fetch = async (url, options) => {
  requests.push({ url: String(url), options });
  if (String(url) === 'https://api.openai.com/v1/responses') {
    return Response.json({
      status:'completed',
      output:[{type:'message',content:[{type:'output_text',text:'پاسخ تحقیق'}]}]
    });
  }
  if (String(url).endsWith('/dispatches')) return new Response(null, {status:204});
  if (String(url).includes('/runs?')) return Response.json({
    workflow_runs:[{ id:123, status:'completed', conclusion:'success',
      html_url:'https://github.com/sinicablekaveh-create/Rubika-Queu/actions/runs/123'}]
  });
  if (String(url) === 'https://mcp.desktopcommander.app/mcp') {
    const message = JSON.parse(options.body);
    if (message.method === 'initialize') return Response.json({
      jsonrpc:'2.0', id:1, result:{protocolVersion:'2025-06-18',capabilities:{}}
    }, {headers:{'Mcp-Session-Id':'test-session'}});
    if (message.method === 'notifications/initialized') return new Response(null,{status:202});
    if (message.method === 'tools/call') return Response.json({
      jsonrpc:'2.0', id:2, result:{content:[{type:'text',text:'device result'}]}
    });
  }
  throw new Error('Unexpected upstream request: ' + url);
};
try {
  const ai = await worker.fetch(request('/v1/dev/chat', {prompt:'بررسی معماری برنامه',use_web:true}),env);
  assert.equal(ai.status,200);
  assert.equal((await ai.json()).answer,'پاسخ تحقیق');
  const provider = requests.find(r => r.url.includes('api.openai.com/v1/responses'));
  assert.ok(provider);
  const modelPayload = JSON.parse(provider.options.body);
  assert.equal(modelPayload.store,false);
  assert.deepEqual(modelPayload.tools,[{type:'web_search'}]);
  assert.ok(!provider.options.body.includes('github-actions-token-test'));
  assert.ok(!provider.options.body.includes(token));

  const proposed = await worker.fetch(request('/v1/dev/propose',
    {task:'بهبود جستجوی گروه‌های برق و افزودن تست'}),env);
  assert.equal(proposed.status,202);
  assert.equal((await proposed.json()).status,'accepted');
  const dispatch = requests.find(r => r.url.endsWith('/actions/workflows/ai-development.yml/dispatches'));
  assert.ok(dispatch);
  assert.equal(JSON.parse(dispatch.options.body).ref,'main');
  assert.equal(JSON.parse(dispatch.options.body).inputs.task,'بهبود جستجوی گروه‌های برق و افزودن تست');

  const status = await worker.fetch(request('/v1/dev/status',undefined,token,'GET'),env);
  assert.equal(status.status,200);
  assert.equal((await status.json()).conclusion,'success');

  const disabled = await worker.fetch(request('/v1/dev/propose',
    {task:'بهبود جستجوی گروه‌های برق و افزودن تست'}),{...env,GITHUB_WORKFLOW_TOKEN:''});
  assert.equal(disabled.status,503);

  const unauthDesktop = await worker.fetch(request('/v1/desktop/call',
    {tool:'list_devices',arguments:{}}, token), desktopEnv);
  assert.equal(unauthDesktop.status,401);
  const blockedDesktop = await worker.fetch(request('/v1/desktop/call',
    {tool:'kill_process',arguments:{pid:1}},desktopEnv.DESKTOP_GATEWAY_TOKEN),desktopEnv);
  assert.equal(blockedDesktop.status,400);
  const desktop = await worker.fetch(request('/v1/desktop/call',
    {tool:'list_devices',arguments:{}},desktopEnv.DESKTOP_GATEWAY_TOKEN),desktopEnv);
  assert.equal(desktop.status,200);
  assert.equal((await desktop.json()).result.content[0].text,'device result');
  const mcp = requests.filter(r => r.url === 'https://mcp.desktopcommander.app/mcp');
  assert.deepEqual(mcp.map(r => JSON.parse(r.options.body).method),
    ['initialize','notifications/initialized','tools/call']);
  assert.equal(mcp[2].options.headers['Mcp-Session-Id'],'test-session');
  assert.equal(mcp[2].options.headers.Authorization,'Bearer desktop-oauth-test');

  const dangerous = await worker.fetch(request('/v1/dev/propose',
    {task:'بهبود جستجوی گروه‌های برق و افزودن تست'}),{...env,GITHUB_REPOSITORY:'someone/other'});
  assert.equal(dangerous.status,503);

  const legacy = await worker.fetch(request('/v1/chat/completions',
    { messages:[{role:'system',content:'edit'},{role:'user',content:'متن'}], stream:false }), env);
  // Legacy text-drafting route still responds independently of the new dev token.
  assert.equal(legacy.status,503);
} finally {
  globalThis.fetch = originalFetch;
}
console.log('Online developer gateway tests passed');
