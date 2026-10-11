package ir.sinicable.rubikaqueue;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Optional discovery UI, isolated from queue creation and authentication. */
public final class AiDiscoveryActivity extends Activity {
    private EditText query, endpoint, model;
    private TextView results;
    private Button search, stop;
    private LinearLayout root;
    private TaskState task;

    /**
     * Retained across configuration changes. It holds only the application context,
     * never an Activity inside a background job.
     */
    private static final class TaskState {
        final Context context;
        final ExecutorService io = Executors.newSingleThreadExecutor();
        volatile boolean cancelled, running;
        volatile Thread activeThread;
        volatile String output = "";
        volatile AiDiscoveryActivity view;

        TaskState(Context context) { this.context = context.getApplicationContext(); }
        void attach(AiDiscoveryActivity activity) { view = activity; updateScreen(); }
        void detach(AiDiscoveryActivity activity) { if (view == activity) view = null; }
        void updateScreen() {
            AiDiscoveryActivity activity = view;
            if (activity == null) return;
            activity.runOnUiThread(() -> {
                if (view == activity && !activity.isFinishing() && !activity.isDestroyed()) {
                    activity.render(running, cancelled, output);
                }
            });
        }
        void progress(String text) { output = text; updateScreen(); }
        void submit(Callable<String> operation) {
            if (running) return;
            cancelled = false;
            running = true;
            progress("در حال پردازش…");
            io.execute(() -> {
                activeThread = Thread.currentThread();
                String result;
                try { result = operation.call(); }
                catch (Exception error) {
                    result = cancelled || error instanceof InterruptedException
                            || Thread.currentThread().isInterrupted()
                            ? "عملیات متوقف شد؛ نتایج محلی محفوظ‌اند."
                            : "عملیات کامل نشد؛ نتایج محلی محفوظ‌اند. " + safeMessage(error);
                }
                if (cancelled) result = "عملیات متوقف شد؛ نتایج محلی محفوظ‌اند.";
                output = result;
                activeThread = null;
                running = false;
                updateScreen();
            });
        }
        void cancel() {
            if (!running) return;
            cancelled = true;
            progress("در حال توقف…");
            Thread thread = activeThread;
            if (thread != null) thread.interrupt();
        }
        boolean stopped() { return cancelled || Thread.currentThread().isInterrupted(); }
        void shutdown() { cancel(); io.shutdownNow(); view = null; }
    }

    private void label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        root.addView(view);
    }
    private EditText field(String hint) {
        EditText view = new EditText(this);
        view.setHint(hint);
        root.addView(view);
        return view;
    }
    private Button button(String text, Runnable action) {
        Button view = new Button(this);
        view.setText(text);
        root.addView(view);
        view.setOnClickListener(v -> action.run());
        return view;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
        label("کشف هوشمند گروه‌ها");
        label("عبارت جست‌وجو با تأیید شما به سرور HTTPS فرستاده می‌شود. AI فقط عبارت‌های مرتبط پیشنهاد می‌کند؛ گروه‌ها از موتور جست‌وجوی موجود دریافت می‌شوند. هیچ پیام یا درخواست عضویتی ارسال نمی‌شود.");
        query = field("موضوع گروه؛ ۲ تا ۳۰۰ نویسه (چند موضوع را با ویرگول جدا کنید)");
        endpoint = field("سرور HTTPS سازگار با Ollama (اختیاری)");
        model = field("مدل");
        model.setText("qwen2.5:1.5b");
        search = button("جست‌وجوی خودکار، رتبه‌بندی و ذخیره", this::confirmSearch);
        stop = button("توقف", () -> task.cancel());
        button("نتایج محلی / آفلاین", () -> local(false));
        button("تاریخچه جست‌وجو", () -> local(true));
        button("پاک کردن داده‌های این حساب", () ->
                new AlertDialog.Builder(this)
                    .setMessage("نتایج محلی و تاریخچه این حساب پاک شوند؟")
                    .setNegativeButton("لغو", null)
                    .setPositiveButton("پاک کن", (d, w) -> task.submit(() -> {
                        RubikaClient client = account(task.context);
                        try (AiDiscoveryStore store = new AiDiscoveryStore(task.context)) {
                            store.clear(client.guid);
                        }
                        return "داده‌های محلی پاک شدند";
                    })).show());
        results = new TextView(this);
        results.setTextIsSelectable(true);
        root.addView(results);

        if (state != null) {
            query.setText(state.getString("discovery.query", ""));
            endpoint.setText(state.getString("discovery.endpoint", ""));
            model.setText(state.getString("discovery.model", "qwen2.5:1.5b"));
        }
        Object prior = getLastNonConfigurationInstance();
        task = prior instanceof TaskState ? (TaskState) prior : new TaskState(getApplicationContext());
        task.attach(this);
    }

    private void render(boolean running, boolean cancelled, String output) {
        search.setEnabled(!running);
        stop.setEnabled(running && !cancelled);
        results.setText(output);
    }

    private static RubikaClient account(Context context) throws Exception {
        RubikaClient client = Vault.load(context);
        if (client == null) throw new java.io.IOException("ابتدا وارد حساب شوید");
        return client;
    }
    private void confirmSearch() {
        final String q, server, selectedModel;
        try {
            q = DiscoveryRanking.query(query.getText().toString());
            server = endpoint.getText().toString().trim();
            selectedModel = model.getText().toString();
            if (!server.isEmpty()) {
                AiPromptPolicy.endpoint(server);
                AiPromptPolicy.model(selectedModel);
            }
        } catch (Exception error) {
            results.setText(error.getMessage());
            return;
        }
        Runnable run = () -> {
            TaskState worker = task;
            worker.submit(() -> discover(worker, q, server, selectedModel));
        };
        if (server.isEmpty()) run.run();
        else new AlertDialog.Builder(this)
                .setTitle("ارسال عبارت به سرور AI")
                .setMessage(q + "\n" + server)
                .setNegativeButton("لغو", null)
                .setPositiveButton("جست‌وجو", (d, w) -> run.run())
                .show();
    }

    private static String discover(TaskState task, String q, String server, String model) throws Exception {
        if (!OperationGate.acquire("ai-discovery")) {
            throw new java.io.IOException("ابتدا عملیات فعلی را متوقف کنید");
        }
        try {
            RubikaClient client = account(task.context);
            String[] typed = DiscoveryRanking.terms(q);
            String[] suggested = new String[0];
            String note = "";
            if (!server.isEmpty()) {
                try {
                    // The Ollama client echoes the full original query for compatibility.
                    // Typed terms already represent it, so it must not consume the AI budget.
                    List<String> suggestionsOnly = new ArrayList<>();
                    String originalKey = DiscoveryRanking.normalize(q);
                    for (String term : AiDiscoveryClient.expand(server, model, q)) {
                        if (!originalKey.equals(DiscoveryRanking.normalize(term))) {
                            suggestionsOnly.add(term);
                        }
                    }
                    suggested = suggestionsOnly.toArray(new String[0]);
                } catch (Exception error) {
                    if (task.stopped()) throw new InterruptedException("جست‌وجو متوقف شد");
                    note = "AI در دسترس نبود؛ جست‌وجوی اصلی انجام شد.\n";
                }
            }
            if (task.stopped()) throw new InterruptedException("جست‌وجو متوقف شد");
            final String[] expanded = suggested;
            String[] searchTerms = DiscoveryRanking.combine(typed, expanded, 12);
            task.progress("در حال جست‌وجو در " + searchTerms.length + " عبارت…");
            JSONObject data = DiscoveryData.empty();
            new DiscoveryEngine((method, input) -> client.call(method, input, false),
                    new DiscoveryEngine.PublicPages(), 1800)
                .run(data, searchTerms, true, false, new DiscoveryEngine.Progress() {
                    private int lastCount = -1;
                    public boolean stopped() { return task.stopped(); }
                    public void update(JSONObject state, String text) {
                        JSONArray found = state.optJSONArray("groups");
                        int count = found == null ? 0 : found.length();
                        if (count != lastCount) {
                            lastCount = count;
                            task.progress("در حال جست‌وجو… گروه‌های یافت‌شده: " + count);
                        }
                    }
                });
            if (task.stopped()) throw new InterruptedException("جست‌وجو متوقف شد");
            JSONArray groups = data.getJSONArray("groups");
            try (AiDiscoveryStore store = new AiDiscoveryStore(task.context)) {
                store.save(client.guid, q, groups, expanded);
            }
            return note + ranked(q, groups, expanded);
        } finally {
            OperationGate.release("ai-discovery");
        }
    }

    private static String ranked(String q, JSONArray groups) throws Exception {
        return ranked(q, groups, new String[0]);
    }
    private static String ranked(String q, JSONArray groups, String[] suggested) throws Exception {
        String[] typed;
        try { typed = DiscoveryRanking.terms(q); }
        catch (IllegalArgumentException error) {
            // An empty topic must not block viewing the user's saved groups.
            typed = new String[0];
        }
        return rankWithTerms(typed, suggested, groups);
    }
    private static String rankWithTerms(String[] typed, String[] suggested,
            JSONArray groups) throws Exception {
        List<JSONObject> rows = new ArrayList<>();
        for (int i = 0; i < groups.length(); i++) rows.add(groups.getJSONObject(i));
        rows.sort((a, b) -> {
            int byScore = Integer.compare(score(typed, suggested, b),
                    score(typed, suggested, a));
            return byScore != 0 ? byScore
                    : a.optString("name").compareToIgnoreCase(b.optString("name"));
        });
        int matched = 0;
        for (JSONObject group : rows) {
            if (score(typed, suggested, group) > 0) matched++;
        }
        StringBuilder out = new StringBuilder("گروه‌های یافت‌شده: " + rows.size()
                + "\nدارای تطبیق موضوعی: " + matched
                + "\nرتبه‌ها بر اساس عبارت اصلی و پیشنهادهای AI هستند؛ امتیاز درصد نیست و مجوز ارسال یا اعتبار گروه را تضمین نمی‌کند.\n");
        for (JSONObject group : rows) {
            String name = group.optString("name").trim();
            String identity = group.optString("link").trim();
            if (identity.isEmpty()) identity = group.optString("guid").trim();
            int direct = DiscoveryRanking.scoreAny(typed, group.optString("name"),
                    group.optString("keyword"), !group.optString("guid").isEmpty());
            int value = score(typed, suggested, group);
            String relevance = direct > 0 ? "تطبیق مستقیم" :
                    value > 0 ? "تطبیق با عبارت پیشنهادی" : "بدون تطبیق موضوعی";
            out.append("\n").append(name.isEmpty() ? "گروه بدون عنوان" : name)
                    .append(" • ").append(relevance).append(" • امتیاز ").append(value).append("\n");
            if (!identity.isEmpty()) out.append(identity).append("\n");
            String availability = group.optString("availability").trim();
            if (!availability.isEmpty()) out.append(availability).append("\n");
        }
        return out.toString();
    }
    private static int score(String[] typed, String[] suggested, JSONObject group) {
        return DiscoveryRanking.scoreWithSuggestions(typed, suggested,
                group.optString("name"), group.optString("keyword"),
                !group.optString("guid").isEmpty());
    }
    private void local(boolean history) {
        final String q = query.getText().toString();
        TaskState worker = task;
        worker.submit(() -> {
            RubikaClient client = account(worker.context);
            try (AiDiscoveryStore store = new AiDiscoveryStore(worker.context)) {
                if (history) return store.history(client.guid);
                AiDiscoveryStore.SearchContext context = store.searchContext(client.guid, q);
                return ranked(context.query, store.groups(client.guid), context.suggestions);
            }
        });
    }
    private static String safeMessage(Exception error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) return "اتصال و تنظیمات را بررسی کنید.";
        return message.length() > 180 ? message.substring(0, 180) : message;
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("discovery.query", query.getText().toString());
        out.putString("discovery.endpoint", endpoint.getText().toString());
        out.putString("discovery.model", model.getText().toString());
        super.onSaveInstanceState(out);
    }
    @Override public Object onRetainNonConfigurationInstance() { return task; }
    @Override protected void onDestroy() {
        task.detach(this);
        if (!isChangingConfigurations()) task.shutdown();
        super.onDestroy();
    }
}
