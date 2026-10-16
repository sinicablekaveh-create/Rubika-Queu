package ir.sinicable.rubikaqueue;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;

/** Local approvals are never populated from search, imports or queue data. */
public final class AllowedDestinationStore {
    private static final String PREFS = "approved_destinations_v1";
    private AllowedDestinationStore() {}

    private static String ownerKey(String owner) {
        if (owner == null || !owner.matches("u[A-Za-z0-9_-]{11,127}")) {
            throw new SecurityException("شناسه حساب معتبر نیست؛ ارسال مسدود است");
        }
        return "owner:" + owner;
    }

    public static Set<String> read(Context context, String owner) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(ownerKey(owner), Collections.emptySet()));
    }

    public static void approve(Context context, String owner, String key) {
        validateStoredKey(key);
        Set<String> updated = read(context, owner);
        updated.add(key);
        if (!context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putStringSet(ownerKey(owner), updated).commit()) {
            throw new IllegalStateException("تأیید مقصد ذخیره نشد");
        }
    }

    public static void revoke(Context context, String owner, String key) {
        validateStoredKey(key);
        Set<String> updated = read(context, owner);
        updated.remove(key);
        if (!context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putStringSet(ownerKey(owner), updated).commit()) {
            throw new IllegalStateException("حذف مقصد ذخیره نشد");
        }
    }

    private static void validateStoredKey(String key) {
        if (key == null || (!key.startsWith("group:") && !key.startsWith("phone:"))) {
            throw new IllegalArgumentException("مقصد نامعتبر است");
        }
        String kind = key.startsWith("group:") ? "group" : "phone";
        if (!AllowedDestinationPolicy.key(kind, key.substring(kind.length() + 1)).equals(key)) {
            throw new IllegalArgumentException("مقصد نامعتبر است");
        }
    }

    /** Enforced before resolving recipients and again immediately before send. */
    public static void requireApproved(Context context, String owner, JSONObject row) throws Exception {
        if (row == null) throw new SecurityException("گیرنده مشخص نیست");
        String rowKind = row.optString("kind", "");
        final String kind;
        final String id;
        if ("group".equals(rowKind)) {
            kind = "group";
            id = row.optString("guid", "");
        } else if (rowKind.isEmpty() || "phone".equals(rowKind)) {
            kind = "phone";
            id = Phones.normalize(row.optString("phone", ""));
        } else {
            throw new SecurityException("نوع گیرنده پشتیبانی نمی‌شود");
        }
        if (!AllowedDestinationPolicy.isApproved(read(context, owner), kind, id)) {
            throw new SecurityException("مقصد در فهرست تأییدشده نیست؛ صف بدون ارسال متوقف شد");
        }
    }
}
