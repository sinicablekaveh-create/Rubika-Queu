/**
 * Optional Cloudflare Worker-style example for an authenticated AI gateway.
 * Configure OPENAI_API_KEY and GATEWAY_TOKEN as deployment secrets.
 * Configure OPENAI_MODEL (default gpt-4.1-mini) as a non-secret variable.
 * This file is an EXAMPLE, not deployed by the Android build.
 */
export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname !== "/v1/chat/completions" || request.method !== "POST") {
      return new Response("Not found", { status: 404 });
    }
    if (!env.OPENAI_API_KEY || !env.GATEWAY_TOKEN || env.GATEWAY_TOKEN.length < 24) {
      return new Response("Gateway not configured", { status: 503 });
    }
    if (request.headers.get("Authorization") !== "Bearer " + env.GATEWAY_TOKEN) {
      return new Response("Unauthorized", { status: 401 });
    }
    if (Number(request.headers.get("Content-Length") || 0) > 16000) {
      return new Response("Request too large", { status: 413 });
    }
    let input;
    try {
      const raw = await request.text();
      if (raw.length > 16000) return new Response("Request too large", { status: 413 });
      input = JSON.parse(raw);
    } catch (_) {
      return new Response("Invalid JSON", { status: 400 });
    }
    const userMessage = input?.messages?.[1];
    const draft = userMessage?.role === "user" && typeof userMessage.content === "string"
      ? userMessage.content.trim() : "";
    if (!Array.isArray(input?.messages) || input.messages.length !== 2 ||
        typeof draft !== "string" || draft.length < 1 || draft.length > 3500 ||
        input.stream !== false) {
      return new Response("Invalid drafting request", { status: 400 });
    }
    const payload = {
      model: env.OPENAI_MODEL || "gpt-4.1-mini",
      store: false,
      stream: false,
      messages: [
        { role: "developer", content: "Rewrite only the user's Persian draft clearly. Do not invent facts, perform actions or expose credentials. Return only revised text." },
        { role: "user", content: draft }
      ]
    };
    let response;
    try {
      response = await fetch("https://api.openai.com/v1/chat/completions", {
        method: "POST",
        headers: {
          "Authorization": "Bearer " + env.OPENAI_API_KEY,
          "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
      });
    } catch (_) {
      return new Response("Model service unavailable", { status: 502 });
    }
    if (!response.ok) {
      // Never relay provider error bodies: they may contain internal metadata.
      return new Response("Model service error", { status: 502 });
    }
    // The Android client limits incoming bytes and rejects invalid model responses.
    return new Response(response.body, {
      status: 200,
      headers: { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" }
    });
  }
};
