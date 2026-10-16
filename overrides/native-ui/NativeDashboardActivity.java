package ir.sinicable.rubikaqueue;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Native RTL presentation for the existing app. This activity NEVER dispatches messages. */
public final class NativeDashboardActivity extends Activity {
    private static final int INK = Color.rgb(21, 44, 60);
    private static final int MUTED = Color.rgb(101, 120, 135);
    private static final int TEAL = Color.rgb(8, 124, 128);
    private static final int SURFACE = Color.rgb(246, 249, 251);
    private static final int AMBER = Color.rgb(147, 91, 30);
    private static final String PREF = "discovery";
    private static final String AUTO_DISCOVERY = "auto_on_queue_start";
    private static final String[] TABS = {"خانه", "صف پیام", "جست‌وجو", "گزارش‌ها", "تنظیمات"};
    private static final String[] SYMBOLS = {"⌂", "▤", "⌕", "≡", "⚙"};

    private LinearLayout shell;
    private LinearLayout body;
    private int activeTab;
    private String draft = "";
    private String terms = "برق صنعتی، سینی کابل، لاله‌زار";
    private String interval = "60";
    private boolean sampleReviewed = false;
    // Selectors below affect offline preview only, never the actual search engine.
    private boolean demoGroups = true;
    private boolean demoBusinesses = true;
    private EditText draftField;
    private EditText termsField;
    private EditText intervalField;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(247, 250, 251));
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        if (savedInstanceState != null) {
            activeTab = savedInstanceState.getInt("tab", 0);
            draft = savedInstanceState.getString("draft", "");
            terms = savedInstanceState.getString("terms", terms);
            interval = savedInstanceState.getString("interval", interval);
            sampleReviewed = savedInstanceState.getBoolean("reviewed", false);
            demoGroups = savedInstanceState.getBoolean("demo_groups", true);
            demoBusinesses = savedInstanceState.getBoolean("demo_businesses", true);
        }
        showTab(activeTab);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        rememberEdits();
        state.putInt("tab", activeTab);
        state.putString("draft", draft);
        state.putString("terms", terms);
        state.putString("interval", interval);
        state.putBoolean("reviewed", sampleReviewed);
        state.putBoolean("demo_groups", demoGroups);
        state.putBoolean("demo_businesses", demoBusinesses);
        super.onSaveInstanceState(state);
    }

    private int dp(float n) { return Math.round(getResources().getDisplayMetrics().density * n); }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }
    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return l;
    }
    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        t.setPadding(0, dp(3), 0, dp(3));
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private LinearLayout.LayoutParams matchMargins(int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(bottom);
        return p;
    }
    private TextView title(String value) {
        TextView t = text(value, 18, INK, true);
        t.setPadding(0, dp(14), 0, dp(8));
        return t;
    }
    private LinearLayout card() {
        LinearLayout c = column();
        c.setPadding(dp(17), dp(15), dp(17), dp(15));
        c.setBackground(shape(Color.WHITE, 18));
        body.addView(c, matchMargins(12));
        return c;
    }
    private void paragraph(LinearLayout c, String value) {
        TextView t = text(value, 13, MUTED, false);
        t.setLineSpacing(dp(3), 1f);
        c.addView(t, matchMargins(8));
    }
    private void headline(LinearLayout c, String value) {
        c.addView(text(value, 16, INK, true), matchMargins(5));
    }
    private Button action(LinearLayout parent, String label, boolean primary, Runnable click) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(14);
        b.setMinHeight(dp(48));
        b.setTextColor(primary ? Color.WHITE : TEAL);
        b.setBackground(shape(primary ? TEAL : Color.rgb(230, 247, 245), 13));
        b.setOnClickListener(v -> click.run());
        parent.addView(b, matchMargins(6));
        return b;
    }
    private EditText field(LinearLayout c, String hint, String content, boolean multiline) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(content);
        e.setTextSize(14);
        e.setTextColor(INK);
        e.setHintTextColor(MUTED);
        e.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        e.setGravity(Gravity.RIGHT | Gravity.TOP);
        e.setSingleLine(!multiline);
        if (multiline) { e.setMinLines(3); e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE); }
        e.setPadding(dp(12), dp(12), dp(12), dp(12));
        e.setBackground(shape(SURFACE, 12));
        c.addView(e, matchMargins(10));
        return e;
    }
    private CheckBox checkbox(LinearLayout c, String label, boolean checked, java.util.function.Consumer<Boolean> listener) {
        CheckBox cb = new CheckBox(this);
        cb.setText(label);
        cb.setTextSize(14);
        cb.setTextColor(INK);
        cb.setMinHeight(dp(48));
        cb.setChecked(checked);
        cb.setOnCheckedChangeListener((button, enabled) -> listener.accept(enabled));
        c.addView(cb, matchMargins(4));
        return cb;
    }
    private void rememberEdits() {
        if (draftField != null) draft = draftField.getText().toString();
        if (termsField != null) terms = termsField.getText().toString();
        if (intervalField != null) interval = intervalField.getText().toString();
    }
    private boolean autoDiscovery() {
        return getSharedPreferences(PREF, MODE_PRIVATE).getBoolean(AUTO_DISCOVERY, true);
    }
    private void setAutoDiscovery(boolean enabled) {
        getSharedPreferences(PREF, MODE_PRIVATE).edit().putBoolean(AUTO_DISCOVERY, enabled).apply();
    }
    private void open(Class<? extends Activity> activity) {
        startActivity(new Intent(this, activity));
    }
    private void info(String message) {
        new AlertDialog.Builder(this).setTitle("رابط نمایشی").setMessage(message)
            .setPositiveButton("متوجه شدم", null).show();
    }

    private void showTab(int tab) {
        if (tab < 0 || tab >= TABS.length) tab = 0;
        rememberEdits();
        draftField = null; termsField = null; intervalField = null;
        activeTab = tab;
        shell = column();
        shell.setBackgroundColor(SURFACE);
        shell.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        shell.setFitsSystemWindows(true);
        setContentView(shell);

        LinearLayout appBar = row();
        appBar.setBackgroundColor(SURFACE);
        appBar.setPadding(dp(20), dp(11), dp(20), dp(12));
        TextView logo = text("◈", 28, TEAL, true);
        appBar.addView(logo);
        LinearLayout barText = column();
        barText.setPadding(dp(12), 0, 0, 0);
        barText.addView(text("صف پیام روبیکا", 17, INK, true));
        barText.addView(text("RUBIKA-QUEU  •  نسخه آزمایشی رابط", 10, MUTED, false));
        appBar.addView(barText);
        shell.addView(appBar);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        body = column();
        body.setPadding(dp(18), 0, dp(18), dp(24));
        scroll.addView(body);
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        if (tab == 0) home();
        else if (tab == 1) queue();
        else if (tab == 2) discovery();
        else if (tab == 3) reports();
        else settings();

        LinearLayout nav = row();
        nav.setBackgroundColor(Color.WHITE);
        nav.setPadding(dp(3), dp(6), dp(3), dp(6));
        for (int i = 0; i < TABS.length; i++) {
            final int selected = i;
            LinearLayout item = column();
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(2), dp(4), dp(2), dp(4));
            if (i == tab) item.setBackground(shape(Color.rgb(224, 246, 243), 14));
            TextView icon = text(SYMBOLS[i], 22, i == tab ? TEAL : MUTED, true);
            icon.setGravity(Gravity.CENTER);
            TextView txt = text(TABS[i], 10, i == tab ? TEAL : MUTED, i == tab);
            txt.setGravity(Gravity.CENTER);
            item.addView(icon);
            item.addView(txt);
            item.setContentDescription("بخش " + TABS[i]);
            item.setOnClickListener(v -> showTab(selected));
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(64), 1f));
        }
        shell.addView(nav);
    }

    private void home() {
        LinearLayout hero = column();
        hero.setPadding(dp(20), dp(22), dp(20), dp(20));
        GradientDrawable grad = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(10, 110, 120), Color.rgb(10, 158, 151)});
        grad.setCornerRadius(dp(22));
        hero.setBackground(grad);
        hero.addView(text("مرکز کنترل پیام‌ها", 20, Color.WHITE, true));
        hero.addView(text("صف‌ها، جست‌وجوی عمومی و گزارش‌ها در یک نگاه", 13, Color.WHITE, false));
        hero.addView(text("۰ پیام واقعی از این صفحه ارسال می‌شود", 12, Color.WHITE, true));
        body.addView(hero, matchMargins(12));
        body.addView(title("نمای کلی"));
        LinearLayout stats = row();
        String[] labels = {"در انتظار", "تأییدشده", "نامشخص"};
        String[] nums = {"۱۲", "۸", "۱"};
        for (int i = 0; i < labels.length; i++) {
            LinearLayout box = column();
            box.setPadding(dp(10), dp(12), dp(10), dp(12));
            box.setBackground(shape(Color.WHITE, 16));
            box.addView(text(nums[i], 24, i == 2 ? AMBER : TEAL, true));
            box.addView(text(labels[i], 12, MUTED, false));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
            p.setMargins(dp(3), 0, dp(3), 0);
            stats.addView(box, p);
        }
        body.addView(stats, matchMargins(6));
        paragraph(body, "اعداد بالا نمونه نمایشی هستند و از حساب روبیکا خوانده نشده‌اند.");
        LinearLayout queueCard = card();
        headline(queueCard, "صف نمونه ۰۱   ·   متوقف");
        paragraph(queueCard, "درخواست‌های جست‌وجو قبل از ارسال واقعی صف و به‌صورت ترتیبی اجرا می‌شوند.");
        action(queueCard, "باز کردن مدیریت واقعی صف", true, () -> open(MainActivity.class));
        LinearLayout shortcut = card();
        headline(shortcut, "دسترسی سریع");
        action(shortcut, "کشف هوشمند گروه‌های عمومی", false, () -> open(AiDiscoveryActivity.class));
        action(shortcut, "گزارش توسعه برای ChatGPT", false, () -> open(DevelopmentReportActivity.class));
        paragraph(shortcut, "دکمه‌های واقعی تنها صفحه‌های موجود را باز می‌کنند؛ شروع یا ارسال خودکار ندارند.");
    }

    private void queue() {
        body.addView(title("تنظیم صف پیام"));
        paragraph(body, "این صفحه پیش‌نمایش بومی طرح است؛ برای صف و ارسال مجاز از مدیریت اصلی استفاده کنید.");
        LinearLayout form = card();
        headline(form, "آماده‌سازی و بازبینی");
        action(form, "انتخاب فایل Excel / CSV (راهنما)", false,
            () -> info("این پیش‌نمایش به فایل‌ها دسترسی ندارد. برای ورود واقعی فایل، مدیریت اصلی صف را باز کنید."));
        paragraph(form, "متن پیام پیش‌نمایش (فقط روی این صفحه، بدون ارسال)");
        draftField = field(form, "متن پیشنویس آزمایشی", draft, true);
        paragraph(form, "فاصله بین ارسال‌ها، بر حسب ثانیه (۳۰ تا ۳۶۰۰)");
        intervalField = field(form, "۶۰", interval, false);
        intervalField.setInputType(InputType.TYPE_CLASS_NUMBER);
        paragraph(form, "انتخاب دریافت‌کننده و زمان‌بندی در صفحه اصلی انجام می‌شود.");
        action(form, "بررسی پیش‌نمایش صف", true, () -> {
            rememberEdits();
            int seconds;
            try { seconds = Integer.parseInt(interval.trim()); }
            catch (NumberFormatException ex) { seconds = -1; }
            if (draft.trim().isEmpty()) {
                info("برای پیش‌نمایش، متن پیام نباید خالی باشد.");
            } else if (seconds < 30 || seconds > 3600) {
                info("فاصله مجاز ۳۰ تا ۳۶۰۰ ثانیه است.");
            } else {
                info("متن معتبر و فاصله " + seconds + " ثانیه است. هنوز دریافت‌کننده‌ای انتخاب نشده است. هیچ صف یا پیامی ایجاد یا ارسال نشد.");
            }
        });
        LinearLayout statuses = card();
        headline(statuses, "نمونه وضعیت ردیف‌ها");
        paragraph(statuses, "در انتظار  ·  تأیید سرور  ·  نامشخص  ·  ردشده");
        paragraph(statuses, "ارسال نامشخص بعد از قطع برنامه نیازمند بازبینی دستی است؛ نباید بی‌اجازه تکرار شود.");
        action(statuses, "رفتن به مدیریت اصلی صف", false, () -> open(MainActivity.class));
    }

    private void discovery() {
        body.addView(title("کشف هوشمند منابع عمومی"));
        paragraph(body, "جست‌وجوی گروه‌ها و راه‌های تماس عمومی کسب‌وکار؛ بدون استخراج مخاطبان خصوصی.");
        LinearLayout form = card();
        headline(form, "موضوعات جست‌وجو");
        termsField = field(form, "موضوعات مرتبط", terms, true);
        paragraph(form, "موضوعات و نوع منابع فقط برای پیش‌نمایش محلی هستند و در جست‌وجوی واقعی ذخیره نمی‌شوند.");
        checkbox(form, "گروه‌های عمومی (فقط پیش‌نمایش)", demoGroups, enabled -> demoGroups = enabled);
        checkbox(form, "اطلاعات عمومی کسب‌وکارها (فقط پیش‌نمایش)", demoBusinesses, enabled -> demoBusinesses = enabled);
        action(form, "پیش‌نمایش نتیجه جست‌وجو (آفلاین)", true, () -> {
            rememberEdits();
            info(NativeDashboardPreviewPolicy.describe(terms, demoGroups, demoBusinesses));
        });
        paragraph(form, "تنظیم زیر واقعی است و بر اجرای جست‌وجوی پیش از صف در بخش مدیریت اصلی اثر می‌گذارد:");
        checkbox(form, "[تنظیم واقعی] جست‌وجوی خودکار قبل از اجرای صف", autoDiscovery(), this::setAutoDiscovery);
        action(form, "باز کردن جست‌وجوی واقعی برای تنظیم و اجرا", false, () -> open(MainActivity.class));
        action(form, "کشف هوشمند گروه‌ها و تاریخچه", false, () -> open(AiDiscoveryActivity.class));
        LinearLayout results = card();
        headline(results, "نتایج پیشنهادی — نمونه نمایشی");
        paragraph(results, "نتیجه بر اساس نوع منبع و عبارت واردشده فقط پس از لمس «پیش‌نمایش» ساخته می‌شود.");
        paragraph(results, "هیچ نتیجه واقعی یا امتیاز ارتباط از سرویس روبیکا دریافت نشده است.");
        paragraph(results, "در برنامه اصلی، ابتدا جست‌وجوی فقط‌خواندنی تکمیل می‌شود، سپس صف ادامه می‌یابد.");
    }

    private void reports() {
        body.addView(title("گزارش و بازبینی"));
        LinearLayout timeline = card();
        headline(timeline, "روند عملیات — نمونه نمایشی");
        paragraph(timeline, "۰۹:۴۱  ·  آماده‌سازی صف آزمایشی");
        paragraph(timeline, "۰۹:۴۲  ·  جست‌وجوی منابع عمومی");
        paragraph(timeline, "۰۹:۴۳  ·  نتیجه نامشخص؛ بازبینی لازم");
        LinearLayout review = card();
        headline(review, "بازبینی وضعیت نامشخص");
        paragraph(review, sampleReviewed ? "نمونه بررسی شد (فقط در حافظه این صفحه)" : "این نمونه نیاز به بازبینی دارد؛ پیام واقعی تغییر نمی‌کند.");
        action(review, "علامت‌گذاری نمونه به‌عنوان بررسی‌شده", false, () -> {
            sampleReviewed = true;
            showTab(3);
        });
        LinearLayout export = card();
        headline(export, "گزارش توسعه برای ChatGPT");
        paragraph(export, "گزارش واقعی فقط با تأیید شما و بدون مخاطبان، پیام‌ها، نشست یا توکن آماده می‌شود.");
        action(export, "باز کردن صفحه گزارش اصلی", true, () -> open(DevelopmentReportActivity.class));
    }

    private void settings() {
        body.addView(title("تنظیمات و حریم خصوصی"));
        LinearLayout preferences = card();
        headline(preferences, "گزینه‌های صف و کشف");
        checkbox(preferences, "[تنظیم واقعی] جست‌وجوی خودکار با شروع صف", autoDiscovery(), this::setAutoDiscovery);
        paragraph(preferences, "این گزینه به همان تنظیم واقعی جست‌وجوی پیش از صف متصل است.");
        LinearLayout about = card();
        headline(about, "نسخه و شناسه");
        paragraph(about, "شناسه: ir.sinicable.rubikaqueue");
        paragraph(about, "پیشنهاد نسخه UI: 1.3.3 / versionCode 17");
        paragraph(about, "Android SDK هدف: 34   ·   سازگار با RTL");
        LinearLayout privacy = card();
        headline(privacy, "محدودیت انتشار و اتصال");
        paragraph(privacy, "به‌روزرسانی بدون پاک شدن داده‌ها نیازمند شناسه بسته یکسان، افزایش versionCode و گواهی امضای یکسان است. تست نصب روی دستگاه هنوز لازم است.");
        paragraph(privacy, "API رسمی بات مستقل از صف حساب شخصی است و فقط با تأیید صریح کاربر باید استفاده شود.");
        action(privacy, "مدیریت اصلی و اتصال‌های مستند", false, () -> open(MainActivity.class));
    }
}