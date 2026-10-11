package ir.sinicable.rubikaqueue;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/** Optional official public website links and documented Rubika Bot API tests. */
public final class OfficialBotActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private EditText tokenField, chatField, textField;
    private TextView status;
    private volatile boolean busy;

    /** Builds the official-link and bot-test screen, excluding the token field from saved view state. */
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);
        setContentView(scroll);

        title(root, "روبیکا: درگاه رسمی وب و بات", 22);
        title(root, "پیوندهای وب رسمی", 17);
        label(root, "سایت و نسخه وب در مرورگر خودتان باز می‌شوند. ورود در مرورگر، حساب را به صف ارسال برنامه متصل نمی‌کند.");
        button(root, "صفحه اصلی روبیکا", () -> open("https://rubika.ir/home"));
        button(root, "پیام‌رسان وب روبیکا", () -> open("https://web.rubika.ir/"));
        button(root, "مستندات API بات", () -> open("https://rubika.ir/botapi"));

        title(root, "آزمایش API رسمی بات", 17);
        label(root, "بات خودتان را از BotFather بسازید. این API برای بات است و جایگزین ورود به حساب شخصی نیست.");
        tokenField = field(root,"توکن بات", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tokenField.setSingleLine(true);
        tokenField.setSaveEnabled(false);
        tokenField.setTextDirection(View.TEXT_DIRECTION_LTR);
        chatField = field(root,"شناسه chat_id مجاز",InputType.TYPE_CLASS_TEXT);
        chatField.setSingleLine(true);
        chatField.setTextDirection(View.TEXT_DIRECTION_LTR);
        textField = field(root,"متن یک پیام آزمایشی",InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        textField.setMinLines(2);
        textField.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4200)});
        button(root, "بررسی بات (getMe)", () -> {
            final String token=tokenField.getText().toString().trim();
            execute(() -> {
                JSONObject response = new OfficialBotApi(token).getMe();
                JSONObject data = response.optJSONObject("data");
                JSONObject bot = data == null ? null : data.optJSONObject("bot");
                if(bot == null) return "بات پاسخ داد؛ هویت را در روبیکا بررسی کنید";
                return "بات پاسخ داد: " + bot.optString("username", bot.optString("first_name","نامشخص"));
            });
        });
        button(root, "ارسال یک پیام آزمایشی", this::confirmSend);
        status = new TextView(this);
        status.setText("آماده؛ هیچ درخواستی ارسال نشده است");
        status.setTextSize(15);
        status.setPadding(0,dp(18),0,dp(18));
        root.addView(status);
        label(root, "توکن روی دیسک ذخیره نمی‌شود. پاسخ‌های خطا نباید شامل توکن باشند. ارسال به بات فقط پس از تأیید شما انجام می‌شود و به صف حساب شخصی متصل نیست.");
    }

    private interface Operation { /** Performs a bot operation and returns the message to display, or throws on failure. */ String run() throws Exception; }
    /** Runs one bot operation asynchronously and displays its result or a generic error without exception details. */
    private void execute(Operation operation) {
        if (busy) { Toast.makeText(this,"عملیات قبلی هنوز تمام نشده",Toast.LENGTH_SHORT).show();return; }
        busy=true;
        status.setText("در حال ارتباط با API رسمی…");
        io.execute(() -> {
            String result;
            try { result=operation.run(); }
            catch (Exception e) { result = e instanceof IllegalArgumentException ? "توکن، شناسه چت و متن را بررسی کنید" : "ارتباط با بات کامل نشد؛ اینترنت و دسترسی بات را بررسی کنید. اگر ارسال کرده‌اید، قبل از تلاش مجدد چت را بررسی کنید."; }
            final String reply=result;
            runOnUiThread(() -> { busy=false; if(!isFinishing()&&!isDestroyed())status.setText(reply); });
        });
    }

    /** Confirms a single bot test message and applies the persisted 30-second local cooldown before sending. */
    private void confirmSend() {
        String token=tokenField.getText().toString().trim();
        String chat=chatField.getText().toString().trim();
        String text=textField.getText().toString();
        if(token.isEmpty()||chat.isEmpty()||text.trim().isEmpty()) {
            Toast.makeText(this,"توکن، شناسه چت و متن را وارد کنید",Toast.LENGTH_LONG).show();return;
        }
        new AlertDialog.Builder(this)
            .setTitle("تأیید ارسال با بات")
            .setMessage("یک پیام به چت " + chat + " ارسال شود؟\n\n" + text)
            .setNegativeButton("انصراف", null)
            .setPositiveButton("ارسال", (dialog,which) -> execute(() -> {
                long now=System.currentTimeMillis();
                long last=getPreferences(0).getLong("bot_test_last",0);
                if (now-last < 30000) return "برای آزمایش بعدی حداقل ۳۰ ثانیه صبر کنید";
                if (!getPreferences(0).edit().putLong("bot_test_last",now).commit())throw new IOException("ثبت زمان آزمایش ناموفق بود");
                JSONObject response = new OfficialBotApi(token).sendMessage(chat,text);
                String id=OfficialBotApi.messageId(response);
                return id.isEmpty()? "پاسخ دریافت شد؛ ارسال را در چت بررسی کنید" : "شناسه پیام بات: " + id;
            })).show();
    }

    /** Opens a fixed official HTTPS link in the browser and reports when no browser is available. */
    private void open(String url) {
        // Intents target fixed HTTPS links; the user signs in within their own browser.
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "مرورگر در دسترس نیست؛ یک مرورگر نصب یا فعال کنید", Toast.LENGTH_LONG).show();
        }
    }

    /** Converts density-independent dimensions to screen pixels. */
    private int dp(int size) { return (int)(size*getResources().getDisplayMetrics().density); }
    /** Adds a right-aligned heading with the requested text size. */
    private void title(LinearLayout root,String text,int size){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setGravity(Gravity.RIGHT);t.setPadding(0,dp(12),0,dp(12));root.addView(t);}
    /** Adds a descriptive text label to the supplied layout. */
    private void label(LinearLayout root,String text){TextView t=new TextView(this);t.setText(text);t.setTextSize(14);t.setPadding(0,dp(8),0,dp(8));root.addView(t);}
    /** Adds and returns an editable field with the requested hint and input type. */
    private EditText field(LinearLayout root,String hint,int type){EditText t=new EditText(this);t.setHint(hint);t.setInputType(type);root.addView(t,new LinearLayout.LayoutParams(-1,-2));return t;}
    /** Adds a button that runs the supplied job when clicked. */
    private void button(LinearLayout root,String caption,Runnable job){Button t=new Button(this);t.setAllCaps(false);t.setText(caption);t.setOnClickListener(view -> job.run());root.addView(t,new LinearLayout.LayoutParams(-1,-2));}
    /** Interrupts background work and clears the token field during activity destruction. */
    @Override protected void onDestroy() { io.shutdownNow(); if (tokenField!=null)tokenField.setText(""); super.onDestroy(); }
}
