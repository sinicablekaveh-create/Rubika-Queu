package ir.sinicable.rubikaqueue;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.util.concurrent.*;

/** Three independent personal-account queues. Slot 1 reuses the legacy account/job. */
public class MultiAccountActivity extends Activity {
 static final int PICK_BASE=210;
 static final int PICK_PHOTO_BASE=310;
 static final int NOTIFY_REQ=41;
 static class Panel {
  int slot; RubikaClient client; String codeHash=""; ExcelReader.Result imported; boolean busy=false;
  EditText phone,password,code,message,interval;
  Uri selectedPhoto; ImageView photoPreview; TextView photoStatus; CheckBox consent;
  TextView accountStatus,fileStatus,queueStatus;
  Button sendCode,login,start,stop,logout;
 }
 final Panel[] p=new Panel[4];
 final ExecutorService io=Executors.newFixedThreadPool(3);
 final Handler handler=new Handler(Looper.getMainLooper());
 LinearLayout root;
 final Runnable refresh=new Runnable(){public void run(){refreshAll();handler.postDelayed(this,1500);}};

 interface Work{String run()throws Exception;}

 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  getWindow().setStatusBarColor(Color.rgb(243,246,245));
  build();
  for(int slot=1;slot<=3;slot++)loadSlot(slot);
  handler.post(refresh);
  handler.postDelayed(this::ensureNotificationVisibility,700);
  for(int slot=1;slot<=3;slot++)restorePhoto(slot);
 }
 @Override protected void onResume(){
  super.onResume();
  ScheduleManager.rearm(this);
  handler.postDelayed(this::resumeSavedQueues,400);
 }
 @Override protected void onDestroy(){handler.removeCallbacks(refresh);super.onDestroy();}

 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 TextView label(LinearLayout parent,String text,int size,boolean bold){
  TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(Color.rgb(27,48,47));
  if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(0,dp(5),0,dp(6));parent.addView(t);return t;
 }
 EditText input(LinearLayout parent,String hint,int type){
  EditText e=new EditText(this);e.setHint(hint);e.setInputType(type);e.setTextSize(15);
  e.setPadding(dp(7),dp(6),dp(7),dp(6));parent.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;
 }
 Button button(LinearLayout parent,String text,Runnable action){
  Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setOnClickListener(v->action.run());
  parent.addView(b,new LinearLayout.LayoutParams(-1,-2));return b;
 }
 LinearLayout card(String title){
  LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(12),dp(14),dp(14));
  GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(16));c.setBackground(bg);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(14);root.addView(c,lp);
  label(c,title,19,true);return c;
 }

 void build(){
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(243,246,245));
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  root.setPadding(dp(16),dp(14),dp(16),dp(26));scroll.addView(root);setContentView(scroll);

  label(root,"۳ حساب روبیکا • ۳ صف همزمان",27,true);
  label(root,"هر حساب نشست، Excel، متن، فاصله، retry و صف مستقل دارد. محدودیت سرور هر حساب رعایت می‌شود.",14,false);

  LinearLayout master=card("کنترل همزمان");
  button(master,"شروع / ادامه هر سه صف ذخیره‌شده",this::startAllSaved);
  button(master,"توقف هر سه صف",this::stopAll);
  button(master,"وضعیت اعلان و باتری / تنظیمات پایداری",this::showBackgroundReliability);
  label(master,"شروع همزمان فقط صف‌هایی را اجرا می‌کند که قبلاً برای همان حساب ساخته و ذخیره شده‌اند.",13,false);

  for(int slot=1;slot<=3;slot++)buildPanel(slot);
 }

 void buildPanel(int slot){
  Panel a=new Panel();a.slot=slot;p[slot]=a;
  LinearLayout c=card("حساب "+slot+(slot==1?" • همان حساب اصلی":""));
  a.accountStatus=label(c,"در حال بررسی حساب…",14,true);
  a.phone=input(c,"شماره حساب "+slot+"؛ 09…",InputType.TYPE_CLASS_PHONE);a.phone.setTextDirection(View.TEXT_DIRECTION_LTR);
  a.password=input(c,"رمز دومرحله‌ای (در صورت فعال بودن)",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
  a.sendCode=button(c,"دریافت کد ورود حساب "+slot,()->requestCode(slot));
  a.code=input(c,"کد ورود",InputType.TYPE_CLASS_NUMBER);a.code.setTextDirection(View.TEXT_DIRECTION_LTR);
  a.login=button(c,"تأیید کد و ورود حساب "+slot,()->signIn(slot));
  a.logout=button(c,"حذف ورود محلی حساب "+slot,()->logout(slot));

  label(c,"صف مستقل حساب "+slot,17,true);
  button(c,"انتخاب Excel یا CSV حساب "+slot,()->pickExcel(slot));
  a.fileStatus=label(c,"هنوز Excel برای این حساب انتخاب نشده",13,false);
  a.message=input(c,"متن پیام حساب "+slot,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
  a.message.setMinLines(3);a.message.setGravity(Gravity.TOP|Gravity.RIGHT);
  a.message.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4200)});
  label(c,"حالت پیام: متن / تصویر / متن همراه تصویر",13,false);
  button(c,"انتخاب عکس حساب "+slot,()->pickPhoto(slot));
  a.photoStatus=label(c,"تصویری انتخاب نشده است",13,false);
  a.photoPreview=new ImageView(this);a.photoPreview.setVisibility(View.GONE);
  a.photoPreview.setAdjustViewBounds(true);a.photoPreview.setMaxHeight(dp(200));c.addView(a.photoPreview);
  button(c,"حذف تصویر انتخابی",()->clearPhoto(slot));
  button(c,"پیش‌نمایش گیرندگان",()->previewRecipients(slot));
  a.consent=new CheckBox(this);a.consent.setText("تأیید می‌کنم همه گیرندگان این صف قبلاً برای دریافت پیام رضایت داده‌اند.");
  c.addView(a.consent);
  a.interval=input(c,"فاصله ارسال؛ ۳۰ تا ۳۶۰۰ ثانیه",InputType.TYPE_CLASS_NUMBER);
  a.interval.setText("30");a.interval.setTextDirection(View.TEXT_DIRECTION_LTR);
  a.start=button(c,"ثبت و شروع / ادامه صف حساب "+slot,()->startSlot(slot));
  a.stop=button(c,"توقف صف حساب "+slot,()->stopSlot(slot));
  button(c,"مکث صف حساب "+slot,()->pauseSlot(slot));
  button(c,"ادامه صف ذخیره‌شده حساب "+slot,()->startSlot(slot));
  a.queueStatus=label(c,"صفی ذخیره نشده",14,false);
 }

 void loadSlot(int slot){
  task(slot,()->{
   Panel a=p[slot];a.client=Vault.load(this,slot);
   JSONObject job=JobStore.load(this,slot);
   if(job!=null)runOnUiThread(()->{
    if(a.message.getText().toString().trim().isEmpty())a.message.setText(job.optString("message"));
    a.interval.setText(Integer.toString(SendInterval.fromJob(job)));
   });
   return a.client==null?"حساب "+slot+" هنوز وارد نشده":"حساب "+slot+" آماده است";
  });
 }

 void task(int slot,Work work){
  Panel a=p[slot];if(a.busy){toast("عملیات حساب "+slot+" هنوز تمام نشده");return;}
  a.busy=true;refreshSlot(slot);
  io.execute(()->{
   String result;try{result=work.run();}catch(Exception e){result=MainActivity.error(e);}
   final String message=result;
   runOnUiThread(()->{a.busy=false;toast(message);refreshSlot(slot);});
  });
 }

 void requestCode(int slot){
  Panel a=p[slot];
  if(AccountSlots.running(slot)&&!AccountSlots.waitingForAuth(slot)){toast("ابتدا صف حساب "+slot+" را متوقف کنید");return;}
  String number=Phones.normalize(a.phone.getText().toString());
  if(number.isEmpty()){toast("شماره حساب "+slot+" معتبر نیست");return;}
  String pass=a.password.getText().toString();
  task(slot,()->{
   RubikaClient c=a.client;
   if(c==null||c.privateKey!=null)c=new RubikaClient();
   JSONObject r=c.sendCode(number,pass);
   String status=r.optString("status");
   if("SendPassKey".equals(status)){a.client=c;return "رمز دومرحله‌ای را وارد کنید و دریافت کد را دوباره بزنید";}
   if(!"OK".equals(status)||!r.has("phone_code_hash"))throw new RubikaClient.ApiError(RubikaClient.safeCode(status));
   a.client=c;a.codeHash=r.getString("phone_code_hash");
   return "کد ورود حساب "+slot+" ارسال شد";
  });
 }

 void signIn(int slot){
  Panel a=p[slot];String otp=Phones.digits(a.code.getText().toString()).trim();
  if(a.client==null||a.codeHash.isEmpty()||otp.isEmpty()){toast("ابتدا کد ورود حساب "+slot+" را دریافت کنید");return;}
  task(slot,()->{
   a.client.signIn(otp,a.codeHash);Vault.save(this,slot,a.client.session());
   AccountSlots.setWaitingForAuth(slot,false);
   try{a.client.register();}catch(Exception ignored){}
   a.codeHash="";
   runOnUiThread(()->{a.code.setText("");a.password.setText("");});
   return "ورود حساب "+slot+" ذخیره شد";
  });
 }

 void logout(int slot){
  if(AccountSlots.running(slot)&&!AccountSlots.waitingForAuth(slot)){toast("ابتدا صف حساب "+slot+" را متوقف کنید");return;}
  new AlertDialog.Builder(this).setTitle("حذف ورود حساب "+slot)
      .setMessage("فقط نشست ذخیره‌شده محلی حساب "+slot+" حذف می‌شود؛ صف آن حساب پاک نمی‌شود.")
      .setNegativeButton("انصراف",null)
      .setPositiveButton("حذف",(d,w)->task(slot,()->{Vault.clear(this,slot);p[slot].client=null;p[slot].codeHash="";return "ورود محلی حساب "+slot+" حذف شد";}))
      .show();
 }

 void pickExcel(int slot){
  if(AccountSlots.running(slot)){toast("ابتدا صف حساب "+slot+" را متوقف کنید");return;}
  Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");
  i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","text/csv","text/comma-separated-values","application/csv","text/plain"});
  i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_BASE+slot);
 }

 @Override protected void onActivityResult(int req,int result,Intent data){
  super.onActivityResult(req,result,data);
  if(req>=PICK_PHOTO_BASE+1&&req<=PICK_PHOTO_BASE+3){
   int photoSlot=req-PICK_PHOTO_BASE;
   if(result==RESULT_OK&&data!=null&&data.getData()!=null)acceptPhoto(photoSlot,data);
   return;
  }
  int slot=req-PICK_BASE;
  if(slot<1||slot>3||result!=RESULT_OK||data==null||data.getData()==null)return;
  Uri uri=data.getData();
  task(slot,()->{
   File f=new File(getCacheDir(),"multi_"+slot+".import");
   try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(f)){
    if(in==null)throw new IOException("فایل باز نشد");
    byte[] b=new byte[8192];int n,total=0;
    while((n=in.read(b))!=-1){total+=n;if(total>8*1024*1024)throw new IOException("حداکثر حجم فایل ۸ مگابایت است");out.write(b,0,n);}
   }
   try{
    ExcelReader.Result r=ExcelReader.read(f);p[slot].imported=r;
    runOnUiThread(()->p[slot].fileStatus.setText("شماره یکتا: "+r.entries.size()+" | تکراری: "+r.duplicates+" | نامعتبر: "+r.invalid));
    return "Excel حساب "+slot+" آماده شد";
   }finally{f.delete();}
  });
 }

 void pickPhoto(int slot){
  if(AccountSlots.running(slot)){toast("ابتدا صف حساب "+slot+" را متوقف کنید");return;}
  Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");
  i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
  startActivityForResult(i,PICK_PHOTO_BASE+slot);
 }
 void acceptPhoto(int slot,Intent data){
  Uri uri=data.getData();
  try{
   String type=getContentResolver().getType(uri);
   if(type==null||!(type.equals("image/jpeg")||type.equals("image/png")||type.equals("image/webp")))
    throw new IOException("فرمت تصویر باید JPG، PNG یا WebP باشد");
   int count=0;byte[] b=new byte[8192];
   try(InputStream in=getContentResolver().openInputStream(uri)){
    if(in==null)throw new IOException("تصویر باز نشد");int n;
    while((n=in.read(b))!=-1){count+=n;if(count>8*1024*1024)throw new IOException("اندازه تصویر بیشتر از ۸ مگابایت است");}
   }
   try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}
   catch(SecurityException ignored){}
   p[slot].selectedPhoto=uri;
   getSharedPreferences("draft_media",0).edit().putString("uri_"+slot,uri.toString()).apply();
   showPhoto(slot);
  }catch(Exception e){toast(MainActivity.error(e));}
 }
 void restorePhoto(int slot){
  String value=getSharedPreferences("draft_media",0).getString("uri_"+slot,"");
  if(value.isEmpty())return;
  try{p[slot].selectedPhoto=Uri.parse(value);showPhoto(slot);}catch(Exception e){clearPhoto(slot);}
 }
 void showPhoto(int slot){
  Panel a=p[slot];
  if(a.selectedPhoto==null){a.photoPreview.setImageDrawable(null);a.photoPreview.setVisibility(View.GONE);a.photoStatus.setText("تصویری انتخاب نشده است");return;}
  try{
   BitmapFactory.Options opt=new BitmapFactory.Options();opt.inJustDecodeBounds=true;
   try(InputStream in=getContentResolver().openInputStream(a.selectedPhoto)){BitmapFactory.decodeStream(in,null,opt);}
   if(opt.outWidth<=0||opt.outHeight<=0||opt.outWidth>16000||opt.outHeight>16000)throw new IOException("اندازه تصویر نامعتبر است");
   opt.inJustDecodeBounds=false;opt.inSampleSize=1;
   while(opt.outWidth/opt.inSampleSize>600||opt.outHeight/opt.inSampleSize>600)opt.inSampleSize*=2;
   Bitmap preview;
   try(InputStream in=getContentResolver().openInputStream(a.selectedPhoto)){preview=BitmapFactory.decodeStream(in,null,opt);}
   if(preview==null)throw new IOException("پیش‌نمایش تصویر خوانده نشد");
   a.photoPreview.setImageBitmap(preview);a.photoPreview.setVisibility(View.VISIBLE);
   a.photoStatus.setText("تصویر انتخاب شد؛ ارسال تصویر تا تأیید انتقال شخصی غیرفعال است");
  }catch(Exception e){clearPhoto(slot);toast("دسترسی به تصویر برقرار نیست؛ دوباره انتخاب کنید");}
 }
 void clearPhoto(int slot){
  Panel a=p[slot];a.selectedPhoto=null;
  getSharedPreferences("draft_media",0).edit().remove("uri_"+slot).apply();
  a.photoPreview.setImageDrawable(null);a.photoPreview.setVisibility(View.GONE);
  a.photoStatus.setText("تصویری انتخاب نشده است");
 }
 void previewRecipients(int slot){
  try{
   Panel a=p[slot];StringBuilder s=new StringBuilder();int total=0;
   if(a.imported!=null){total=a.imported.entries.size();for(int i=0;i<Math.min(total,10);i++){
    ExcelReader.Entry e=a.imported.entries.get(i);s.append(i+1).append(". +").append(e.phone).append("  ").append(e.name).append("\n");
   }}else{
    JSONObject job=JobStore.load(this,slot);if(job!=null){org.json.JSONArray rows=job.getJSONArray("recipients");total=rows.length();
     for(int i=0;i<Math.min(total,10);i++){JSONObject row=rows.getJSONObject(i);s.append(i+1).append(". +").append(row.optString("phone")).append("  ").append(row.optString("name")).append("\n");}
    }
   }
   if(total==0){toast("ابتدا Excel یا CSV را انتخاب کنید");return;}
   if(total>10)s.append("… و ").append(total-10).append(" گیرنده دیگر");
   new AlertDialog.Builder(this).setTitle("پیش‌نمایش "+total+" گیرنده حساب "+slot).setMessage(s.toString()).setPositiveButton("بستن",null).show();
  }catch(Exception e){toast(MainActivity.error(e));}
 }
 void pauseSlot(int slot){
  if(!AccountSlots.running(slot)){toast("صف حساب "+slot+" در حال اجرا نیست");return;}
  try{
   startService(new Intent(this,AccountSlots.serviceClass(slot)).setAction("PAUSE"));
   toast("مکث صف حساب "+slot+" درخواست شد؛ گیرنده نیمه‌تمام دوباره ارسال نخواهد شد");
  }catch(Exception e){toast(MainActivity.error(e));}
 }

 void startSlot(int slot){
  Panel a=p[slot];
  if(AccountSlots.running(slot)){toast("صف حساب "+slot+" همین حالا در حال اجراست");return;}
  if(OperationGate.exclusiveOccupied()){toast("یک عملیات غیرصفی در حال اجراست؛ پس از پایان دوباره بزنید");return;}
  if(a.client==null||a.client.privateKey==null){toast("ابتدا حساب "+slot+" را وارد کنید");return;}
  if(!a.consent.isChecked()){toast("ابتدا رضایت قبلی همه گیرندگان را تأیید کنید");return;}
  if(a.selectedPhoto!=null){
   new AlertDialog.Builder(this).setTitle("ارسال تصویر هنوز فعال نیست")
       .setMessage("تصویر و پیش‌نمایش آماده‌اند، اما بارگذاری و ارسال عکس از حساب شخصی در انتقال موجود تأیید نشده است. برای جلوگیری از ارسال ناقص، صف تصویری شروع نمی‌شود. تصویر را حذف کنید تا فقط متن ارسال شود.")
       .setPositiveButton("متوجه شدم",null).show();return;
  }
  try{
   int seconds=SendInterval.requireValid(a.interval.getText().toString());
   if(a.imported==null){
    JSONObject saved=JobStore.load(this,slot);
    if(saved==null||!JobStore.hasWork(saved)){toast("برای حساب "+slot+" ابتدا Excel انتخاب کنید");return;}
    if(!a.client.guid.equals(saved.optString("owner"))){toast("صف ذخیره‌شده متعلق به حساب دیگری است");return;}
    saved.put("interval_seconds",seconds);saved.put("consented",true);
    JobStore.save(this,slot,saved);launch(slot);return;
   }
   String draft=MessageQueuePolicy.requireDraft(a.message.getText().toString());
   MessageQueuePolicy.requireRecipients(a.imported.entries.size());
   ExcelReader.Result snapshot=a.imported;
   new AlertDialog.Builder(this).setTitle("شروع صف حساب "+slot)
       .setMessage("گیرندگان: "+snapshot.entries.size()+"\nفاصله: "+seconds+" ثانیه\n\nمتن:\n"+draft+
           "\n\nاین صف فقط از حساب "+slot+" ارسال می‌شود.")
       .setNegativeButton("انصراف",null)
       .setPositiveButton("شروع",(d,w)->{
        try{
         JSONObject job=JobStore.create(snapshot,draft,a.client.guid);
         job.put("interval_seconds",seconds);
         job.put("consented",true);
         ScheduleManager.cancel(this,slot);JobStore.save(this,slot,job);
         a.imported=null;a.fileStatus.setText("Excel داخل صف حساب "+slot+" ذخیره شد");
         launch(slot);
        }catch(Exception e){toast(MainActivity.error(e));}
       }).show();
  }catch(Exception e){toast(MainActivity.error(e));}
 }

 void launch(int slot){
  try{
   JSONObject job=JobStore.load(this,slot);
   if(job==null||!JobStore.hasWork(job)){toast("صف حساب "+slot+" کار باقی‌مانده ندارد");return;}
   getSharedPreferences(AccountSlots.recoveryPrefs(slot),0).edit().putBoolean("paused",false).apply();
   startForegroundService(new Intent(this,AccountSlots.serviceClass(slot)));
   toast("صف حساب "+slot+" شروع شد");
  }catch(Exception e){
   ScheduledQueueReceiver.notifyRecovery(this,slot,"صف حساب "+slot+" محفوظ است؛ برای ادامه دوباره Start را بزنید.");
   toast(MainActivity.error(e));
  }
 }

 void stopSlot(int slot){
  try{
   if(AccountSlots.running(slot)){
    startService(new Intent(this,AccountSlots.serviceClass(slot)).setAction("STOP"));
    toast("توقف صف حساب "+slot+" درخواست شد");
   }else{
    ScheduleManager.cancel(this,slot);
    getSharedPreferences(AccountSlots.recoveryPrefs(slot),0).edit().putBoolean("resume",false).putBoolean("paused",false).apply();
    toast("صف حساب "+slot+" در حال اجرا نیست");
   }
  }catch(Exception e){toast(MainActivity.error(e));}
 }

 void startAllSaved(){
  int started=0;
  for(int slot=1;slot<=3;slot++){
   try{
    if(AccountSlots.running(slot))continue;
    RubikaClient c=Vault.load(this,slot);JSONObject job=JobStore.load(this,slot);
    if(c==null||job==null||!JobStore.hasWork(job)||!c.guid.equals(job.optString("owner")))continue;
    if(!job.optBoolean("consented",false)||job.optString("media_type","").length()>0)continue;
    getSharedPreferences(AccountSlots.recoveryPrefs(slot),0).edit().putBoolean("paused",false).apply();
    startForegroundService(new Intent(this,AccountSlots.serviceClass(slot)));started++;
   }catch(Exception ignored){}
  }
  toast(started>0?started+" صف آماده همزمان شروع شد":"صف آماده‌ای برای شروع پیدا نشد");
 }

 void stopAll(){
  int stopped=0;
  for(int slot=1;slot<=3;slot++)if(AccountSlots.running(slot)){
   try{startService(new Intent(this,AccountSlots.serviceClass(slot)).setAction("STOP"));stopped++;}catch(Exception ignored){}
  }
  toast(stopped>0?"توقف "+stopped+" صف درخواست شد":"هیچ صفی در حال اجرا نیست");
 }

 void resumeSavedQueues(){
  for(int slot=1;slot<=3;slot++){
   try{
    if(AccountSlots.running(slot))continue;
    if(getSharedPreferences(AccountSlots.recoveryPrefs(slot),0).getBoolean("paused",false))continue;
    if(!getSharedPreferences(AccountSlots.recoveryPrefs(slot),0).getBoolean("resume",false))continue;
    RubikaClient c=Vault.load(this,slot);JSONObject job=JobStore.load(this,slot);
    if(c==null||job==null||!JobStore.hasWork(job))continue;
    if(!job.optBoolean("consented",false)||job.optString("media_type","").length()>0)continue;
    if(job.optLong("scheduled_start_at",0)>System.currentTimeMillis()){ScheduleManager.schedule(this,job,slot);continue;}
    startForegroundService(new Intent(this,AccountSlots.serviceClass(slot)));
   }catch(Exception e){ScheduledQueueReceiver.notifyRecovery(this,slot,"صف حساب "+slot+" محفوظ است؛ برای ادامه Start را بزنید.");}
  }
 }

 void refreshAll(){for(int slot=1;slot<=3;slot++)refreshSlot(slot);}
 void refreshSlot(int slot){
  Panel a=p[slot];if(a==null)return;
  boolean running=AccountSlots.running(slot),waiting=AccountSlots.waitingForAuth(slot);
  boolean signed=a.client!=null&&a.client.privateKey!=null;
  a.accountStatus.setText(waiting?"صف منتظر ورود مجدد حساب "+slot+" است":signed?"وارد شده: +"+a.client.phone:"وارد حساب نشده");
  boolean accountEditable=!a.busy&&(!running||waiting);
  a.phone.setEnabled(accountEditable);a.password.setEnabled(accountEditable);a.code.setEnabled(accountEditable);
  a.sendCode.setEnabled(accountEditable);a.login.setEnabled(accountEditable);a.logout.setEnabled(accountEditable&&signed);
  a.start.setEnabled(!a.busy&&!running);a.stop.setEnabled(!a.busy&&running);
  try{
   JSONObject job=JobStore.load(this,slot);
   String state=job==null?"صفی ذخیره نشده":JobStore.summary(job)+"\nفاصله: "+SendInterval.fromJob(job)+" ثانیه";
   if(running)state="● در حال اجرا\n"+state;
   else if(waiting)state="◐ منتظر ورود مجدد\n"+state;
   else if(getSharedPreferences(AccountSlots.recoveryPrefs(slot),0).getBoolean("paused",false))state="◐ مکث دستی\n"+state;
   else state="○ متوقف / آماده\n"+state;
   a.queueStatus.setText(state);
  }catch(Exception e){a.queueStatus.setText(MainActivity.error(e));}
 }


 void ensureNotificationVisibility(){
  if(Build.VERSION.SDK_INT<33)return;
  if(checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED)return;
  android.content.SharedPreferences prefs=getSharedPreferences("ui_permissions",0);
  if(prefs.getBoolean("notification_prompted",false))return;
  prefs.edit().putBoolean("notification_prompted",true).apply();
  requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},NOTIFY_REQ);
 }

 @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
  super.onRequestPermissionsResult(requestCode,permissions,grantResults);
  if(requestCode==NOTIFY_REQ){
   boolean ok=grantResults.length>0&&grantResults[0]==android.content.pm.PackageManager.PERMISSION_GRANTED;
   toast(ok?"اعلان‌های سه صف فعال شد":"صف می‌تواند اجرا شود، ولی برای دیدن وضعیت و دکمه توقف اعلان را از تنظیمات فعال کنید");
  }
 }

 boolean notificationsAllowed(){
  return Build.VERSION.SDK_INT<33||checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED;
 }

 boolean batteryExempt(){
  if(Build.VERSION.SDK_INT<23)return true;
  PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
  return pm!=null&&pm.isIgnoringBatteryOptimizations(getPackageName());
 }

 void showBackgroundReliability(){
  String text="اعلان‌ها: "+(notificationsAllowed()?"فعال":"غیرفعال")+
      "\nبهینه‌سازی باتری اندروید: "+(batteryExempt()?"استثنا / بدون محدودیت Doze":"فعال")+
      "\n\nبرای صف‌های طولانی در گوشی سامسونگ، برنامه را در Battery > Background usage limits داخل Never sleeping apps قرار دهید. "+
      "حالت Restricted یا Deep sleeping می‌تواند اجرای پس‌زمینه، Alarm و بازیابی پس از Boot را مختل کند.";
  new AlertDialog.Builder(this).setTitle("پایداری اجرای سه صف").setMessage(text)
      .setPositiveButton("تنظیمات باتری",(d,w)->openBatterySettings())
      .setNeutralButton("تنظیمات اعلان",(d,w)->openNotificationSettings())
      .setNegativeButton("بستن",null).show();
 }

 void openBatterySettings(){
  try{startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));}
  catch(Exception e){openAppDetails();}
 }
 void openNotificationSettings(){
  try{startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));}
  catch(Exception e){openAppDetails();}
 }
 void openAppDetails(){
  try{startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}
  catch(Exception e){toast("تنظیمات برنامه باز نشد");}
 }

 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
