package ir.sinicable.rubikaqueue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/**
 * Separate, documented Rubika Bot API transport (not the user-account protocol).
 * Docs: https://rubika.ir/botapi and https://rubika.ir/botapi/methods
 * This class does not store tokens or copy the Rubika website's private scripts.
 */
public final class OfficialBotApi {
    private static final String BASE = "https://botapi.rubika.ir/v3/";
    private static final int MAX_REPLY_BYTES = 64 * 1024;
    private final String token;

    public OfficialBotApi(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_:.\\-]{8,256}")) {
            throw new IllegalArgumentException("توکن بات معتبر نیست");
        }
        this.token = token;
    }

    /** POST /getMe - identifies the bot associated with the supplied token. */
    public JSONObject getMe() throws Exception {
        return call("getMe", null);
    }

    /** POST /sendMessage - send a single, explicitly confirmed test message. */
    public JSONObject sendMessage(String chatId, String text) throws Exception {
        if (chatId == null || chatId.trim().isEmpty() || chatId.length() > 180) {
            throw new IllegalArgumentException("شناسه چت معتبر نیست");
        }
        if (text == null || text.trim().isEmpty() || text.length() > 4200) {
            throw new IllegalArgumentException("متن پیام معتبر نیست");
        }
        JSONObject payload = new JSONObject();
        payload.put("chat_id", chatId.trim());
        payload.put("text", text);
        return call("sendMessage", payload);
    }

    private JSONObject call(String method, JSONObject payload) throws Exception {
        // Methods are fixed constants; no untrusted URL or host can be supplied.
        if (!"getMe".equals(method) && !"sendMessage".equals(method)) {
            throw new IllegalArgumentException("متد پشتیبانی نمی‌شود");
        }
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(BASE + token + "/" + method).openConnection();
            con.setRequestMethod("POST");
            con.setConnectTimeout(12000);
            con.setReadTimeout(20000);
            con.setInstanceFollowRedirects(false);
            con.setUseCaches(false);
            con.setRequestProperty("Accept", "application/json");
            if (payload != null) {
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                con.setDoOutput(true);
                byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = con.getOutputStream()) {
                    out.write(body);
                }
            }
            int status = con.getResponseCode();
            if (status < 200 || status >= 300) {
                // Don't expose token-bearing URLs, response bodies, or secrets.
                throw new IOException("پاسخ HTTP بات: " + status);
            }
            JSONObject result;
            try (InputStream in = con.getInputStream()) {
                result = new JSONObject(readBounded(in));
            }
            if (result.has("status") && !result.optString("status").equalsIgnoreCase("OK")) {
                throw new IOException("متد بات موفق نبود؛ وضعیت پاسخ را در روبیکا بررسی کنید");
            }
            if (result.has("ok") && !result.optBoolean("ok")) {
                throw new IOException("بات درخواست را نپذیرفت؛ دسترسی و شناسه چت را بررسی کنید");
            }
            return result;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static String readBounded(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = in.read(buffer)) != -1) {
            if (out.size() + count > MAX_REPLY_BYTES) throw new IOException("پاسخ بات بیش از حد بزرگ است");
            out.write(buffer, 0, count);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    public static String messageId(JSONObject response) {
        JSONObject data = response.optJSONObject("data");
        String id = data == null ? "" : data.optString("message_id", "");
        return id.isEmpty() ? response.optString("message_id", "") : id;
    }
}
