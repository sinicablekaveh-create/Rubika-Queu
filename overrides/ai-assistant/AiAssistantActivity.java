package ir.sinicable.rubikaqueue;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Opt-in AI drafting: browser handoff or user-owned HTTPS Ollama endpoint. */
public final class AiAssistantActivity extends Activity {
    private EditText draft, server, model, onlineGateway, onlineModel, gatewayToken;
    private Button onlineGenerate;
    private TextView answer;
    private Button generate;
    private String availableSuggestion;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private static final int MAX_RESPONSE_BYTES = 65536;

    private void label(LinearLayout root, String content) {
        TextView v = new TextView(this);
        v.setText(content);
        v.setTextSize(15);
        v.setPadding(8, 10, 8, 10);
        root.addView(v);
    }

    private EditText field(LinearLayout root, String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setTextSize(16);
        root.addView(e, new LinearLayout.LayoutParams(-1, -2));
        return e;
    }

    private void button(LinearLayout root, String text, View.OnClickListener click) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setOnClickListener(click);
        root.addView(b, new LinearLayout.LayoutParams(-1, -2));
        if ("پیشنهاد از مدل متن‌باز (HTTPS)".equals(text)) generate = b;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setPadding(24, 24, 24, 24);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root);
        setContentView(scroll);
        label(root, "دستیار هوش مصنوعی — پیش‌نویس پیام");
        label(root, "فقط متن همین کادر استفاده می‌شود. مخاطبان، حساب روبیکا و تاریخچهٔ پیام‌ها خوانده یا ارسال نمی‌شوند.");
        String original = getIntent().getStringExtra("draft");
        draft = field(root, "متنی که می‌خواهید ویرایش شود", original == null ? "" : original);
        draft.setMinLines(5);
        draft.setGravity(Gravity.TOP);
        label(root, "روش رایگان ۱: استفاده از ChatGPT در مرورگر با سهمیه حساب خودتان. متن پس از تأیید شما به کلیپ‌بورد کپی می‌شود؛ آن را خودتان در ChatGPT جای‌گذاری کنید.");
        button(root, "کپی متن و بازکردن ChatGPT", v -> openChatGpt());
        label(root, "هوش مصنوعی آنلاین در برنامه: سرور واسط HTTPS سازگار با OpenAI را وارد کنید. اشتراک ChatGPT و ابزارهای حساب به برنامه منتقل نمی‌شوند. کلید اصلی ارائه‌دهنده باید فقط روی سرور واسط بماند.");
        onlineGateway = field(root, "نشانی سرور واسط اختصاصی (https://ai.example.com)", "");
        onlineGateway.setTextDirection(View.TEXT_DIRECTION_LTR);
        onlineModel = field(root, "شناسه مدل مجاز روی سرور واسط", "gpt-4.1-mini");
        onlineModel.setTextDirection(View.TEXT_DIRECTION_LTR);
        gatewayToken = field(root, "توکن موقت سرور واسط (نه API Key شرکت ارائه‌دهنده)", "");
        gatewayToken.setTextDirection(View.TEXT_DIRECTION_LTR);
        gatewayToken.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        onlineGenerate = new Button(this);
        onlineGenerate.setAllCaps(false);
        onlineGenerate.setText("ویرایش آنلاین متن با هوش مصنوعی");
        onlineGenerate.setOnClickListener(v -> askOnlineGateway());
        root.addView(onlineGenerate, new LinearLayout.LayoutParams(-1, -2));
        label(root, "روش رایگان ۲: دریافت پاسخ داخل برنامه از مدل متن‌باز روی سرور شخصی خودتان. سرور باید HTTPS و دارای گواهی معتبر باشد؛ هیچ API Key تجاری لازم نیست.");
        server = field(root, "نشانی HTTPS سرور شخصی (مثلاً https://ai.example.com)", "");
        server.setTextDirection(View.TEXT_DIRECTION_LTR);
        model = field(root, "مدل نصب‌شده در Ollama", "qwen2.5:1.5b");
        model.setTextDirection(View.TEXT_DIRECTION_LTR);
        button(root, "پیشنهاد از مدل متن‌باز (HTTPS)", v -> askLocalModel());
        answer = new TextView(this);
        answer.setTextIsSelectable(true);
        answer.setTextSize(16);
        answer.setPadding(10, 18, 10, 18);
        answer.setText("پاسخ در این قسمت نمایش داده می‌شود و به‌طور خودکار ارسال نخواهد شد.");
        root.addView(answer);
        button(root, "کپی پاسخ پیشنهادی", v -> {
            if (availableSuggestion == null) {
                Toast.makeText(this, "ابتدا یک پاسخ معتبر دریافت کنید", Toast.LENGTH_SHORT).show();
                return;
            }
            copy(availableSuggestion);
        });
        button(root, "برگرداندن پیشنهاد به متن پیام", v -> returnToComposer());
    }

    /** Explicitly return a generated draft; never trigger any Rubika send. */
    private void returnToComposer() {
        final String proposal;
        try { proposal = AiPromptPolicy.suggestion(availableSuggestion); }
        catch (IllegalArgumentException ex) {
            Toast.makeText(this, "پاسخ معتبر یا مناسب کادر پیام موجود نیست", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("بازگشت به کادر پیام")
            .setMessage("متن پیشنهادی برای بررسی و جایگزینی به صفحه پیام برگردد؟ این کار هیچ پیامی ارسال نمی‌کند.")
            .setNegativeButton("لغو", null)
            .setPositiveButton("بازگردان", (d, which) -> {
                Intent result = new Intent();
                result.putExtra("ai_proposal", proposal);
                setResult(RESULT_OK, result);
                finish();
            }).show();
    }

    private void copy(String text) {
        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        clipboard.setPrimaryClip(ClipData.newPlainText("Rubika AI draft", text));
        Toast.makeText(this, "متن کپی شد", Toast.LENGTH_SHORT).show();
    }

    private void openChatGpt() {
        final String content;
        try { content = AiPromptPolicy.prompt(draft.getText().toString()); }
        catch (IllegalArgumentException ex) { Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show(); return; }
        new AlertDialog.Builder(this)
            .setTitle("بازکردن ChatGPT")
            .setMessage("فقط متن همین کادر در کلیپ‌بورد قرار می‌گیرد. ارسال آن به ChatGPT به انتخاب خودتان است. کلیپ‌بورد ممکن است توسط برنامه‌های دیگر خوانده شود.")
            .setNegativeButton("لغو", null)
            .setPositiveButton("کپی و بازکردن", (d, which) -> {
                copy("متن زیر را برای پیام‌رسانی حرفه‌ای، خوانا و محترمانه بازنویسی کن؛ چیزی به آن اضافه نکن که در متن نیست:\n\n" + content);
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/"))); }
                catch (Exception e) { Toast.makeText(this, "آدرس chatgpt.com را در مرورگر باز کنید", Toast.LENGTH_LONG).show(); }
            }).show();
    }

    private void askLocalModel() {
        final String content, endpoint, modelName;
        try {
            content = AiPromptPolicy.prompt(draft.getText().toString());
            endpoint = AiPromptPolicy.endpoint(server.getText().toString());
            modelName = AiPromptPolicy.model(model.getText().toString());
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("ارسال فقط متن به سرور انتخابی شما")
            .setMessage("متن نوشته‌شده به " + endpoint + " ارسال می‌شود. داده‌های حساب، شماره‌ها و صف روبیکا ارسال نمی‌شوند. ادامه می‌دهید؟")
            .setNegativeButton("لغو", null)
            .setPositiveButton("ارسال متن", (dialog, which) -> {
                generate.setEnabled(false);
                availableSuggestion = null;
                answer.setText("در حال دریافت پاسخ…");
                io.execute(() -> {
                    String response;
                    boolean success;
                    try {
                        response = callOllama(endpoint, modelName, content);
                        success = true;
                    } catch (Exception e) {
                        response = "پاسخی دریافت نشد. تنظیمات مدل و HTTPS سرور را بررسی کنید.";
                        success = false;
                    }
                    String result = response;
                    boolean accepted = success;
                    runOnUiThread(() -> {
                        if (!isFinishing()) {
                            answer.setText(result);
                            availableSuggestion = accepted ? result : null;
                            generate.setEnabled(true);
                        }
                    });
                });
            }).show();
    }

    /** Only the manually entered draft is sent, after an explicit confirmation. */
    private void askOnlineGateway() {
        final String content, gateway, selectedModel, token;
        try {
            content = AiPromptPolicy.prompt(draft.getText().toString());
            gateway = onlineGateway.getText().toString();
            selectedModel = AiPromptPolicy.model(onlineModel.getText().toString());
            token = AiOnlineClient.gatewayToken(gatewayToken.getText().toString());
            AiOnlineClient.gatewayUrl(gateway);
        } catch (IllegalArgumentException error) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("ارسال متن به هوش مصنوعی آنلاین")
            .setMessage("فقط متن همین کادر به سرور واسط انتخابی شما فرستاده می‌شود. " +
                        "ممکن است سرور واسط آن را به ارائه‌دهنده مدل بفرستد. هیچ پیام، " +
                        "مخاطب، نشست روبیکا یا صف ارسال به‌طور خودکار منتقل نمی‌شود. ادامه می‌دهید؟")
            .setNegativeButton("لغو", null)
            .setPositiveButton("ارسال متن", (d, which) -> {
                onlineGenerate.setEnabled(false);
                availableSuggestion = null;
                answer.setText("در حال ارتباط با هوش مصنوعی آنلاین…");
                io.execute(() -> {
                    String response;
                    boolean success;
                    try {
                        response = AiOnlineClient.complete(gateway, selectedModel, token, content);
                        success = true;
                    } catch (Exception error) {
                        response = "پاسخی دریافت نشد. نشانی HTTPS، توکن و مدل سرور واسط را بررسی کنید.";
                        success = false;
                    }
                    final String displayed = response;
                    final boolean accepted = success;
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            answer.setText(displayed);
                            availableSuggestion = accepted ? displayed : null;
                            onlineGenerate.setEnabled(true);
                        }
                    });
                });
            }).show();
    }

    private static String callOllama(String root, String model, String text) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection)new URL(root + "/api/chat").openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(45000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            JSONObject request = new JSONObject();
            request.put("model", model);
            request.put("stream", false);
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system")
              .put("content", "You are a Persian writing assistant. Improve only user-provided draft text. Do not invent facts or automatically send messages."));
            messages.put(new JSONObject().put("role", "user").put("content", text));
            request.put("messages", messages);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(request.toString().getBytes(StandardCharsets.UTF_8));
            }
            if (connection.getResponseCode() != 200) throw new java.io.IOException("Server returned an unsuccessful response");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream input = connection.getInputStream()) {
                byte[] tmp = new byte[2048];
                int n;
                while ((n = input.read(tmp)) >= 0) {
                    if (buffer.size() + n > MAX_RESPONSE_BYTES) throw new java.io.IOException("Response too large");
                    buffer.write(tmp, 0, n);
                }
            }
            JSONObject response = new JSONObject(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
            JSONObject message = response.optJSONObject("message");
            String content = message == null ? "" : message.optString("content", "").trim();
            if (content.isEmpty()) throw new java.io.IOException("Empty response");
            return content.length() <= 8000 ? content : content.substring(0, 8000);
        } finally {
            connection.disconnect();
        }
    }

    @Override protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
}
