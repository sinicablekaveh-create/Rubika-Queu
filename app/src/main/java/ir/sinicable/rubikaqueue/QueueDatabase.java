package ir.sinicable.rubikaqueue;
import android.content.*;
import android.database.sqlite.*;
public class QueueDatabase extends SQLiteOpenHelper {
 public QueueDatabase(Context c){super(c,"rubika_queue.db",null,1);}
 public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE queue_items(id INTEGER PRIMARY KEY AUTOINCREMENT,target TEXT,message TEXT,type TEXT,status TEXT,retry_count INTEGER,next_retry_time INTEGER,error TEXT,created_at INTEGER,updated_at INTEGER)");}
 public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}
}
