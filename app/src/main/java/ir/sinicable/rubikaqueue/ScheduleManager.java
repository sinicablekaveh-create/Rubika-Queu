package ir.sinicable.rubikaqueue;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONObject;

/** Independent explicit start times for account slots 1..3. */
public final class ScheduleManager {
 static final String ACTION="ir.sinicable.rubikaqueue.START_SCHEDULED_QUEUE";
 private ScheduleManager(){}
 private static int request(int slot){return 3910+AccountSlots.valid(slot);}
 private static PendingIntent pending(Context c,long created,int slot){
  Intent i=new Intent(c,ScheduledQueueReceiver.class).setAction(ACTION)
      .putExtra("job_created",created).putExtra("slot",AccountSlots.valid(slot));
  return PendingIntent.getBroadcast(c,request(slot),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 public static boolean permissionGranted(Context c){
  AlarmManager alarms=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  return Build.VERSION.SDK_INT<31 || alarms.canScheduleExactAlarms();
 }
 public static void cancel(Context c){cancel(c,1);}
 public static void cancel(Context c,int slot){
  slot=AccountSlots.valid(slot);
  AlarmManager alarms=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  Intent i=new Intent(c,ScheduledQueueReceiver.class).setAction(ACTION).putExtra("slot",slot);
  PendingIntent old=PendingIntent.getBroadcast(c,request(slot),i,PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
  if(old!=null){alarms.cancel(old);old.cancel();}
 }
 public static boolean schedule(Context c,JSONObject job){return schedule(c,job,1);}
 public static boolean schedule(Context c,JSONObject job,int slot){
  slot=AccountSlots.valid(slot);
  long when=job.optLong("scheduled_start_at",0);
  if(when<=System.currentTimeMillis()||!permissionGranted(c))return false;
  AlarmManager alarms=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending(c,job.optLong("created"),slot));
  return true;
 }
 public static void rearm(Context c){for(int slot=1;slot<=3;slot++)rearm(c,slot);}
 public static void rearm(Context c,int slot){
  try{
   JSONObject job=JobStore.load(c,slot);
   if(job==null||job.optLong("scheduled_start_at")<=0||!JobStore.hasWork(job)||!permissionGranted(c))return;
   long when=Math.max(job.optLong("scheduled_start_at"),System.currentTimeMillis()+30000L);
   AlarmManager alarms=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
   alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending(c,job.optLong("created"),slot));
  }catch(Exception ignored){}
 }
}
