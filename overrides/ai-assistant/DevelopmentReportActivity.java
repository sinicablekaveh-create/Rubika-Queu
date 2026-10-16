package ir.sinicable.rubikaqueue;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Owner-initiated, review-before-share development report. No background network traffic. */
public final class DevelopmentReportActivity extends Activity {
    private EditText request;
    private EditText report;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root);
        setContentView(scroll);

        label(root, "ارسال گزارش توسعه به ChatGPT", 21);
        label(root, "فقط مشخصات عمومی برنامه و توضیحی که خودتان وارد می‌کنید در گزارش می‌آیند. "
                + "مخاطبان، پیام‌ها، کلیدها و اطلاعات حساب روبیکا خوانده نمی‌شوند. "
                + "متن نهایی را پیش از کپی یا اشتراک بررسی کنید.", 14);
        request = new EditText(this);
        request.setHint("چه قابلیتی اضافه یا چه مشکلی اصلاح شود؟");
        request.setMinLines(3);
        request.setMaxLines(10);
        root.addView(request, new LinearLayout.LayoutParams(-1, -2));

        button(root, "ساخت گزارش توسعه (بدون ارسال)", v -> createReport());
        report = new EditText(this);
        report.setHint("پیش‌نمایش قابل‌ویرایش گزارش برای ChatGPT");
        report.setMinLines(8);
        report.setTextIsSelectable(true);
        root.addView(report, new LinearLayout.LayoutParams(-1, -2));

        button(root, "کپی گزارش", v -> {
            String text = preview();
            if (text != null) copy(text);
        });
        button(root, "اشتراک گزارش با ChatGPT یا برنامه دلخواه", v -> {
            String text = preview();
            if (text == null) return;
            confirmExternalShare(text, false);
        });
        button(root, "کپی گزارش و بازکردن سایت ChatGPT", v -> {
            String text = preview();
            if (text == null) return;
            confirmExternalShare(text, true);
        });
        label(root, "دکمه اشتراک فقط صفحه انتخاب برنامه را باز می‌کند؛ ارسال نهایی نیازمند اقدام شماست. "
                + "هیچ تغییری در صف پیام، ارسال روبیکا یا فایل‌های گوشی انجام نمی‌شود.", 13);
    }

    private void label(LinearLayout root, String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setPadding(8, 12, 8, 12);
        root.addView(view);
    }

    private void button(LinearLayout root, String title, View.OnClickListener action) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(title);
        button.setOnClickListener(action);
        root.addView(button, new LinearLayout.LayoutParams(-1, -2));
    }

    private void createReport() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            long code = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
            String value = DevelopmentReportBuilder.format(getPackageName(), info.versionName,
                    code, Build.VERSION.SDK_INT, request.getText().toString());
            report.setText(value);
        } catch (Exception error) {
            Toast.makeText(this, "توضیح معتبر وارد کنید؛ گزارش ساخته نشد", Toast.LENGTH_LONG).show();
        }
    }

    private String preview() {
        String text = report.getText().toString();
        if (text.trim().isEmpty() || text.length() > 6500) {
            Toast.makeText(this, "ابتدا گزارش معتبر بسازید و بازبینی کنید", Toast.LENGTH_LONG).show();
            return null;
        }
        return text;
    }

    private void copy(String value) {
        ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (manager == null) {
            Toast.makeText(this, "کلیپ‌بورد در دسترس نیست", Toast.LENGTH_SHORT).show();
            return;
        }
        manager.setPrimaryClip(ClipData.newPlainText("Rubika-Queu development report", value));
        Toast.makeText(this, "گزارش کپی شد", Toast.LENGTH_SHORT).show();
    }

    private void confirmExternalShare(String value, boolean openBrowser) {
        new AlertDialog.Builder(this)
                .setTitle("تأیید انتقال اطلاعات")
                .setMessage("فقط متن نمایش‌داده‌شده در پیش‌نمایش منتقل یا کپی می‌شود. "
                        + "اطلاعاتی که خودتان تایپ کرده‌اید نیز در آن وجود دارد. "
                        + "هیچ دسترسی خودکاری به حساب ChatGPT یا گوشی ایجاد نمی‌شود. ادامه می‌دهید؟")
                .setNegativeButton("لغو", null)
                .setPositiveButton("تأیید", (dialog, which) -> {
                    if (openBrowser) {
                        copy(value);
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/")));
                        } catch (RuntimeException error) {
                            Toast.makeText(this, "سایت chatgpt.com را دستی باز کنید", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        try {
                            Intent send = new Intent(Intent.ACTION_SEND);
                            send.setType("text/plain");
                            send.putExtra(Intent.EXTRA_TEXT, value);
                            startActivity(Intent.createChooser(send, "برنامه دریافت‌کننده گزارش را انتخاب کنید"));
                        } catch (RuntimeException error) {
                            Toast.makeText(this, "اشتراک ممکن نیست؛ گزارش را کپی کنید", Toast.LENGTH_LONG).show();
                        }
                    }
                }).show();
    }
}
