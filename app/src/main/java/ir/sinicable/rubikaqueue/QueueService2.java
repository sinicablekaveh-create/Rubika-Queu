package ir.sinicable.rubikaqueue;
import android.app.*;
import android.content.*;
import android.os.*;
import org.json.*;

public class QueueService2 extends Service {
 public static volatile boolean running=false;
 public static volatile boolean waitingForAuth=false;
 private volatile boolean cancel=false;
 private Thread worker;
 private PowerManager.WakeLock wake;
 private static final int NOTICE=172;

 static class FatalQueueException extends Exception {
  FatalQueueException(String message){super(message);}
 }

 @Override public IBinder onBind(Intent i){return null;}

 @Override public int onStartCommand(Intent i,int flags,int id){
  if(i!=null&&("STOP".equals(i.getAction())||"PAUSE".equals(i.getAction()))){
   getSharedPreferences("queue_recovery_2",0).edit().putBoolean("resume",false).putBoolean("paused","PAUSE".equals(i.getAction())).apply();
   cancel=true;
   if(worker!=null)worker.interrupt();else stopSelf();
   return START_NOT_STICKY;
  }
  if(running)return START_STICKY;
  if(!OperationGate.acquire("queue2")){
   persistQueueStartError("صف شروع نشد؛ عملیات دیگری فعال است");
   stopSelf();
   return START_STICKY;
  }
  getSharedPreferences("queue_recovery_2",0).edit().putBoolean("resume",true).apply();
  running=true;cancel=false;
  try{
   NotificationChannel channel=new NotificationChannel("queue_2","صف ارسال روبیکا",NotificationManager.IMPORTANCE_LOW);
   getSystemService(NotificationManager.class).createNotificationChannel(channel);
   startForeground(NOTICE,notification("در حال آماده‌سازی صف"));
   wake=((PowerManager)getSystemService(POWER_SERVICE))
       .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"RubikaQueue:Sending2");
   wake.setReferenceCounted(false);
   worker=new Thread(this::superviseQueue,"message-queue-supervisor");
   worker.start();
  }catch(Exception e){
   waitingForAuth=false;
   running=false;
   OperationGate.release("queue2");
   persistQueueStartError("خطا در راه‌اندازی صف: "+MainActivity.error(e));
   if(wake!=null&&wake.isHeld())wake.release();
   stopSelf();
  }
  return START_STICKY;
 }

 void persistQueueStartError(String reason){
  try{
   JSONObject job=JobStore.load(this,2);
   if(job!=null){job.put("note",reason);JobStore.save(this,2,job);}
  }catch(Exception ignored){}
 }

 Notification notification(String text){
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MultiAccountActivity.class),
      PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,QueueService2.class).setAction("STOP"),
      PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  return new Notification.Builder(this,"queue_2")
      .setContentTitle("صف پیام روبیکا • حساب 2")
      .setContentText(text)
      .setStyle(new Notification.BigTextStyle().bigText(text))
      .setSmallIcon(R.drawable.icon).setOngoing(true)
      .setContentIntent(open)
      .addAction(new Notification.Action.Builder(null,"توقف دستی",stop).build())
      .build();
 }

 void notice(String s){
  try{getSystemService(NotificationManager.class).notify(NOTICE,notification(s));}
  catch(Exception ignored){}
 }

 void checkpoint(JSONObject j,String text)throws Exception{
  j.put("note",text);
  JobStore.save(this,2,j);
  notice(text);
 }

 static String code(RubikaClient.ApiError api){
  return api.code==null?"":api.code.trim().toUpperCase(java.util.Locale.ROOT)
      .replace(' ','_').replace('-','_');
 }

 static boolean rate(String code){
  return code.contains("TOO_REQUESTS")||code.contains("RATE")||code.contains("FLOOD");
 }

 private void superviseQueue(){
  int supervisorRetry=0;
  try{
   while(!cancel){
    JSONObject job=null;
    try{
     job=JobStore.load(this,2);
     if(job==null)throw new FatalQueueException("صف ذخیره‌شده وجود ندارد");
     if(!job.optBoolean("consented",false))throw new FatalQueueException("رضایت دریافت پیام برای این صف تأیید نشده؛ در صفحه حساب تأیید کنید");
     if(!job.optString("media_type","").isEmpty())throw new FatalQueueException("انتقال تصویر شخصی تأیید نشده؛ صف تصویری ارسال نشد");
     RubikaClient client=null;
     try{client=Vault.load(this,2);}catch(Exception vaultProblem){
      waitingForAuth=true;
      checkpoint(job,"نشست ذخیره‌شده خوانده نشد؛ صف محفوظ است و منتظر ورود دوباره می‌ماند");
      try{sleepInterruptibly(30000L);}catch(InterruptedException stop){Thread.currentThread().interrupt();break;}
      continue;
     }
     if(client==null||client.privateKey==null||!client.guid.equals(job.optString("owner"))){
      waitingForAuth=true;
      checkpoint(job,client==null||client.privateKey==null
          ?"صف متوقف نشده؛ منتظر ورود مجدد حساب است"
          :"صف متعلق به حساب دیگری است؛ منتظر ورود حساب درست است");
      try{sleepInterruptibly(30000L);}catch(InterruptedException stop){Thread.currentThread().interrupt();break;}
      continue;
     }
     waitingForAuth=false;
     if(!NetworkState.online(this)){
      checkpoint(job,"اینترنت معتبر در دسترس نیست؛ صف محفوظ است و بدون مصرف retry منتظر اتصال می‌ماند");
      try{sleepInterruptibly(15000L);}catch(InterruptedException stop){Thread.currentThread().interrupt();break;}
      continue;
     }

     if(job.optLong("scheduled_start_at",0)>System.currentTimeMillis()){
      if(!ScheduleManager.schedule(this,job,2))
       checkpoint(job,"زمان‌بندی ذخیره شده ولی مجوز زمان‌بندی دقیق فعال نیست");
      else checkpoint(job,"صف زمان‌بندی شد؛ در زمان انتخاب‌شده ادامه پیدا می‌کند");
      break;
     }
     if(job.optLong("scheduled_start_at")>0){
      job.put("scheduled_start_at",0);JobStore.save(this,2,job);
     }

     // A killed process can leave a row persisted as 'sending'. The server may
     // have accepted it. Never dispatch it again automatically after restart.
     JSONArray recovered=job.getJSONArray("recipients");
     boolean recoveredInFlight=false;
     for(int i=0;i<recovered.length();i++){
      JSONObject row=recovered.getJSONObject(i);
      if("sending".equals(row.optString("status"))){
       row.put("status","uncertain");
       row.put("error","برنامه هنگام ارسال بسته شد؛ نتیجه نامشخص است و ارسال خودکار تکرار نمی‌شود");
       recoveredInFlight=true;
      }
     }
     if(recoveredInFlight)checkpoint(job,"ارسال‌های نیمه‌تمام برای بررسی ثبت شدند؛ ادامه فقط برای گیرندگان دیگر");
     try(SentHistoryStore history=new SentHistoryStore(this)){history.reconcile(job);}
     if(JobStore.uncertain(job))
      checkpoint(job,"موارد با نتیجه نامشخص تکرار نمی‌شوند؛ سایر گیرندگان ادامه پیدا می‌کنند");

     // Crash recovery: a saved retry_wait without a future retry deadline
     // must become pending rather than falsely finishing the whole queue.
     if(job.optLong("next_retry_time",0)<=System.currentTimeMillis()){
      boolean restored=false;
      JSONArray rows=job.getJSONArray("recipients");
      for(int i=0;i<rows.length();i++){
       JSONObject row=rows.getJSONObject(i);
       if("retry_wait".equals(row.optString("status"))){
        row.put("status","pending");restored=true;
       }
      }
      if(restored){job.put("next_retry_time",0);JobStore.save(this,2,job);}
     }
     processPending(job,client);
     supervisorRetry=0;

     if(cancel)break;
     if(!JobStore.hasWork(job)){
      getSharedPreferences("queue_recovery_2",0).edit().putBoolean("resume",false).apply();
      checkpoint(job,JobStore.uncertain(job)
          ?"همه گیرندگان قابل‌پردازش تمام شدند؛ موارد نامشخص فقط نیازمند بررسی گزارش هستند"
          :"صف تمام شد");
      break;
     }
    }catch(InterruptedException stopped){
     Thread.currentThread().interrupt();
     if(job!=null)try{checkpoint(job,cancel?"صف با درخواست شما متوقف شد":"اجرای صف قطع شد؛ وضعیت برای بازیابی ذخیره است");}catch(Exception ignored){}
     break;
    }catch(FatalQueueException fatal){
     getSharedPreferences("queue_recovery_2",0).edit().putBoolean("resume",false).apply();
     if(job!=null)try{checkpoint(job,"نیاز به اقدام کاربر: "+fatal.getMessage());}catch(Exception ignored){}
     else persistQueueStartError("نیاز به اقدام کاربر: "+fatal.getMessage());
     break;
    }catch(RubikaClient.ApiError api){
     String c=code(api);
     if("INVALID_AUTH".equals(c)){
      waitingForAuth=true;
      if(job!=null)try{checkpoint(job,"اعتبار ورود رد شد؛ صف محفوظ است و منتظر ورود مجدد می‌ماند");}catch(Exception ignored){}
      try{sleepInterruptibly(30000L);}catch(InterruptedException stop){Thread.currentThread().interrupt();break;}
      continue;
     }
     supervisorRetry++;
     long delay=RetryBackoff.delayMs(supervisorRetry,(long)(Math.random()*10000));
     if(job!=null)try{checkpoint(job,"خطای موقت سراسری "+c+"؛ تلاش بازیابی پس از "+((delay+999)/1000)+" ثانیه");}catch(Exception ignored){}
     try{sleepInterruptibly(delay);}catch(InterruptedException stop){Thread.currentThread().interrupt();break;}
    }catch(Exception unexpected){
     supervisorRetry++;
     long delay=RetryBackoff.delayMs(supervisorRetry,(long)(Math.random()*10000));
     if(job!=null)try{checkpoint(job,"خطای داخلی/شبکه؛ صف محفوظ است و پس از "+((delay+999)/1000)+" ثانیه دوباره تلاش می‌کند: "+MainActivity.error(unexpected));}catch(Exception ignored){}
     try{sleepInterruptibly(delay);}catch(InterruptedException stop){Thread.currentThread().interrupt();break;}
    }
   }
  }finally{
   waitingForAuth=false;
   running=false;
   OperationGate.release("queue2");
   if(wake!=null&&wake.isHeld())wake.release();
   stopForeground(STOP_FOREGROUND_REMOVE);
   stopSelf();
  }
 }

 private void processPending(JSONObject job,RubikaClient client)throws Exception{
  final int intervalSeconds=SendInterval.fromJob(job);
  final long intervalMs=intervalSeconds*1000L;
  long nextAllowed=SystemClock.elapsedRealtime()+
      SendInterval.remainingMs(job.optLong("last_attempt"),System.currentTimeMillis(),intervalSeconds);

  while(!cancel){
   if(!NetworkState.online(this)){
    checkpoint(job,"ارتباط اینترنت قطع شد؛ تلاش‌های صف محفوظ‌اند و پس از اتصال ادامه می‌دهند");
    Thread.sleep(15000L);
    continue;
   }
   // The user may reauthenticate while the queue is active. Reload the session
   // before each recipient; never keep using a revoked in-memory credential.
   RubikaClient refreshed=null;
   try{refreshed=Vault.load(this,2);}catch(Exception vaultProblem){
    waitingForAuth=true;
    checkpoint(job,"نشست ورود هنگام ادامه صف قابل خواندن نبود؛ صف محفوظ است و منتظر ورود مجدد می‌ماند");
    return;
   }
   if(refreshed==null||refreshed.privateKey==null||!refreshed.guid.equals(job.optString("owner"))){
    waitingForAuth=true;
    checkpoint(job,"حساب از دسترس خارج شد؛ صف محفوظ است و منتظر ورود مجدد می‌ماند");
    return;
   }
   client=refreshed;
   long retryAt=job.optLong("next_retry_time",0);
   if(retryAt>System.currentTimeMillis()){
    long remain=retryAt-System.currentTimeMillis();
    checkpoint(job,"خطای موقت؛ تلاش بعدی در "+((remain+999)/1000)+" ثانیه");
    Thread.sleep(Math.min(remain,30000));
    continue;
   }
   if(retryAt>0){
    JSONArray all=job.getJSONArray("recipients");
    for(int i=0;i<all.length();i++){
     JSONObject row=all.getJSONObject(i);
     if("retry_wait".equals(row.optString("status")))row.put("status","pending");
    }
    job.put("next_retry_time",0);
    JobStore.save(this,2,job);
   }

   int n=JobStore.next(job);
   if(n<0)return;

   long wait=nextAllowed-SystemClock.elapsedRealtime();
   if(wait>0){
    checkpoint(job,"انتظار برای نوبت بعد: "+((wait+999)/1000)+" ثانیه");
    Thread.sleep(Math.min(wait,30000));
    continue;
   }

   JSONObject current=job.getJSONArray("recipients").getJSONObject(n);
   boolean dispatch=false;
   try{
    if("group".equals(current.optString("kind"))){
     current.put("status","skipped");
     current.put("error","صف گروهی غیرفعال است");
     checkpoint(job,"مورد "+(n+1)+" گروهی بود و کنار گذاشته شد؛ ادامه صف");
     continue;
    }

    String destinationKey=QueueDuplicateGuard.destinationKey(
        current.optString("kind"),current.optString("phone"),current.optString("guid"));
    if(!destinationKey.isEmpty()){
     boolean duplicate=false;
     JSONArray rows=job.getJSONArray("recipients");
     for(int i=0;i<n;i++){
      JSONObject old=rows.getJSONObject(i);
      if(QueueDuplicateGuard.blocksRepeat(old.optString("status"))&&
          destinationKey.equals(QueueDuplicateGuard.destinationKey(
              old.optString("kind"),old.optString("phone"),old.optString("guid")))){
       duplicate=true;break;
      }
     }
     if(duplicate){
      current.put("status","skipped");
      current.put("error","مقصد تکراری در همین صف؛ ارسال دوم انجام نشد");
      checkpoint(job,"مورد "+(n+1)+" تکراری بود؛ ادامه صف");
      continue;
     }
    }

    job.put("last_attempt",System.currentTimeMillis());
    checkpoint(job,"بررسی شماره "+(n+1)+" از "+job.getJSONArray("recipients").length());

    acquireCpuForNetwork();
    String guid=client.resolve(current.getString("phone"),current.optString("name"));
    if(cancel)throw new InterruptedException();

    if(guid.isEmpty()){
     current.put("status","skipped");
     current.put("error","حساب روبیکا برای این شماره پیدا نشد");
     nextAllowed=SystemClock.elapsedRealtime()+intervalMs;
     checkpoint(job,"شماره "+(n+1)+" در روبیکا پیدا نشد؛ ادامه صف");
     continue;
    }

    current.put("guid",guid);
    long queuedRnd=current.optLong("rnd",0);
    if(!RubikaClient.validMessageRnd(queuedRnd)){
     queuedRnd=RubikaClient.freshMessageRnd();
     current.put("rnd",queuedRnd);
     checkpoint(job,"شناسه داخلی مورد "+(n+1)+" با قالب فعلی روبیکا هماهنگ شد");
    }
    current.put("status","sending");
    checkpoint(job,"در حال ارسال مورد "+(n+1));
    if(cancel){
     current.put("status","pending");JobStore.save(this,2,job);
     throw new InterruptedException();
    }

    dispatch=true;
    String messageId=client.send(guid,job.getString("message"),current.getLong("rnd"));
    dispatch=false;
    job.put("retry_count",0);
    JobStore.recordSuccess(current,messageId,System.currentTimeMillis());
    job.put("last_attempt",System.currentTimeMillis());
    nextAllowed=SystemClock.elapsedRealtime()+intervalMs;
    checkpoint(job,"مورد "+(n+1)+(messageId.isEmpty()
        ?" توسط سرور پذیرفته شد؛ ادامه صف"
        :" با شناسه پیام ثبت شد؛ ادامه صف"));
    try(SentHistoryStore history=new SentHistoryStore(this)){history.archive(job,current);}
    catch(Exception ignored){}

   }catch(InterruptedException stop){
    throw stop;
   }catch(RubikaClient.ApiError api){
    String c=code(api);
    if("INVALID_AUTH".equals(c)){
     // A server rejection is definitive; the request did not get accepted.
     dispatch=false;
     if("sending".equals(current.optString("status")))current.put("status","pending");
     current.put("error","اعتبار ورود رد شد؛ پس از ورود مجدد همین مورد دوباره بررسی می‌شود");
     JobStore.save(this,2,job);
     throw api;
    }

    if(rate(c)){
     // Rate limits are server instructions. Pause globally rather than
     // skipping recipients or retrying rapidly to bypass the limit.
     long retry=Math.max(1,Math.min(8,job.optLong("retry_count",0)+1));
     long localDelay=RetryBackoff.delayMs(retry,(long)(Math.random()*10000));
     long serverDelay=api instanceof RubikaClient.RateLimitError?((RubikaClient.RateLimitError)api).retryAfterMs:0L;
     long delay=Math.max(localDelay,serverDelay);
     current.put("status","retry_wait");
     current.put("error",MainActivity.error(api));
     job.put("retry_count",retry);
     job.put("next_retry_time",System.currentTimeMillis()+delay);
     dispatch=false;
     checkpoint(job,"محدودیت موقت روبیکا؛ صف متوقف نشده و پس از "+((delay+999)/1000)+" ثانیه ادامه می‌دهد"+(serverDelay>0?" (طبق Retry-After سرور)":""));
     continue;
    }

    // INVALID_INPUT and other explicit server rejections are final for this row.
    // They are not ambiguous deliveries, so skip the row and continue safely.
    dispatch=false;
    current.put("status","skipped");
    current.put("error",MainActivity.error(api));
    current.put("server_code",c);
    job.put("retry_count",0);
    job.put("last_attempt",System.currentTimeMillis());
    nextAllowed=SystemClock.elapsedRealtime()+intervalMs;
    checkpoint(job,"مورد "+(n+1)+" توسط سرور رد شد ("+c+")؛ صف به مورد بعدی ادامه می‌دهد");
    continue;

   }catch(Exception problem){
    String description=MainActivity.error(problem);

    if(dispatch){
     // A transport failure after dispatch may have delivered the message.
     // Never repeat this row automatically, but keep processing the others.
     current.put("status","uncertain");
     current.put("error","نتیجه ارسال نامشخص؛ "+description);
     dispatch=false;
     job.put("last_attempt",System.currentTimeMillis());
     nextAllowed=SystemClock.elapsedRealtime()+intervalMs;
     checkpoint(job,"نتیجه مورد "+(n+1)+" نامشخص است؛ تکرار نشد و صف ادامه دارد");
     continue;
    }

    boolean permanent=problem instanceof IllegalArgumentException||
        description.contains("تطبیق شماره")||
        description.contains("حساب روبیکا برای این شماره")||
        description.contains("صف گروهی");

    if(permanent){
     current.put("status","skipped");
     current.put("error",description);
     job.put("last_attempt",System.currentTimeMillis());
     nextAllowed=SystemClock.elapsedRealtime()+intervalMs;
     checkpoint(job,"مورد "+(n+1)+" قابل ارسال نبود؛ کنار گذاشته شد و صف ادامه دارد");
     continue;
    }

    // Before dispatch, automatic retry is safe because no message request was made.
    int rowRetry=current.optInt("row_retry_count",0)+1;
    current.put("row_retry_count",rowRetry);
    current.put("error",description);
    if(rowRetry>=5){
     current.put("status","skipped");
     current.put("error","پس از ۵ تلاش ناموفق پیش از ارسال کنار گذاشته شد؛ "+description);
     current.put("row_retry_count",0);
     job.put("next_retry_time",0);
     nextAllowed=SystemClock.elapsedRealtime()+intervalMs;
     checkpoint(job,"مورد "+(n+1)+" پس از ۵ خطای موقت کنار گذاشته شد؛ صف به مورد بعدی ادامه می‌دهد");
     continue;
    }
    long delay=RetryBackoff.delayMs(rowRetry,(long)(Math.random()*10000));
    current.put("status","retry_wait");
    job.put("next_retry_time",System.currentTimeMillis()+delay);
    checkpoint(job,"خطای موقت پیش از ارسال مورد "+(n+1)+"؛ تلاش "+rowRetry+" از ۵ پس از "+((delay+999)/1000)+" ثانیه");
   }finally{
    releaseCpu();
   }
  }
 }

 private void acquireCpuForNetwork(){
  try{
   if(wake!=null&&!wake.isHeld())wake.acquire(120000L);
  }catch(Exception ignored){}
 }

 private void releaseCpu(){
  try{
   if(wake!=null&&wake.isHeld())wake.release();
  }catch(Exception ignored){}
 }

 private void sleepInterruptibly(long delay)throws InterruptedException{
  long end=SystemClock.elapsedRealtime()+Math.max(1000L,delay);
  while(!cancel){
   long left=end-SystemClock.elapsedRealtime();
   if(left<=0)return;
   Thread.sleep(Math.min(left,30000L));
  }
  throw new InterruptedException();
 }

 @Override public void onTaskRemoved(Intent rootIntent){
  persistQueueStartError("برنامه از فهرست اخیر کنار رفت؛ صف ذخیره است و سرویس در صورت اجازه اندروید ادامه/بازیابی می‌شود");
  super.onTaskRemoved(rootIntent);
 }

 @Override public void onTimeout(int startId,int fgsType){
  persistQueueStartError("اندروید مهلت Foreground Service را تمام کرد؛ صف ذخیره شد");
  getSharedPreferences("queue_recovery_2",0).edit().putBoolean("resume",true).apply();
  ScheduledQueueReceiver.notifyRecovery(this,2,"صف به محدودیت اجرای پس‌زمینه رسید؛ برای ادامه «ادامه صف» را بزنید.");
  cancel=true;
  if(worker!=null)worker.interrupt();
  stopSelf();
 }

 @Override public void onDestroy(){
  cancel=true;
  if(worker!=null)worker.interrupt();
  if(wake!=null&&wake.isHeld())wake.release();
  super.onDestroy();
 }
}
