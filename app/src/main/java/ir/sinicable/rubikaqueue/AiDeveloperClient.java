package ir.sinicable.rubikaqueue;

import org.json.JSONObject;
import org.json.JSONArray;
import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Manually authorized online coding chat and GitHub development proposals. */
public final class AiDeveloperClient {
    private AiDeveloperClient() {}
    private static final int MAX_RESPONSE_BYTES = 65536;

    static String path(String root, String route) {
        // Shares all HTTPS and first-party site safeguards with the drafting client.
        AiOnlineClient.gatewayUrl(root);
        return AiPromptPolicy.endpoint(root) + route;
    }
    static String prompt(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() < 2 || text.length() > 3500)
            throw new IllegalArgumentException("متن تحقیق باید ۲ تا ۳۵۰۰ نویسه باشد");
        return text;
    }
    static String task(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() < 10 || text.length() > 1200)
            throw new IllegalArgumentException("دستور توسعه باید بین ۱۰ تا ۱۲۰۰ نویسه باشد");
        return text;
    }
    static JSONObject chatRequest(String question, boolean web) throws Exception {
        return new JSONObject().put("prompt", prompt(question)).put("use_web", web);
    }
    static JSONObject proposalRequest(String developmentTask) throws Exception {
        return new JSONObject().put("task", task(developmentTask));
    }
    static String chatAnswer(String json) throws Exception {
        JSONObject result = new JSONObject(json);
        if (!(result.opt("answer") instanceof String))
            throw new IOException("پاسخ گفتگو معتبر نیست");
        String answer = result.getString("answer").trim();
        if (answer.isEmpty() || answer.length() > 12000)
            throw new IOException("پاسخ گفتگو بیش از حد بزرگ یا خالی است");
        return answer;
    }
    static String proposalAnswer(String json) throws Exception {
        JSONObject result = new JSONObject(json);
        if (!"accepted".equals(result.optString("status")))
            throw new IOException("درخواست توسعه تأیید نشده است");
        return "درخواست در GitHub ثبت شد. اجرای Workflow و نتیجهٔ تست را بررسی کنید:\n"
                + result.getString("workflow_url");
    }
    static String statusAnswer(String json) throws Exception {
        JSONObject result = new JSONObject(json);
        String status = result.optString("status");
        if ("not_started".equals(status)) return "هنوز اجرای توسعه‌ای ثبت نشده است.";
        if (!("queued".equals(status) || "in_progress".equals(status) || "completed".equals(status)))
            throw new IOException("وضعیت Workflow معتبر نیست");
        StringBuilder builder = new StringBuilder("وضعیت آخرین Workflow: ").append(status);
        String conclusion = result.optString("conclusion");
        if (!conclusion.isEmpty() && !"null".equals(conclusion))
            builder.append("\nنتیجه: ").append(conclusion);
        String run = result.optString("run_url");
        if (run.startsWith("https://github.com/sinicablekaveh-create/Rubika-Queu/actions/runs/"))
            builder.append("\n").append(run);
        return builder.toString();
    }
    public static String chat(String server, String token, String question, boolean web)
            throws Exception {
        return chatAnswer(exchange(server, token, "/v1/dev/chat", chatRequest(question, web), 200));
    }
    public static String propose(String server, String token, String developmentTask)
            throws Exception {
        return proposalAnswer(exchange(server, token, "/v1/dev/propose",
                proposalRequest(developmentTask), 202));
    }
    public static String status(String server, String token) throws Exception {
        return statusAnswer(exchange(server, token, "/v1/dev/status", null, 200));
    }
    static String desktopAnswer(String json) throws Exception {
        JSONObject result = new JSONObject(json).getJSONObject("result");
        JSONArray content = result.optJSONArray("content");
        if (content == null) throw new IOException("پاسخ دستگاه معتبر نیست");
        StringBuilder answer = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            JSONObject block = content.optJSONObject(i);
            if (block != null && "text".equals(block.optString("type"))) {
                if (answer.length() > 0) answer.append('\n');
                answer.append(block.optString("text"));
            }
        }
        if (result.optBoolean("isError") || answer.length() == 0 || answer.length() > 60000)
            throw new IOException("عملیات دستگاه ناموفق یا پاسخ نامعتبر است");
        return answer.toString();
    }
    public static String desktop(String server, String token, String tool, JSONObject arguments)
            throws Exception {
        if (!("list_devices".equals(tool) || "list_directory".equals(tool) ||
                "read_file".equals(tool) || "write_file".equals(tool) ||
                "start_process".equals(tool)))
            throw new IllegalArgumentException("عملیات دستگاه مجاز نیست");
        return desktopAnswer(exchange(server, token, "/v1/desktop/call",
                new JSONObject().put("tool", tool).put("arguments", arguments), 200));
    }
    private static String exchange(String root, String token, String route, JSONObject payload,
            int expectedStatus) throws Exception {
        String uri = path(root, route);
        String bearer = AiOnlineClient.gatewayToken(token);
        HttpsURLConnection connection = (HttpsURLConnection) new URL(uri).openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(60000);
            connection.setRequestMethod(payload == null ? "GET" : "POST");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + bearer);
            if (payload != null) {
                byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream out = connection.getOutputStream()) { out.write(bytes); }
            }
            if (connection.getResponseCode() != expectedStatus)
                throw new IOException("سرور توسعه پاسخ موفق نداد. وضعیت HTTP: "
                        + connection.getResponseCode());
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            try (InputStream in = connection.getInputStream()) {
                byte[] buffer = new byte[2048];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted())
                        throw new InterruptedException("درخواست متوقف شد");
                    if (data.size() + n > MAX_RESPONSE_BYTES)
                        throw new IOException("پاسخ سرور بیش از حد بزرگ است");
                    data.write(buffer, 0, n);
                }
            }
            return new String(data.toByteArray(), StandardCharsets.UTF_8);
        } finally { connection.disconnect(); }
    }
}
