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

/** Local, append-only receipt log. Unconfirmed/unknown sends are never labeled delivered. */
public final class SentHistoryStore extends SQLiteOpenHelper {
 /** Creates the version-one SQLite helper for the independent sent-receipt archive. */
 public SentHistoryStore(Context c){super(c,"rubika_sent_history.db",null,1);}
 /** Creates receipt storage with unique job-row identities and an index for each owner's recent history. */
 @Override public void onCreate(SQLiteDatabase db){
  db.execSQL("CREATE TABLE sent_log (id INTEGER PRIMARY KEY AUTOINCREMENT,owner TEXT NOT NULL,job_created INTEGER NOT NULL,source_row INTEGER NOT NULL,kind TEXT,target TEXT,name TEXT,message TEXT,message_id TEXT,confirmation TEXT,status TEXT,sent_at INTEGER NOT NULL)");
  db.execSQL("CREATE UNIQUE INDEX uniq_sent_attempt ON sent_log(owner,job_created,source_row)");
  db.execSQL("CREATE INDEX sent_by_owner_date ON sent_log(owner,sent_at DESC,id DESC)");
 }
 /** Performs no migration because only schema version one is currently defined. */
 @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}
 /** Returns whether the row records a server-acknowledged or manually confirmed send eligible for archiving. */
 static boolean isRecorded(JSONObject r){String status=r.optString("status");return status.equals("sent")||status.equals("accepted")||status.equals("sent_manual");}
 /** Archives an eligible recipient outcome, ignoring duplicates identified by owner, job creation time and source row. */
 public synchronized void archive(JSONObject job,JSONObject r){
  if(!isRecorded(r))return;
  ContentValues v=new ContentValues();
  v.put("owner",job.optString("owner"));v.put("job_created",job.optLong("created"));v.put("source_row",r.optInt("row"));
  v.put("kind",r.optString("kind","contact"));v.put("target","group".equals(r.optString("kind"))?r.optString("guid"):r.optString("phone"));
  v.put("name",r.optString("name"));v.put("message",job.optString("message"));
  v.put("message_id",r.optString("message_id"));v.put("confirmation",r.optString("confirmation"));
  v.put("status",r.optString("status"));v.put("sent_at",r.optLong("sent_at",r.optLong("reviewed_at",0)));
  getWritableDatabase().insertWithOnConflict("sent_log",null,v,SQLiteDatabase.CONFLICT_IGNORE);
 }
 /** Copies recorded outcomes from a saved job into history, recovering missing archive entries after interruption. */
 public void reconcile(JSONObject job)throws Exception{
  if(job==null)return;
  JSONArray rows=job.getJSONArray("recipients");
  for(int i=0;i<rows.length();i++)if(isRecorded(rows.getJSONObject(i)))archive(job,rows.getJSONObject(i));
 }
 /** Returns the number of archived receipts belonging to the supplied account GUID. */
 public int total(String owner){
  try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM sent_log WHERE owner=?",new String[]{owner})){
   return c.moveToFirst()?c.getInt(0):0;
  }
 }
 /** Formats a zero-based page of the owner's receipts, newest first, retaining distinctions between confirmation types. */
 public String page(String owner,int page,int limit){
  StringBuilder b=new StringBuilder();
  String query="SELECT kind,target,name,message,message_id,confirmation,status,sent_at FROM sent_log WHERE owner=? ORDER BY sent_at DESC,id DESC LIMIT ? OFFSET ?";
  try(Cursor c=getReadableDatabase().rawQuery(query,new String[]{owner,Integer.toString(limit),Integer.toString(page*limit)})){
   int n=page*limit;
   while(c.moveToNext()){
    n++;
    b.append(n).append(". ");
    b.append("group".equals(c.getString(0))?"گروه: ":"شماره: +").append(c.getString(1));
    String name=c.getString(2);if(name!=null&&!name.isEmpty())b.append(" • ").append(name);
    b.append("\n");String status=c.getString(6);
    if("sent".equals(status))b.append("ثبت ارسال با شناسه پیام");
    else if("sent_manual".equals(status))b.append("تأیید دستی در روبیکا");
    else b.append("پذیرفته‌شده توسط سرور؛ تحویل تأیید نشده");
    b.append(" • ").append(format(c.getLong(7)));
    String id=c.getString(4);if(id!=null&&!id.isEmpty())b.append("\nشناسه: ").append(id);
    String msg=c.getString(3);if(msg!=null&&!msg.isEmpty())b.append("\nمتن: ").append(msg.length()>160?msg.substring(0,160)+"…":msg);
    b.append("\n\n");
   }
  }
  return b.toString();
 }
 /** Formats up to the requested number of recent receipts for the owner's compact history display. */
 public String shortLatest(String owner,int max){
  StringBuilder b=new StringBuilder();
  try(Cursor c=getReadableDatabase().rawQuery("SELECT kind,target,name,status,sent_at FROM sent_log WHERE owner=? ORDER BY sent_at DESC,id DESC LIMIT ?",new String[]{owner,Integer.toString(max)})){
   while(c.moveToNext()){
    b.append("• ").append("group".equals(c.getString(0))?"گروه: ":"+").append(c.getString(1));
    String name=c.getString(2);if(name!=null&&!name.isEmpty())b.append(" (").append(name).append(")");
    b.append(" • ").append(format(c.getLong(4))).append("\n");
   }
  }
  return b.toString();
 }
 /** Formats a positive millisecond timestamp in local time, or returns the unknown-time label. */
 public static String format(long time){return time>0?new SimpleDateFormat("yyyy/MM/dd HH:mm:ss",Locale.getDefault()).format(new Date(time)):"نامشخص";}
}
