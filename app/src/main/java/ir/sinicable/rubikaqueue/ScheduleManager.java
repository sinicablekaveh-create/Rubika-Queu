package ir.sinicable.rubikaqueue;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONObject;

/** Schedules the user's explicit send time; never schedules the individual recipients. */
public final class ScheduleManager {
 private static final int REQUEST=3910;
 static final String ACTION="ir.sinicable.rubikaqueue.START_SCHEDULED_QUEUE";
 private ScheduleManager(){}
 private static PendingIntent pending(Context c,long created){
  Intent i=new Intent(c,ScheduledQueueReceiver.class).setAction(ACTION).putExtra("job_created",created);
  return PendingIntent.getBroadcast(c,REQUEST,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 public static boolean permissionGranted(Context c){
  AlarmManager alarms=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  return Build.VERSION.SDK_INT<31 || alarms.canScheduleExactAlarms();
 }
 public static void cancel(Context c){
  AlarmManager alarms=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  Intent i=new Intent(c,ScheduledQueueReceiver.class).setAction(ACTION);
  PendingIntent old=PendingIntent.getBroadcast(c,REQUEST,i,PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
  if(old!=null){alarms.cancel(old);old.cancel();}
 }
 public static boolean schedule(Context c,JSONObject job){
  long when=job.optLong("scheduled_start_at",0);
  if(when<=System.currentTimeMillis())return false;
  if(!permissionGranted(c))return false;
  AlarmManager alarms=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending(c,job.optLong("created")));
  return true;
 }
 public static void rearm(Context c){
  try{
   JSONObject job=JobStore.load(c);
   if(job==null||job.optLong("scheduled_start_at")<=0||JobStore.next(job)<0||JobStore.uncertain(job)||!permissionGranted(c))return;
   long when=Math.max(job.optLong("scheduled_start_at"),System.currentTimeMillis()+30000L);
   AlarmManager alarms=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
   alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending(c,job.optLong("created")));
  }catch(Exception ignored){}
 }
}
