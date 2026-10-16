package ir.sinicable.rubikaqueue;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Explicit, single-destination approval/revocation; never authorizes discovery results. */
public final class AllowedDestinationsActivity extends Activity {
    private String owner;
    private LinearLayout entries;
    private EditText input;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            RubikaClient account = Vault.load(this);
            if (account == null || account.guid == null || account.guid.isEmpty()) {
                Toast.makeText(this, "ابتدا وارد حساب روبیکا شوید", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            owner = account.guid;
            AllowedDestinationStore.read(this, owner);
        } catch (Exception error) {
            Toast.makeText(this, "حساب قابل تأیید نیست", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (getResources().getDisplayMetrics().density * 18);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);
        setContentView(scroll);

        text(root, "مقصدهای مجاز", 22);
        text(root, "فقط شناسه گروه یا شماره‌ای را اضافه کنید که مجوز ارسال به آن را دارید. نتیجه جست‌وجو و فایل اکسل هیچ مقصدی را خودکار تأیید نمی‌کنند. فهرست در این دستگاه و فقط برای همین حساب ذخیره می‌شود.", 15);
        input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("شناسه گروه g0... یا شماره تلفن");
        root.addView(input);
        button(root, "بررسی و تأیید شناسه گروه", () -> confirm("group"));
        button(root, "بررسی و تأیید شماره", () -> confirm("phone"));
        text(root, "مقصدهای تأییدشده (با حذف هر مورد، ادامه ارسال به آن مقصد مسدود می‌شود):", 16);
        entries = new LinearLayout(this);
        entries.setOrientation(LinearLayout.VERTICAL);
        root.addView(entries);
        renderEntries();
    }

    private void confirm(String kind) {
        try {
            String raw = input.getText().toString().trim();
            String id = "phone".equals(kind) ? Phones.normalize(raw) : raw;
            String key = AllowedDestinationPolicy.key(kind, id);
            new AlertDialog.Builder(this)
                .setTitle("تأیید مقصد")
                .setMessage("آیا مجوز ارسال پیام به این مقصد را دارید؟\n" + key +
                    "\nفقط پس از تأیید دستی شما به فهرست مجاز افزوده می‌شود.")
                .setNegativeButton("انصراف", null)
                .setPositiveButton("تأیید و افزودن", (dialog, which) -> {
                    try {
                        AllowedDestinationStore.approve(this, owner, key);
                        input.setText("");
                        renderEntries();
                    } catch (Exception error) {
                        Toast.makeText(this, "تأیید ذخیره نشد", Toast.LENGTH_LONG).show();
                    }
                }).show();
        } catch (Exception invalid) {
            Toast.makeText(this, "شناسه یا شماره واردشده معتبر نیست", Toast.LENGTH_LONG).show();
        }
    }

    private void renderEntries() {
        entries.removeAllViews();
        List<String> keys = new ArrayList<>(AllowedDestinationStore.read(this, owner));
        Collections.sort(keys);
        if (keys.isEmpty()) text(entries, "هیچ مقصدی مجاز نیست؛ ارسال صف مسدود است.", 14);
        for (String key : keys) {
            text(entries, key, 14);
            button(entries, "لغو مجوز این مقصد", () -> new AlertDialog.Builder(this)
                .setTitle("حذف مقصد مجاز")
                .setMessage(key)
                .setNegativeButton("انصراف", null)
                .setPositiveButton("حذف", (dialog, which) -> {
                    try {
                        AllowedDestinationStore.revoke(this, owner, key);
                        renderEntries();
                    } catch (Exception error) {
                        Toast.makeText(this, "حذف ذخیره نشد", Toast.LENGTH_LONG).show();
                    }
                }).show());
        }
    }

    private void text(LinearLayout parent, String value, int sp) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setPadding(0, 12, 0, 12);
        parent.addView(view);
    }

    private void button(LinearLayout parent, String title, Runnable action) {
        Button view = new Button(this);
        view.setText(title);
        view.setOnClickListener(v -> action.run());
        parent.addView(view, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
}
