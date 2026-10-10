package ir.sinicable.rubikaqueue;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import org.json.*;
import java.io.*;
public class DiscoveryService extends Service {
 public static volatile boolean running=false;private volatile boolean cancel=false;private Thread worker;private PowerManager.WakeLock wake;
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent i,int flags,int id){
  if(i!=null&&"STOP".equals(i.getAction())){cancel=true;if(worker!=null)worker.interrupt();else stopSelf();return START_NOT_STICKY;}
  if(running)return START_NOT_STICKY;if(!OperationGate.acquire("discovery")){stopSelf();return START_NOT_STICKY;}
  running=true;cancel=false;
  try{getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("discovery","جست‌وجوی گروه و تماس کاری",NotificationManager.IMPORTANCE_LOW));startForeground(72,notice("جست‌وجو شروع شد؛ هیچ پیامی ارسال نمی‌شود"));wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"RubikaQueue:Discovery");wake.acquire(60*60*1000L);worker=new Thread(this::work,"discovery");worker.start();}catch(Exception e){running=false;OperationGate.release("discovery");stopSelf();}
  return START_NOT_STICKY;
 }
 Notification notice(String text){PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);PendingIntent stop=PendingIntent.getService(this,2,new Intent(this,DiscoveryService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);return new Notification.Builder(this,"discovery").setContentTitle("جست‌وجوی گروه‌ها و شماره تماس‌ها").setContentText(text).setSmallIcon(R.drawable.icon).setOngoing(true).setContentIntent(open).addAction(new Notification.Action.Builder(null,"توقف جست‌وجو",stop).build()).build();}
 void persist(JSONObject s,String note)throws Exception{s.put("note",note);DiscoveryStore.save(this,s);getSystemService(NotificationManager.class).notify(72,notice(note));}
 void work(){JSONObject data=null;
  try{RubikaClient client=Vault.load(this);if(client==null)throw new IOException("ابتدا وارد حساب شوید");data=DiscoveryStore.load(this);if(!data.optString("owner",client.guid).equals(client.guid))data=DiscoveryData.empty();data.put("owner",client.guid);
   SharedPreferences p=getSharedPreferences("discovery",0);String[] terms=LeadExtractor.terms(p.getString("terms",LeadExtractor.DEFAULT_TERMS));if(terms.length==0)throw new IOException("یک عبارت جست‌وجو وارد کنید");
   DiscoveryEngine engine=new DiscoveryEngine((method,input)->client.call(method,input,false),new DiscoveryEngine.PublicPages(),1800);
   engine.run(data,terms,p.getBoolean("groups",true),p.getBoolean("contacts",true),new DiscoveryEngine.Progress(){public boolean stopped(){return cancel;}public void update(JSONObject s,String note)throws Exception{persist(s,note);}});
  }catch(InterruptedException e){if(data!=null)try{data.put("note","جست‌وجو متوقف شد؛ نتایج پیدا شده نگه داشته شدند");}catch(Exception ignored){}}
  catch(Exception e){if(data!=null)try{data.put("note","توقف جست‌وجو: "+MainActivity.error(e));DiscoveryData.log(data,"جست‌وجو",MainActivity.error(e));}catch(Exception ignored){}}
  finally{
   try{if(data!=null){String uri=getSharedPreferences("discovery",0).getString("output","");
     // Generate a complete local workbook before touching the chosen external document.
     File temp=new File(getFilesDir(),"discovery-results.xlsx");try(OutputStream out=new FileOutputStream(temp)){ResearchXlsx.write(data,out);}
     if(!uri.isEmpty()){try(InputStream in=new FileInputStream(temp);OutputStream out=getContentResolver().openOutputStream(Uri.parse(uri),"wt")){if(out==null)throw new IOException("فایل مقصد باز نشد");byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}data.put("note",data.optString("note")+"\nفایل Excel به‌روز شد");}
     DiscoveryStore.save(this,data);
   }}catch(Exception e){try{if(data!=null){data.put("note",data.optString("note")+"\nذخیرهٔ Excel ناموفق بود؛ نتایج داخل برنامه محفوظ‌اند. از «ذخیره Excel» استفاده کنید.");DiscoveryStore.save(this,data);}}catch(Exception ignored){}}
   running=false;OperationGate.release("discovery");if(wake!=null&&wake.isHeld())wake.release();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
  }
 }
 @Override public void onDestroy(){cancel=true;if(worker!=null)worker.interrupt();if(wake!=null&&wake.isHeld())wake.release();super.onDestroy();}
}
