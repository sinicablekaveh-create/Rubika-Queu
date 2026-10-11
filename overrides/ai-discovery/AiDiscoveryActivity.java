package ir.sinicable.rubikaqueue;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Optional discovery UI, isolated from queue creation and authentication. */
public final class AiDiscoveryActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private volatile boolean cancelled;
    private EditText query, endpoint, model;
    private TextView results;
    private Button search;
    private Button stop;
    private volatile Thread activeThread;
    private LinearLayout root;
    private void label(String text) { TextView v = new TextView(this); v.setText(text); root.addView(v); }
    private EditText field(String hint) { EditText v = new EditText(this); v.setHint(hint); root.addView(v); return v; }
    private Button button(String text, Runnable action) { Button v = new Button(this); v.setText(text); root.addView(v); v.setOnClickListener(w -> action.run()); return v; }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(24,24,24,24);
        ScrollView scroll = new ScrollView(this); scroll.addView(root); setContentView(scroll);
        label("کشف هوشمند گروه‌ها");
        label("عبارت جست‌وجو با تأیید شما به سرور HTTPS فرستاده می‌شود. AI فقط عبارت‌های مرتبط پیشنهاد می‌کند؛ گروه‌ها از موتور جست‌وجوی موجود دریافت می‌شوند. هیچ پیام یا درخواست عضویتی ارسال نمی‌شود.");
        query = field("موضوع گروه؛ ۲ تا ۳۰۰ نویسه");
        endpoint = field("سرور HTTPS سازگار با Ollama (اختیاری)");
        model = field("مدل"); model.setText("qwen2.5:1.5b");
        search = button("جست‌وجو و رتبه‌بندی", this::confirmSearch);
        stop = button("توقف", this::cancelActiveTask); stop.setEnabled(false);
        button("نتایج محلی / آفلاین", () -> local(false));
        button("تاریخچه جست‌وجو", () -> local(true));
        button("پاک کردن داده‌های این حساب", () -> new AlertDialog.Builder(this).setMessage("نتایج محلی و تاریخچه این حساب پاک شوند؟").setNegativeButton("لغو",null).setPositiveButton("پاک کن",(d,w) -> submit(() -> {
            RubikaClient client = account(); try (AiDiscoveryStore store = new AiDiscoveryStore(this)) { store.clear(client.guid); } return "داده‌های محلی پاک شدند";
        })).show());
        results = new TextView(this); results.setTextIsSelectable(true); root.addView(results);
    }
    private RubikaClient account() throws Exception {
        RubikaClient c = Vault.load(this); if (c == null) throw new java.io.IOException("ابتدا وارد حساب شوید"); return c;
    }
    private void confirmSearch() {
        final String q, server, selectedModel;
        try { q = DiscoveryRanking.query(query.getText().toString()); server = endpoint.getText().toString().trim(); selectedModel = model.getText().toString();
            if (!server.isEmpty()) { AiPromptPolicy.endpoint(server); AiPromptPolicy.model(selectedModel); }
        } catch (Exception e) { results.setText(e.getMessage()); return; }
        Runnable run = () -> discover(q, server, selectedModel);
        if (server.isEmpty()) run.run();
        else new AlertDialog.Builder(this).setTitle("ارسال عبارت به سرور AI").setMessage(q + "\n" + server).setNegativeButton("لغو",null).setPositiveButton("جست‌وجو",(d,w) -> run.run()).show();
    }
    private void discover(String q, String server, String selectedModel) {
        submit(() -> {
            if (!OperationGate.acquire("ai-discovery")) throw new java.io.IOException("ابتدا عملیات فعلی را متوقف کنید");
            try {
                RubikaClient c = account();
                String[] terms = {q}; String note = "";
                if (!server.isEmpty()) {
                    try { terms = AiDiscoveryClient.expand(server, selectedModel, q); }
                    catch (Exception e) { note = "AI در دسترس نبود؛ جست‌وجوی اصلی انجام شد.\n"; }
                }
                if (cancelled) throw new InterruptedException("جست‌وجو متوقف شد");
                JSONObject data = DiscoveryData.empty();
                new DiscoveryEngine((method,input) -> c.call(method,input,false), new DiscoveryEngine.PublicPages(),1800)
                    .run(data,terms,true,false,new DiscoveryEngine.Progress() {
                        public boolean stopped() { return cancelled || Thread.currentThread().isInterrupted(); }
                        public void update(JSONObject s,String text) { }
                    });
                if (cancelled) throw new InterruptedException("جست‌وجو متوقف شد");
                JSONArray groups = data.getJSONArray("groups");
                try (AiDiscoveryStore store = new AiDiscoveryStore(this)) { store.save(c.guid,q,groups); }
                return note + ranked(q,groups);
            } finally { OperationGate.release("ai-discovery"); }
        });
    }
    private String ranked(String q, JSONArray groups) throws Exception {
        List<JSONObject> rows = new ArrayList<>();
        for (int i=0;i<groups.length();i++) rows.add(groups.getJSONObject(i));
        rows.sort((a,b) -> {
            int byScore = Integer.compare(score(q,b),score(q,a));
            return byScore != 0 ? byScore : a.optString("name").compareToIgnoreCase(b.optString("name"));
        });
        StringBuilder out = new StringBuilder("گروه‌ها: " + rows.size() + "\nرتبه بر اساس تطبیق موضوع است؛ مجوز ارسال یا اعتبار گروه را تضمین نمی‌کند.\n");
        for (JSONObject g : rows) {
            String name = g.optString("name").trim();
            String identity = g.optString("link").trim();
            if (identity.isEmpty()) identity = g.optString("guid").trim();
            out.append("\n").append(name.isEmpty() ? "گروه بدون عنوان" : name)
                .append(" • امتیاز ").append(score(q,g)).append("\n");
            if (!identity.isEmpty()) out.append(identity).append("\n");
            String availability = g.optString("availability").trim();
            if (!availability.isEmpty()) out.append(availability).append("\n");
        }
        return out.toString();
    }
    private int score(String q, JSONObject g) {
        return DiscoveryRanking.score(q,g.optString("name"),g.optString("keyword"),!g.optString("guid").isEmpty());
    }
    private void local(boolean history) {
        String q = query.getText().toString();
        submit(() -> { RubikaClient c = account(); try (AiDiscoveryStore store = new AiDiscoveryStore(this)) { return history ? store.history(c.guid) : ranked(q,store.groups(c.guid)); } });
    }
    private void submit(Callable<String> task) {
        if (!search.isEnabled()) return;
        cancelled = false; search.setEnabled(false); stop.setEnabled(true); results.setText("در حال پردازش…");
        io.execute(() -> {
            activeThread = Thread.currentThread();
            String text;
            try { text = task.call(); }
            catch (Exception e) {
                text = cancelled || e instanceof InterruptedException
                    ? "عملیات متوقف شد؛ نتایج محلی محفوظ‌اند."
                    : "عملیات کامل نشد؛ نتایج محلی محفوظ‌اند. " + safeMessage(e);
            }
            final String output = text;
            activeThread = null;
            runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) { results.setText(output); search.setEnabled(true); stop.setEnabled(false); } });
        });
    }
    private void cancelActiveTask() {
        cancelled = true;
        Thread thread = activeThread;
        if (thread != null) thread.interrupt();
        results.setText("در حال توقف…");
        stop.setEnabled(false);
    }
    private String safeMessage(Exception error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) return "اتصال و تنظیمات را بررسی کنید.";
        return message.length() > 180 ? message.substring(0,180) : message;
    }
    @Override public void onDestroy() { cancelActiveTask(); io.shutdownNow(); super.onDestroy(); }
}
