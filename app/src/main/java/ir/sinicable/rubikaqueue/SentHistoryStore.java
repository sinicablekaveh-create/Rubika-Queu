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
 public SentHistoryStore(Context c){super(c,"rubika_sent_history.db",null,1);}
 @Override public void onCreate(SQLiteDatabase db){
  db.execSQL("CREATE TABLE sent_log (id INTEGER PRIMARY KEY AUTOINCREMENT,owner TEXT NOT NULL,job_created INTEGER NOT NULL,source_row INTEGER NOT NULL,kind TEXT,target TEXT,name TEXT,message TEXT,message_id TEXT,confirmation TEXT,status TEXT,sent_at INTEGER NOT NULL)");
  db.execSQL("CREATE UNIQUE INDEX uniq_sent_attempt ON sent_log(owner,job_created,source_row)");
  db.execSQL("CREATE INDEX sent_by_owner_date ON sent_log(owner,sent_at DESC,id DESC)");
 }
 @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}
 static boolean isRecorded(JSONObject r){String status=r.optString("status");return status.equals("sent")||status.equals("accepted")||status.equals("sent_manual");}
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
 public void reconcile(JSONObject job)throws Exception{
  if(job==null)return;
  JSONArray rows=job.getJSONArray("recipients");
  for(int i=0;i<rows.length();i++)if(isRecorded(rows.getJSONObject(i)))archive(job,rows.getJSONObject(i));
 }
 public int total(String owner){
  try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM sent_log WHERE owner=?",new String[]{owner})){
   return c.moveToFirst()?c.getInt(0):0;
  }
 }
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
 public static String format(long time){return time>0?new SimpleDateFormat("yyyy/MM/dd HH:mm:ss",Locale.getDefault()).format(new Date(time)):"نامشخص";}
}
