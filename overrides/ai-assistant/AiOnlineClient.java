package ir.sinicable.rubikaqueue;

import org.json.JSONArray;
import org.json.JSONObject;
import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Opt-in online drafting through a user-owned, authenticated HTTPS gateway.
 * This is NOT a ChatGPT account/session integration. Provider API keys stay on the server.
 */
public final class AiOnlineClient {
    private static final int MAX_BYTES = 65536;
    private AiOnlineClient() {}

    static String gatewayUrl(String root) {
        String endpoint = AiPromptPolicy.endpoint(root);
        if ("api.openai.com".equalsIgnoreCase(URI.create(endpoint).getHost())) {
            throw new IllegalArgumentException("کلید OpenAI نباید در برنامه استفاده شود؛ سرور واسط خودتان را وارد کنید");
        }
        return endpoint + "/v1/chat/completions";
    }

    static String gatewayToken(String token) {
        String value = token == null ? "" : token.trim();
        if (!value.matches("[A-Za-z0-9._~+=/-]{8,512}") || value.startsWith("sk-")) {
            throw new IllegalArgumentException("توکن دسترسی موقت سرور واسط لازم است؛ کلید OpenAI را وارد نکنید");
        }
        return value;
    }

    static JSONObject request(String model, String draft) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("model", AiPromptPolicy.model(model));
        payload.put("stream", false);
        payload.put("store", false);
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content",
                "You improve a Persian message draft supplied by the user. Reply with only the polished draft. " +
                "Do not invent factual claims, request account credentials or trigger sending."));
        messages.put(new JSONObject().put("role", "user").put("content", AiPromptPolicy.prompt(draft)));
        payload.put("messages", messages);
        return payload;
    }

    static String extract(String json) throws Exception {
        JSONObject payload = new JSONObject(json);
        JSONArray choices = payload.optJSONArray("choices");
        if (choices == null || choices.length() == 0) throw new IOException("پاسخ مدل معتبر نیست");
        JSONObject first = choices.getJSONObject(0);
        if ("length".equals(first.optString("finish_reason"))) {
            throw new IOException("پاسخ مدل ناقص است");
        }
        JSONObject message = first.optJSONObject("message");
        if (message == null || !(message.opt("content") instanceof String)) {
            throw new IOException("متن پاسخ مدل موجود نیست");
        }
        return AiPromptPolicy.suggestion(message.getString("content"));
    }

    public static String complete(String gateway, String model, String token, String draft) throws Exception {
        String url = gatewayUrl(gateway);
        String bearer = gatewayToken(token);
        byte[] body = request(model, draft).toString().getBytes(StandardCharsets.UTF_8);
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(45000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + bearer);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream out = connection.getOutputStream()) { out.write(body); }
            if (connection.getResponseCode() != 200) {
                throw new IOException("سرور واسط پاسخ موفق نداد (HTTP " + connection.getResponseCode() + ")");
            }
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            try (InputStream in = connection.getInputStream()) {
                byte[] buffer = new byte[2048];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException("درخواست متوقف شد");
                    if (result.size() + n > MAX_BYTES) throw new IOException("پاسخ سرور بیش از حد بزرگ است");
                    result.write(buffer, 0, n);
                }
            }
            return extract(new String(result.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            connection.disconnect();
        }
    }
}
