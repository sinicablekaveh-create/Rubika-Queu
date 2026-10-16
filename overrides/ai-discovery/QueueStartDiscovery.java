package ir.sinicable.rubikaqueue;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.IOException;

/** Runs the existing read-only discovery pass before queue delivery begins. */
public final class QueueStartDiscovery {
    public interface Progress {
        boolean stopped();
        void update(String note) throws Exception;
    }

    private QueueStartDiscovery() {}

    public static boolean enabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("discovery", Context.MODE_PRIVATE);
        return prefs.getBoolean(QueueStartDiscoveryPolicy.PREFERENCE, true);
    }

    public static void run(Context context, RubikaClient client, Progress progress) throws Exception {
        if (client == null || client.privateKey == null) throw new IOException("ابتدا وارد حساب شوید");
        SharedPreferences prefs = context.getSharedPreferences("discovery", Context.MODE_PRIVATE);
        String[] terms = LeadExtractor.terms(prefs.getString("terms", LeadExtractor.DEFAULT_TERMS));
        boolean groups = prefs.getBoolean("groups", true);
        boolean contacts = prefs.getBoolean("contacts", true);
        if (!QueueStartDiscoveryPolicy.shouldRun(enabled(context), groups, contacts, terms)) return;

        JSONObject state = DiscoveryStore.load(context);
        if (!state.optString("owner", client.guid).equals(client.guid)) state = DiscoveryData.empty();
        state.put("owner", client.guid);
        DiscoveryEngine engine = new DiscoveryEngine(
                (method, input) -> client.call(method, input, false),
                new DiscoveryEngine.PublicPages(), 1800);
        engine.run(state, terms, groups, contacts, new DiscoveryEngine.Progress() {
            @Override public boolean stopped() { return progress.stopped(); }
            @Override public void update(JSONObject current, String note) throws Exception {
                current.put("note", note);
                DiscoveryStore.save(context, current);
                progress.update(note);
            }
        });
        state.put("note", "جست‌وجوی خودکار هنگام شروع صف پایان یافت؛ "
                + DiscoveryData.summary(state).replace('\n', ' '));
        DiscoveryStore.save(context, state);
        String uri = prefs.getString("output", "");
        if (!uri.isEmpty() && !progress.stopped()) {
            java.io.File temp = new java.io.File(context.getFilesDir(), "discovery-results.xlsx");
            try (java.io.OutputStream out = new java.io.FileOutputStream(temp)) {
                ResearchXlsx.write(state, out);
            }
            try (java.io.InputStream in = new java.io.FileInputStream(temp);
                 java.io.OutputStream out = context.getContentResolver()
                         .openOutputStream(android.net.Uri.parse(uri), "wt")) {
                if (out == null) throw new IOException("فایل خروجی جست‌وجو باز نشد");
                byte[] buffer = new byte[8192];
                int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            }
        }
    }
}
