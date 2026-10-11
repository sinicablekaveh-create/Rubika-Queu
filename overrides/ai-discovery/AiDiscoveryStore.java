package ir.sinicable.rubikaqueue;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;

/** Independent account-scoped cache; never changes the queue or legacy discovery store. */
public final class AiDiscoveryStore extends SQLiteOpenHelper implements AutoCloseable {
    public AiDiscoveryStore(Context context) { super(context, "ai-discovery.db", null, 2); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE groups_cache(owner TEXT NOT NULL, identity TEXT NOT NULL, data TEXT NOT NULL, PRIMARY KEY(owner,identity))");
        db.execSQL("CREATE TABLE searches(id INTEGER PRIMARY KEY, owner TEXT NOT NULL, query TEXT NOT NULL, searched_at INTEGER NOT NULL, result_count INTEGER NOT NULL, suggested_terms TEXT NOT NULL DEFAULT '[]')");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL("ALTER TABLE searches ADD COLUMN suggested_terms TEXT NOT NULL DEFAULT '[]'");
            return;
        }
        throw new IllegalStateException("Unsupported discovery schema upgrade");
    }
    public void save(String owner, String query, JSONArray groups) throws Exception {
        save(owner, query, groups, new String[0]);
    }
    public void save(String owner, String query, JSONArray groups, String[] suggestions)
            throws Exception {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // Match both aliases before saving. A group may first be discovered by link
            // and later by GUID (or the reverse); replacing on a single key duplicates it.
            Map<String, JSONObject> known = new LinkedHashMap<>();
            try (Cursor c = db.rawQuery(
                    "SELECT identity,data FROM groups_cache WHERE owner=?", new String[]{owner})) {
                while (c.moveToNext()) known.put(c.getString(0), new JSONObject(c.getString(1)));
            }
            for (int i = 0; i < groups.length(); i++) {
                JSONObject incoming = groups.getJSONObject(i);
                String guid = incoming.optString("guid").trim();
                String link = incoming.optString("link").trim();
                if (guid.isEmpty() && link.isEmpty()) continue;
                // A reused public link may refer to multiple identified groups. A
                // link-only observation cannot select either GUID without guessing.
                if (guid.isEmpty() && !link.isEmpty()) {
                    java.util.HashSet<String> candidates = new java.util.HashSet<>();
                    for (JSONObject previous : known.values()) {
                        if (link.equals(previous.optString("link").trim())) {
                            String previousGuid = previous.optString("guid").trim();
                            if (!previousGuid.isEmpty()) candidates.add(previousGuid);
                        }
                    }
                    if (candidates.size() > 1) continue;
                }
                JSONObject merged = new JSONObject(incoming.toString());
                ArrayList<String> stale = new ArrayList<>();
                for (Map.Entry<String, JSONObject> row : known.entrySet()) {
                    JSONObject previous = row.getValue();
                    String previousGuid = previous.optString("guid").trim();
                    String previousLink = previous.optString("link").trim();
                    // Different identified GUIDs cannot be silently fused merely
                    // because a public link has been reused or changed ownership.
                    if (!guid.isEmpty() && !previousGuid.isEmpty()
                            && !guid.equals(previousGuid)) continue;
                    if ((!guid.isEmpty() && guid.equals(previousGuid))
                            || (!link.isEmpty() && link.equals(previousLink))
                            || (!guid.isEmpty() && guid.equals(row.getKey()))
                            || (!link.isEmpty() && link.equals(row.getKey()))) {
                        stale.add(row.getKey());
                        if (guid.isEmpty() && !previousGuid.isEmpty()) {
                            guid = previousGuid;
                            merged.put("guid", guid);
                        }
                        if (link.isEmpty() && !previousLink.isEmpty()) {
                            link = previousLink;
                            merged.put("link", link);
                        }
                    }
                }
                for (String identity : stale) {
                    db.delete("groups_cache", "owner=? AND identity=?", new String[]{owner, identity});
                    known.remove(identity);
                }
                String identity = guid.isEmpty() ? link : guid;
                ContentValues v = new ContentValues();
                v.put("owner", owner); v.put("identity", identity); v.put("data", merged.toString());
                db.insertWithOnConflict("groups_cache", null, v, SQLiteDatabase.CONFLICT_REPLACE);
                known.put(identity, merged);
            }
            ContentValues h = new ContentValues();
            h.put("owner", owner); h.put("query", query); h.put("searched_at", System.currentTimeMillis()); h.put("result_count", groups.length());
            JSONArray accepted = new JSONArray();
            for (String value : DiscoveryRanking.combine(new String[0], suggestions, 4)) {
                accepted.put(value);
            }
            h.put("suggested_terms", accepted.toString());
            db.insertOrThrow("searches", null, h);
            db.execSQL("DELETE FROM searches WHERE owner=? AND id NOT IN (SELECT id FROM searches WHERE owner=? ORDER BY id DESC LIMIT 100)", new Object[]{owner, owner});
            db.execSQL("DELETE FROM groups_cache WHERE owner=? AND rowid NOT IN (SELECT rowid FROM groups_cache WHERE owner=? ORDER BY rowid DESC LIMIT 1000)", new Object[]{owner, owner});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    /** Suggested terms belong to the account and the exact originating query. */
    public static final class SearchContext {
        public final String query;
        public final String[] suggestions;
        SearchContext(String query, String[] suggestions) {
            this.query = query;
            this.suggestions = suggestions;
        }
    }
    public SearchContext searchContext(String owner, String requestedQuery) throws Exception {
        String requested = requestedQuery == null ? "" : requestedQuery.trim();
        boolean specific = !requested.isEmpty();
        String sql = specific
                ? "SELECT query,suggested_terms FROM searches WHERE owner=? AND query=? ORDER BY id DESC LIMIT 1"
                : "SELECT query,suggested_terms FROM searches WHERE owner=? ORDER BY id DESC LIMIT 1";
        String[] args = specific ? new String[]{owner, requested} : new String[]{owner};
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args)) {
            if (cursor.moveToFirst()) {
                JSONArray stored = new JSONArray(cursor.getString(1));
                String[] suggestions = new String[stored.length()];
                for (int i = 0; i < stored.length(); i++) suggestions[i] = stored.getString(i);
                return new SearchContext(cursor.getString(0), suggestions);
            }
        }
        return new SearchContext(requested, new String[0]);
    }
    public JSONArray groups(String owner) throws Exception {
        JSONArray result = new JSONArray();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT data FROM groups_cache WHERE owner=?", new String[]{owner})) {
            while (c.moveToNext()) result.put(new JSONObject(c.getString(0)));
        }
        return result;
    }
    public String history(String owner) {
        StringBuilder out = new StringBuilder();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);
        try (Cursor c = getReadableDatabase().rawQuery("SELECT query,searched_at,result_count FROM searches WHERE owner=? ORDER BY id DESC LIMIT 100", new String[]{owner})) {
            while (c.moveToNext()) out.append(c.getString(0)).append(" • ").append(format.format(new Date(c.getLong(1)))).append(" • ").append(c.getInt(2)).append('\n');
        }
        return out.length() == 0 ? "تاریخچه خالی است" : out.toString();
    }
    public void clear(String owner) {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try { db.delete("searches", "owner=?", new String[]{owner}); db.delete("groups_cache", "owner=?", new String[]{owner}); db.setTransactionSuccessful(); }
        finally { db.endTransaction(); }
    }
}
