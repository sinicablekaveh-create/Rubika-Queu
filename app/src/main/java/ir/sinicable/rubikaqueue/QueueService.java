package ir.sinicable.rubikaqueue;
import android.app.*;
import android.content.*;
import android.os.*;
import org.json.*;

public class QueueService extends Service {
 public static volatile boolean running=false;
 private volatile boolean cancel=false;private Thread worker;private PowerManager.WakeLock wake;
 private static final int NOTICE=71;
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent i,int flags,int id){
  if(i!=null&&"STOP".equals(i.getAction())){cancel=true;if(worker!=null)worker.interrupt();else stopSelf();return START_NOT_STICKY;}
  if(running)return START_NOT_STICKY;
  if(!OperationGate.acquire("queue")){stopSelf();return START_NOT_STICKY;}
  running=true;cancel=false;
  try{NotificationChannel channel=new NotificationChannel("queue","صف ارسال روبیکا",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager.class).createNotificationChannel(channel);startForeground(NOTICE,notification("در حال آماده‌سازی صف"));
   wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"RubikaQueue:Sending");wake.acquire(10*60*60*1000L);
   worker=new Thread(this::runQueue,"message-queue");worker.start();
  }catch(Exception e){running=false;OperationGate.release("queue");stopSelf();}
  return START_NOT_STICKY;
 }
 Notification notification(String text){
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,QueueService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  return new Notification.Builder(this,"queue").setContentTitle("صف پیام روبیکا • آزمایشی").setContentText(text).setSmallIcon(R.drawable.icon).setOngoing(true).setContentIntent(open).addAction(new Notification.Action.Builder(null,"توقف",stop).build()).build();
 }
 void notice(String s){getSystemService(NotificationManager.class).notify(NOTICE,notification(s));}
 void checkpoint(JSONObject j,String text)throws Exception{j.put("note",text);JobStore.save(this,j);notice(text);}
 private void runQueue(){
  JSONObject job=null;JSONObject current=null;boolean dispatch=false;
  try{
   job=JobStore.load(this);RubikaClient client=Vault.load(this);
   if(job==null||client==null)throw new Exception("اکسل یا ورود حساب آماده نیست");
   if(!client.guid.equals(job.optString("owner")))throw new Exception("حساب صف با حساب فعلی متفاوت است");
   if(JobStore.uncertain(job))throw new Exception("ابتدا مورد نامشخص را در روبیکا بررسی کنید");
   if(job.optLong("scheduled_start_at",0)>System.currentTimeMillis()){
    if(!ScheduleManager.schedule(this,job))checkpoint(job,"زمان‌بندی ذخیره شده ولی مجوز زمان‌بندی دقیق فعال نیست");
    else checkpoint(job,"صف زمان‌بندی شد؛ ارسال فعلاً آغاز نمی‌شود");
    return;
   }
   if(job.optLong("scheduled_start_at")>0){job.put("scheduled_start_at",0);JobStore.save(this,job);}
   try(SentHistoryStore log=new SentHistoryStore(this)){log.reconcile(job);}
   // Explicit user resume starts a new bounded retry cycle, keeping the cooldown.
   if(job.optLong("retry_count",0)>=RetryController.MAX_ATTEMPTS)job.put("retry_count",0);
   int intervalSeconds=SendInterval.fromJob(job);
   long intervalMs=intervalSeconds*1000L;
   long nextAllowed=SystemClock.elapsedRealtime()+SendInterval.remainingMs(job.optLong("last_attempt"),System.currentTimeMillis(),intervalSeconds);
   while(!cancel){
    long retryAt=job.optLong("next_retry_time",0);
    if(retryAt>System.currentTimeMillis()){
     long remain=retryAt-System.currentTimeMillis();
     checkpoint(job,"در انتظار retry: "+((remain+999)/1000)+" ثانیه");
     Thread.sleep(Math.min(remain,30000));
     continue;
    }
    if(retryAt>0){
     JSONArray recipients=job.getJSONArray("recipients");
     for(int i=0;i<recipients.length();i++){
      JSONObject row=recipients.getJSONObject(i);
      if("retry_wait".equals(row.optString("status")))row.put("status","pending");
     }
     job.put("next_retry_time",0);JobStore.save(this,job);
    }
    int n=JobStore.next(job);if(n<0){checkpoint(job,"صف تمام شد");break;}
    long wait=nextAllowed-SystemClock.elapsedRealtime();
    if(wait>0){checkpoint(job,"انتظار برای نوبت بعد: "+((wait+999)/1000)+" ثانیه");Thread.sleep(Math.min(wait,30000));continue;}
    current=job.getJSONArray("recipients").getJSONObject(n);dispatch=false;
    job.put("last_attempt",System.currentTimeMillis());checkpoint(job,"بررسی شماره "+(n+1)+" از "+job.getJSONArray("recipients").length());
    String guid=current.optString("kind").equals("group")?client.groupTarget(current.getString("guid")):client.resolve(current.getString("phone"),current.optString("name"));
    if(cancel)break;
    if(guid.isEmpty()){current.put("status","skipped");current.put("error","حساب روبیکا برای این شماره پیدا نشد");job.put("last_attempt",System.currentTimeMillis());nextAllowed=SystemClock.elapsedRealtime()+intervalMs;checkpoint(job,"شماره یافت نشد؛ نوبت بعد پس از "+intervalSeconds+" ثانیه");current=null;continue;}
    current.put("guid",guid);current.put("status","sending");checkpoint(job,"در حال ارسال مورد "+(n+1));
    if(cancel){current.put("status","pending");JobStore.save(this,job);break;}
    dispatch=true;
    String messageId;
    try {
     messageId=client.send(guid,job.getString("message"),current.getLong("rnd"));
     job.put("retry_count",0);
    } catch(RubikaClient.ApiError api){
     String c=api.code;
     if(c.contains("TOO_REQUESTS")||c.contains("RATE")||c.contains("FLOOD")){
      long retry=job.optLong("retry_count",0)+1;
      long delay=RetryController.backoff((int)Math.min(retry,Integer.MAX_VALUE));
      nextAllowed=SystemClock.elapsedRealtime()+intervalMs;
      current.put("status","retry_wait");
      job.put("retry_count",retry);
      job.put("next_retry_time",System.currentTimeMillis()+delay);
      dispatch=false;
      if(!RetryController.canRetry((int)Math.min(retry,Integer.MAX_VALUE))){
       checkpoint(job,"صف پس از ۸ محدودیت متوالی متوقف شد؛ پس از زمان انتظار با دکمه ادامه از سر بگیرید");
       break;
      }
      checkpoint(job,"محدودیت روبیکا؛ retry بعد از "+((delay+999)/1000)+" ثانیه");
      Thread.sleep(Math.min(delay,30000));
      continue;
     }
     throw api;
    }
    JobStore.recordSuccess(current,messageId,System.currentTimeMillis());
    job.put("last_attempt",System.currentTimeMillis());nextAllowed=SystemClock.elapsedRealtime()+intervalMs;checkpoint(job,"مورد "+(n+1)+(messageId.isEmpty()?" توسط سرور پذیرفته شد؛ شناسه پیام در پاسخ موجود نبود":" با شناسه پیام ثبت شد"));dispatch=false;
    // Archive only after durable job checkpoint; reconcile() recovers after a crash.
    try(SentHistoryStore history=new SentHistoryStore(this)){history.archive(job,current);}
    current=null;
   }
   if(cancel&&job!=null)checkpoint(job,"با درخواست شما متوقف شد؛ ادامه از موارد باقی‌مانده");
  }catch(InterruptedException e){try{if(job!=null)checkpoint(job,"صف متوقف شد؛ ادامه از موارد باقی‌مانده");}catch(Exception ignored){}}
  catch(Exception e){
   try{if(job!=null){if(current!=null){if(dispatch){current.put("status","uncertain");current.put("error","ممکن است پیام ارسال شده باشد؛ "+MainActivity.error(e));}else if(current.optString("status").equals("sending"))current.put("status","pending");}
    String detail=MainActivity.error(e);checkpoint(job,(dispatch?"نتیجه ارسال نامشخص؛ ":"توقف؛ ")+detail);}}
   catch(Exception ignored){}
  }finally{running=false;OperationGate.release("queue");if(wake!=null&&wake.isHeld())wake.release();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
 }
 @Override public void onDestroy(){cancel=true;if(worker!=null)worker.interrupt();if(wake!=null&&wake.isHeld())wake.release();super.onDestroy();}
}
