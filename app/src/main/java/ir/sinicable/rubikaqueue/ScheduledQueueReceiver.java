package ir.sinicable.rubikaqueue;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.json.JSONObject;

/** Recovery/scheduling for three independent account slots. */
public class ScheduledQueueReceiver extends BroadcastReceiver {
 public static final String ACTION_RESUME="ir.sinicable.rubikaqueue.action.RESUME_QUEUE";

 public static void notifyRecovery(Context context,String text){notifyRecovery(context,1,text);}
 public static void notifyRecovery(Context context,int slot,String text){
  try{
   slot=AccountSlots.valid(slot);
   NotificationManager manager=context.getSystemService(NotificationManager.class);
   String channel="queue_recovery_"+slot;
   manager.createNotificationChannel(new NotificationChannel(channel,"بازیابی صف حساب "+slot,NotificationManager.IMPORTANCE_DEFAULT));

   Intent resume=new Intent(context,ScheduledQueueReceiver.class).setAction(ACTION_RESUME).putExtra("slot",slot);
   PendingIntent resumePi=PendingIntent.getBroadcast(context,930+slot,resume,
       PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   PendingIntent open=PendingIntent.getActivity(context,940+slot,new Intent(context,MultiAccountActivity.class),
       PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

   manager.notify(930+slot,new Notification.Builder(context,channel)
       .setSmallIcon(R.drawable.icon)
       .setContentTitle("صف روبیکا حساب "+slot+" آماده ادامه است")
       .setContentText(text)
       .setStyle(new Notification.BigTextStyle().bigText(text))
       .setContentIntent(open)
       .addAction(new Notification.Action.Builder(null,"ادامه صف",resumePi).build())
       .setAutoCancel(true).build());
  }catch(Exception ignored){}
 }

 private static boolean resumable(Context context,int slot)throws Exception{
  if(!context.getSharedPreferences(AccountSlots.recoveryPrefs(slot),Context.MODE_PRIVATE)
      .getBoolean("resume",false))return false;
  if(context.getSharedPreferences(AccountSlots.recoveryPrefs(slot),Context.MODE_PRIVATE)
      .getBoolean("paused",false))return false;
  JSONObject job=JobStore.load(context,slot);
  return job!=null&&job.optBoolean("consented",false)&&
      job.optString("media_type","").isEmpty()&&JobStore.hasWork(job);
 }

 private static void startOrNotify(Context context,int slot,String fallback)throws Exception{
  if(!resumable(context,slot))return;
  try{
   context.startForegroundService(new Intent(context,AccountSlots.serviceClass(slot)));
  }catch(Exception denied){
   notifyRecovery(context,slot,fallback);
  }
 }

 @Override public void onReceive(Context context,Intent intent){
  if(intent==null)return;
  String action=intent.getAction();

  if(ACTION_RESUME.equals(action)){
   int slot=intent.getIntExtra("slot",1);
   try{
    if(!resumable(context,slot))return;
    if(Vault.load(context,slot)==null){
     notifyRecovery(context,slot,"برای ادامه صف ابتدا برنامه را باز کنید و حساب "+slot+" را دوباره وارد کنید.");
     return;
    }
    context.startForegroundService(new Intent(context,AccountSlots.serviceClass(slot)));
   }catch(Exception failure){
    notifyRecovery(context,slot,"اندروید شروع خودکار را نپذیرفت؛ صفحه سه حساب را باز کنید.");
   }
   return;
  }

  if(Intent.ACTION_BOOT_COMPLETED.equals(action)
      ||Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
      ||"android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED".equals(action)){
   ScheduleManager.rearm(context);
   for(int slot=1;slot<=3;slot++){
    try{
     if(!resumable(context,slot))continue;
     JSONObject saved=JobStore.load(context,slot);
     long planned=saved.optLong("scheduled_start_at",0);
     if(planned>System.currentTimeMillis())continue;
     if(Vault.load(context,slot)==null){
      notifyRecovery(context,slot,"صف حساب "+slot+" محفوظ است؛ برای ورود مجدد برنامه را باز کنید.");
      continue;
     }
     startOrNotify(context,slot,"صف حساب "+slot+" محفوظ است. برای ادامه، «ادامه صف» را بزنید.");
    }catch(Exception unavailable){
     notifyRecovery(context,slot,"صف حساب "+slot+" محفوظ است؛ برای ادامه برنامه را باز کنید.");
    }
   }
   return;
  }

  if(!ScheduleManager.ACTION.equals(action))return;
  int slot=intent.getIntExtra("slot",1);
  try{
   JSONObject job=JobStore.load(context,slot);
   if(job==null||job.optLong("created")!=intent.getLongExtra("job_created",-1))return;
   long start=job.optLong("scheduled_start_at",0);
   if(start<=0||System.currentTimeMillis()+1000<start||!JobStore.hasWork(job))return;
   startOrNotify(context,slot,"زمان شروع صف حساب "+slot+" رسیده است؛ برای ادامه «ادامه صف» را بزنید.");
  }catch(Exception failure){
   notifyRecovery(context,slot,"زمان شروع صف حساب "+slot+" رسیده است؛ صفحه سه حساب را باز کنید.");
  }
 }
}
