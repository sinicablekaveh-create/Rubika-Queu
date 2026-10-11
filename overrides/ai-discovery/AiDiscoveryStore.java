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
    public AiDiscoveryStore(Context context) { super(context, "ai-discovery.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE groups_cache(owner TEXT NOT NULL, identity TEXT NOT NULL, data TEXT NOT NULL, PRIMARY KEY(owner,identity))");
        db.execSQL("CREATE TABLE searches(id INTEGER PRIMARY KEY, owner TEXT NOT NULL, query TEXT NOT NULL, searched_at INTEGER NOT NULL, result_count INTEGER NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Unsupported discovery schema upgrade");
    }
    public void save(String owner, String query, JSONArray groups) throws Exception {
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
                JSONObject merged = new JSONObject(incoming.toString());
                ArrayList<String> stale = new ArrayList<>();
                for (Map.Entry<String, JSONObject> row : known.entrySet()) {
                    JSONObject previous = row.getValue();
                    String previousGuid = previous.optString("guid").trim();
                    String previousLink = previous.optString("link").trim();
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
            db.insertOrThrow("searches", null, h);
            db.execSQL("DELETE FROM searches WHERE owner=? AND id NOT IN (SELECT id FROM searches WHERE owner=? ORDER BY id DESC LIMIT 100)", new Object[]{owner, owner});
            db.execSQL("DELETE FROM groups_cache WHERE owner=? AND rowid NOT IN (SELECT rowid FROM groups_cache WHERE owner=? ORDER BY rowid DESC LIMIT 1000)", new Object[]{owner, owner});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
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
