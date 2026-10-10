package ir.sinicable.rubikaqueue;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.json.JSONObject;

/** Starts a still-pending job only for the user's chosen date/time. */
public class ScheduledQueueReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){
  if(intent==null)return;
  String action=intent.getAction();
  if(Intent.ACTION_BOOT_COMPLETED.equals(action)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
       ||"android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED".equals(action)){
   ScheduleManager.rearm(context);return;
  }
  if(!ScheduleManager.ACTION.equals(action))return;
  try{
   JSONObject job=JobStore.load(context);
   if(job==null||job.optLong("created")!=intent.getLongExtra("job_created",-1))return;
   long start=job.optLong("scheduled_start_at",0);
   if(start<=0||System.currentTimeMillis()+1000<start||JobStore.next(job)<0||JobStore.uncertain(job))return;
   // Do not mark as started until QueueService has checked the job and account.
   context.startForegroundService(new Intent(context,QueueService.class));
  }catch(Exception failure){
   // Starting foreground work may be restricted on certain devices; never pretend a send occurred.
   try{
    JSONObject job=JobStore.load(context);
    if(job!=null){job.put("note","زمان ارسال رسید اما شروع خودکار توسط دستگاه متوقف شد؛ برای ادامه برنامه را باز کنید");JobStore.save(context,job);}
    NotificationManager manager=context.getSystemService(NotificationManager.class);
    manager.createNotificationChannel(new NotificationChannel("queue_schedule","زمان‌بندی صف",NotificationManager.IMPORTANCE_DEFAULT));
    PendingIntent open=PendingIntent.getActivity(context,0,new Intent(context,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    manager.notify(92,new Notification.Builder(context,"queue_schedule").setSmallIcon(R.drawable.icon)
      .setContentTitle("زمان ارسال صف روبیکا رسید").setContentText("برای ادامهٔ ارسال برنامه را باز کنید")
      .setContentIntent(open).setAutoCancel(true).build());
   }catch(Exception ignored){}
  }
 }
}
