// Free-tier, opt-in Persian message drafting adapter for Rubika-Queu.
// No GitHub or Rubika messaging operations and no paid provider keys.
const MODEL = '@cf/meta/llama-3.1-8b-instruct-fast';
const MAX_REQUEST_BYTES = 12000;
const MAX_DRAFT_CHARS = 3500;
const MAX_SUGGESTION_CHARS = 4200;

function json(value, status = 200) {
  return new Response(JSON.stringify(value), {
    status,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'no-store',
      'X-Content-Type-Options': 'nosniff'
    }
  });
}

async function boundedJson(request) {
  if (Number(request.headers.get('Content-Length') || 0) > MAX_REQUEST_BYTES) {
    throw new RangeError('Request too large');
  }
  if (!request.body) throw new SyntaxError('Missing request body');
  const reader = request.body.getReader();
  let length = 0;
  const chunks = [];
  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      length += value.byteLength;
      if (length > MAX_REQUEST_BYTES) throw new RangeError('Request too large');
      chunks.push(value);
    }
  } finally {
    await reader.cancel().catch(() => {});
  }
  const bytes = new Uint8Array(length);
  let offset = 0;
  for (const chunk of chunks) {
    bytes.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(bytes));
}

export default {
  async fetch(request, env) {
    const { pathname } = new URL(request.url);
    if (pathname === '/health' && request.method === 'GET') {
      return json({ status: 'ok', service: 'rubika-free-drafting' });
    }
    if (pathname !== '/v1/chat/completions') return json({ error: 'Not found' }, 404);
    if (request.method !== 'POST') return json({ error: 'Method not allowed' }, 405);
    if (!env?.AI || typeof env.AI.run !== 'function' ||
        typeof env.GATEWAY_TOKEN !== 'string' || env.GATEWAY_TOKEN.length < 24) {
      return json({ error: 'Gateway is not configured' }, 503);
    }
    if (request.headers.get('Authorization') !== 'Bearer ' + env.GATEWAY_TOKEN) {
      return json({ error: 'Unauthorized' }, 401);
    }
    let input;
    try {
      input = await boundedJson(request);
    } catch (err) {
      return json({ error: err instanceof RangeError ? 'Request too large' : 'Invalid JSON' },
        err instanceof RangeError ? 413 : 400);
    }
    const messages = input?.messages;
    const draft = Array.isArray(messages) && messages.length === 2 &&
      messages[0]?.role === 'system' && messages[1]?.role === 'user' &&
      typeof messages[1].content === 'string' ? messages[1].content.trim() : '';
    if (!draft || draft.length > MAX_DRAFT_CHARS || input?.stream !== false ||
        input?.store === true) {
      return json({ error: 'Invalid drafting request' }, 400);
    }

    let result;
    try {
      result = await env.AI.run(MODEL, {
        messages: [
          { role: 'system', content: 'Improve ONLY the user-supplied Persian message draft. Return only the edited message, with no invented facts, instructions, secrets, or actions. Never send any message.' },
          { role: 'user', content: draft }
        ],
        stream: false,
        max_tokens: 768
      });
    } catch (_) {
      return json({ error: 'Free AI unavailable or daily allowance exhausted' }, 503);
    }
    const answer = typeof result?.response === 'string' ? result.response.trim() : '';
    if (!answer || answer.length > MAX_SUGGESTION_CHARS) {
      return json({ error: 'Invalid model reply' }, 502);
    }
    return json({
      model: 'rubika-free',
      choices: [{ index: 0, message: { role: 'assistant', content: answer }, finish_reason: 'stop' }]
    });
  }
};
