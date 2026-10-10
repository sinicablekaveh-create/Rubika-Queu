package ir.sinicable.rubikaqueue;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class QueueManager {
    private final QueueDatabase db;

    public QueueManager(Context context) { db = new QueueDatabase(context); }
    public QueueDatabase database() { return db; }

    /** Claim one due item atomically, so concurrent workers cannot claim it twice. */
    public synchronized QueueItem nextReady() {
        SQLiteDatabase sql = db.getWritableDatabase();
        long now = System.currentTimeMillis();
        Cursor cursor = null;
        sql.beginTransaction();
        try {
            cursor = sql.rawQuery(
                "SELECT id,target,message,type,status,retry_count,next_retry_time,error " +
                "FROM queue_items WHERE status='PENDING' " +
                "OR (status='RETRY_WAIT' AND next_retry_time<=?) " +
                "ORDER BY created_at ASC,id ASC LIMIT 1",
                new String[]{Long.toString(now)});
            if (!cursor.moveToFirst()) {
                sql.setTransactionSuccessful();
                return null;
            }
            QueueItem item = new QueueItem();
            item.id = cursor.getLong(0);
            item.target = cursor.getString(1);
            item.message = cursor.getString(2);
            item.type = cursor.getString(3);
            item.status = cursor.getString(4);
            item.retryCount = cursor.getInt(5);
            item.nextRetryTime = cursor.getLong(6);
            item.error = cursor.getString(7);

            ContentValues values = new ContentValues();
            values.put("status", "SENDING");
            values.put("updated_at", now);
            int changed = sql.update("queue_items", values,
                "id=? AND status=?",
                new String[]{Long.toString(item.id), item.status});
            sql.setTransactionSuccessful();
            if (changed != 1) return null;
            item.status = "SENDING";
            return item;
        } finally {
            if (cursor != null) cursor.close();
            sql.endTransaction();
        }
    }
}
