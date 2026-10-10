package ir.sinicable.rubikaqueue;
import android.app.*;
import android.content.*;
import android.os.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import android.provider.Settings;

public class MainActivity extends Activity {
 LinearLayout root,accountPanel;EditText phone,password,code,message;TextView accountStatus,fileStatus,queueStatus;Button sendCode,login,importButton,start,test,logout,resolve,export,history;EditText intervalSeconds;TextView scheduleStatus;CheckBox scheduleEnabled;long selectedStartAt=0;
 EditText searchTerms;CheckBox searchGroups,searchContacts;Button researchStart,researchExport,researchView,groupSend;TextView discoveryStatus;boolean pendingResearch=false;
 RubikaClient client;String codeHash="";ExcelReader.Result imported;volatile boolean busy=false;
 final ExecutorService io=Executors.newSingleThreadExecutor();final Handler handler=new Handler(Looper.getMainLooper());
 final Runnable refresh=new Runnable(){public void run(){refreshStatus();handler.postDelayed(this,1500);}};
 interface Work{String run()throws Exception;}
 @Override public void onCreate(Bundle s){super.onCreate(s);getWindow().setStatusBarColor(Color.rgb(243,246,245));build();handler.post(refresh);task(()->{client=Vault.load(this);JSONObject saved=JobStore.load(this);if(saved!=null)runOnUiThread(()->intervalSeconds.setText(Integer.toString(SendInterval.fromJob(saved))));return client==null?"برای شروع وارد حساب شوید":"حساب ذخیره‌شده آماده است";});}
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 TextView label(LinearLayout p,String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(27,48,47));if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(0,dp(6),0,dp(7));p.addView(t);return t;}
 LinearLayout card(String title){LinearLayout p=new LinearLayout(this);p.setOrientation(1);p.setPadding(dp(16),dp(12),dp(16),dp(15));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(16));p.setBackground(bg);LinearLayout.LayoutParams l=new LinearLayout.LayoutParams(-1,-2);l.bottomMargin=dp(14);root.addView(p,l);label(p,title,19,true);return p;}
 EditText input(LinearLayout p,String hint,int type){EditText e=new EditText(this);e.setHint(hint);e.setInputType(type);e.setTextSize(16);e.setPadding(dp(8),dp(8),dp(8),dp(8));p.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;}
 Button button(LinearLayout p,String text,Runnable action){Button b=new Button(this);b.setText(text);b.setTextSize(15);b.setAllCaps(false);b.setMinHeight(dp(48));b.setOnClickListener(v->action.run());p.addView(b,new LinearLayout.LayoutParams(-1,-2));return b;}
 void build(){
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(243,246,245));root=new LinearLayout(this);root.setOrientation(1);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setPadding(dp(18),dp(14),dp(18),dp(28));scroll.addView(root);setContentView(scroll);
  label(root,"صف پیام روبیکا",29,true);label(root,"اکسل تا ۱۰۰۰ شماره  •  فاصلهٔ حداقل ۳۰ ثانیه",15,false);
  label(root,"نسخهٔ آزمایشی و غیررسمی ۱.۱ — ارسال صف از حساب شخصی",14,true);
  accountPanel=card("۱. حساب شخصی");
  accountStatus=label(accountPanel,"در حال بررسی…",15,false);
  phone=input(accountPanel,"شمارهٔ حساب خودتان؛ 09…",InputType.TYPE_CLASS_PHONE);phone.setTextDirection(View.TEXT_DIRECTION_LTR);
  password=input(accountPanel,"رمز دومرحله‌ای (اگر فعال است)",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
  sendCode=button(accountPanel,"دریافت کد ورود",()->requestCode());
  code=input(accountPanel,"کد ورود",InputType.TYPE_CLASS_NUMBER);code.setTextDirection(View.TEXT_DIRECTION_LTR);
  login=button(accountPanel,"تأیید کد و ورود",()->signIn());
  test=button(accountPanel,"ارسال یک پیام آزمایشی به خودم",()->testSelf());
  logout=button(accountPanel,"حذف ورود ذخیره‌شده از این برنامه",()->logout());
  button(accountPanel,"وب‌سایت رسمی و API بات روبیکا",()->startActivity(new Intent(this,OfficialBotActivity.class)));
  label(accountPanel,"کد و رمز را فقط همین‌جا وارد کنید. نشست ورود در گوشی رمزگذاری می‌شود. جست‌وجو از روبیکا و منابع عمومی برق استفاده می‌کند؛ برنامه به پیامک‌ها یا مخاطبان گوشی دسترسی ندارد.",13,false);
  buildDiscovery();
  LinearLayout p=card("۲. فایل و متن پیام");
  label(p,"فایل xlsx؛ شیت اول. عنوان ستون شماره: phone و نام اختیاری: name. شماره‌های ایرانی با 09، 989 یا +989 پذیرفته می‌شوند.",14,false);
  importButton=button(p,"انتخاب فایل اکسل",()->pick());fileStatus=label(p,"هنوز فایلی انتخاب نشده",14,false);
  message=input(p,"متن یکسان برای همهٔ شماره‌ها",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);message.setMinLines(4);message.setGravity(Gravity.TOP|Gravity.RIGHT);
  message.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4200)});
  label(p,"فاصله بین ارسال هر پیام (ثانیه)",17,true);
  intervalSeconds=input(p,"۳۰ تا ۳۶۰۰ ثانیه",InputType.TYPE_CLASS_NUMBER);
  intervalSeconds.setText("30");intervalSeconds.setTextDirection(View.TEXT_DIRECTION_LTR);
  label(p,"هر پیام پس از حداقل این فاصله از تلاش قبلی ارسال می‌شود؛ تأخیر شبکه یا محدودیت سرور می‌تواند بیشتر باشد. مقدار برای همین صف ذخیره می‌شود. این گزینه تاریخ شروع نیست.",13,false);
  label(p,"اختیاری: شروع صف در تاریخ و ساعت آینده",16,true);
  scheduleEnabled=new CheckBox(this);scheduleEnabled.setText("شروع خودکار در تاریخ و ساعت انتخابی");p.addView(scheduleEnabled);
  scheduleStatus=label(p,"شروع فوری؛ زمان‌بندی فعال نیست",14,false);
  button(p,"انتخاب تاریخ و ساعت ارسال",()->chooseSchedule());
  button(p,"پاک کردن زمان‌بندی",()->{selectedStartAt=0;scheduleEnabled.setChecked(false);scheduleStatus.setText("شروع فوری؛ زمان‌بندی فعال نیست");});
  label(p,"این تاریخ فقط زمان آغاز صف را مشخص می‌کند، نه فاصلهٔ بین پیام‌ها. زمان از ساعت گوشی (تقویم میلادی) استفاده می‌کند. برای اجرای خودکار در پس‌زمینه مجوز «هشدارها و یادآورها» لازم است؛ پس از ری‌استارت نیز برنامه زمان را بازیابی می‌کند.",13,false);
  label(p,"شناسایی گیرنده ممکن است شماره و نام را به مخاطبان حساب روبیکا اضافه کند. نام دلخواه را در اکسل بگذارید. از فهرست گیرندگانی استفاده کنید که انتظار این پیام را دارند.",13,false);
  start=button(p,"بررسی و ارسال صف از حساب شخصی",()->prepareStart());
  LinearLayout q=card("۳. وضعیت ارسال");queueStatus=label(q,"صفی ساخته نشده",15,false);
  history=button(q,"نمایش پیام‌های ارسال‌شده و سابقهٔ ارسال",()->showSentHistory(0));
  button(q,"اختیاری: شروع ادامهٔ صف در تاریخ آینده",()->scheduleSavedJob());
  button(q,"توقف ارسال / لغو زمان‌بندی",()->{
   if(QueueService.running){startService(new Intent(this,QueueService.class).setAction("STOP"));toast("توقف درخواست شد؛ درخواست در حال ارسال ممکن است کامل شود");}
   else {try{ScheduleManager.cancel(this);JSONObject job=JobStore.load(this);if(job!=null&&job.optLong("scheduled_start_at")>0){job.put("scheduled_start_at",0);job.put("note","زمان‌بندی توسط کاربر لغو شد؛ صف آمادهٔ ادامهٔ دستی است");JobStore.save(this,job);toast("زمان‌بندی لغو شد؛ پیام‌ها ارسال نشدند");refreshStatus();}else toast("صف متوقف است");}catch(Exception e){toast(error(e));}}
  });
  resolve=button(q,"بررسی / اصلاح گزارش",()->resolveUnknown());
  export=button(q,"ذخیرهٔ گزارش CSV",()->export());
  button(q,"راهنمای اجرا",()->new AlertDialog.Builder(this).setTitle("راهنما").setMessage("۱. با شماره خود وارد شوید و ارسال آزمایشی به خودتان را امتحان کنید.\n۲. ابتدا یک فایل دو یا سه‌ردیفی انتخاب کنید.\n۳. متن را بنویسید و فهرست را پیش از شروع بررسی کنید.\n۴. ارسال در اعلان قابل توقف است. اینترنت باید برقرار بماند. در تنظیمات باتری سامسونگ، در صورت توقف پس‌زمینه، برنامه را از حالت خواب خارج کنید.\n۵. ۱۰۰۰ پیام با این فاصله دست‌کم حدود ۸ ساعت و ۲۰ دقیقه زمان می‌برد؛ خطا و تأخیر شبکه زمان را بیشتر می‌کند.\n۶. برای اجرای زمان‌بندی دقیق، مجوز هشدارها و یادآورها را فعال کنید. زمان‌بندی آینده بعد از راه‌اندازی دوبارهٔ گوشی بازیابی می‌شود؛ موارد موفق تکرار نمی‌شوند.\n۷. فاصلهٔ دلخواه بین ۳۰ و ۳۶۰۰ ثانیه در همین بخش تنظیم می‌شود. محدودیت سرور قابل دور زدن نیست؛ خطاهای حساب و شبکه بررسی می‌شوند.\n۸. این برنامه رسمی روبیکا نیست و ممکن است با تغییر رابط روبیکا نیاز به اصلاح داشته باشد.\n۹. حذف ورود در این برنامه، نشست را از سرور خارج نمی‌کند؛ برای بستن کامل نشست از بخش دستگاه‌های فعال روبیکا اقدام کنید.\n۱۰. مجوز GPL-3.0؛ منطق ارتباطی برگرفته از Rubpy 7.3.5. کد منبع همراه خروجی ارائه شده است.").setPositiveButton("بستن",null).show());
 }
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 static String error(Throwable e){if(e instanceof RubikaClient.ApiError)return e.getMessage();if(e instanceof java.net.SocketTimeoutException)return "مهلت ارتباط تمام شد؛ اتصال را بررسی کنید";if(e instanceof java.net.UnknownHostException)return "سرور در دسترس نیست؛ اینترنت را بررسی کنید";if(e instanceof javax.net.ssl.SSLException)return "ارتباط امن با سرور برقرار نشد";if(e instanceof org.json.JSONException)return "ساختار پاسخ یا فایل با نسخه برنامه سازگار نیست";String s=e.getMessage();if(s!=null&&s.matches(".*[آ-ی].*"))return s;return "خطا: "+e.getClass().getSimpleName()+"؛ صف متوقف شد";}
 void task(Work work){if(busy){toast("لطفاً تا پایان عملیات صبر کنید");return;}busy=true;refreshStatus();io.execute(()->{String result;try{result=work.run();}catch(Exception e){result=error(e);}String s=result;runOnUiThread(()->{busy=false;if(!isFinishing()){toast(s);refreshStatus();}});});}
 void requestCode(){if(QueueService.running)return;String n=Phones.normalize(phone.getText().toString());String pass=password.getText().toString();if(n.isEmpty()){toast("شماره حساب معتبر نیست");return;}task(()->{if(client==null||client.privateKey!=null)client=new RubikaClient();JSONObject r=client.sendCode(n,pass);String status=r.optString("status");if(status.equals("SendPassKey"))return "رمز دومرحله‌ای را وارد کنید و دریافت کد را دوباره بزنید";if(!status.equals("OK")||!r.has("phone_code_hash"))throw new RubikaClient.ApiError(RubikaClient.safeCode(status));codeHash=r.getString("phone_code_hash");return "کد ورود ارسال شد";});}
 void signIn(){String otp=Phones.digits(code.getText().toString()).trim();if(client==null||codeHash.isEmpty()||otp.isEmpty()){toast("ابتدا کد ورود را دریافت کنید");return;}task(()->{client.signIn(otp,codeHash);Vault.save(this,client.session());String note="ورود ذخیره شد";try{client.register();}catch(Exception e){note+="؛ ثبت نام دستگاه تأیید نشد";}codeHash="";runOnUiThread(()->{code.setText("");password.setText("");});return note;});}
 void testSelf(){if(client==null||client.privateKey==null){toast("ابتدا وارد شوید");return;}new AlertDialog.Builder(this).setTitle("آزمایش حساب").setMessage("یک پیام با متن «آزمایش صف پیام روبیکا» فقط به پیام‌های ذخیره‌شدهٔ حساب خودتان فرستاده شود؟").setNegativeButton("انصراف",null).setPositiveButton("ارسال آزمایشی",(d,w)->task(()->{if(!OperationGate.acquire("test"))return "ابتدا عملیات فعلی را متوقف کنید";try{long last=getPreferences(0).getLong("test_at",0);if(System.currentTimeMillis()-last<30000)return "برای آزمایش بعدی ۳۰ ثانیه صبر کنید";getPreferences(0).edit().putLong("test_at",System.currentTimeMillis()).commit();String id=client.send(client.guid,"آزمایش صف پیام روبیکا",new java.security.SecureRandom().nextInt(1000000000));return id.isEmpty()?"سرور ارسال آزمایشی را پذیرفت؛ در روبیکا بررسی کنید":"ارسال آزمایشی با شناسه پیام ثبت شد؛ در روبیکا بررسی کنید";}finally{OperationGate.release("test");}})).show();}
 void logout(){if(QueueService.running)return;new AlertDialog.Builder(this).setTitle("حذف ورود محلی").setMessage("ورود ذخیره‌شده حذف می‌شود. برای بستن نشست روی سرور، بخش دستگاه‌های فعال روبیکا را هم بررسی کنید.").setNegativeButton("انصراف",null).setPositiveButton("حذف",(d,w)->task(()->{Vault.clear(this);client=null;codeHash="";return "ورود محلی حذف شد";})).show();}
 void pick(){if(QueueService.running||busy)return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,101);}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
  if(req==103){researchFileChosen(uri,data);return;}
  if(req==101)task(()->{File f=new File(getCacheDir(),"import.xlsx");try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(f)){if(in==null)throw new IOException("فایل باز نشد");byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))!=-1){total+=n;if(total>8*1024*1024)throw new IOException("حداکثر حجم فایل ۸ مگابایت است");out.write(b,0,n);}}try{ExcelReader.Result r=ExcelReader.read(f);imported=r;runOnUiThread(()->fileStatus.setText("شماره یکتا: "+r.entries.size()+"\nتکراری حذف‌شده: "+r.duplicates+" | نامعتبر: "+r.invalid));return "اکسل خوانده شد؛ فهرست را پیش از شروع بررسی کنید";}finally{f.delete();}});
  if(req==102)task(()->{JSONObject job=JobStore.load(this);if(job==null)return "گزارشی وجود ندارد";try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("فایل گزارش باز نشد");out.write(new byte[]{(byte)239,(byte)187,(byte)191});StringBuilder b=new StringBuilder("row,phone,name,status,status_fa,message_id,error,confirmation,kind,group_link,sent_at,reviewed_at,scheduled_start_at,interval_seconds\r\n");JSONArray a=job.getJSONArray("recipients");for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);b.append(r.optInt("row")).append(',').append(csv(r.optString("phone"))).append(',').append(csv(r.optString("name"))).append(',').append(csv(r.optString("status"))).append(',').append(csv(JobStore.statusLabel(r))).append(',').append(csv(r.optString("message_id"))).append(',').append(csv(r.optString("error"))).append(',').append(csv(r.optString("confirmation"))).append(',').append(csv(r.optString("kind","contact"))).append(',').append(csv(r.optString("link"))).append(',').append(csv(SentHistoryStore.format(r.optLong("sent_at")))).append(',').append(csv(SentHistoryStore.format(r.optLong("reviewed_at")))).append(',').append(csv(SentHistoryStore.format(job.optLong("scheduled_start_at")))).append(',').append(SendInterval.fromJob(job)).append("\r\n");}out.write(b.toString().getBytes(StandardCharsets.UTF_8));}return "گزارش ذخیره شد";});
 }
 static String csv(String s){if(s.matches("^[=+@\\-\\t\\r].*"))s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
 void prepareStart(){if(busy||OperationGate.occupied())return;if(client==null||client.privateKey==null){toast("ابتدا وارد شوید");return;}
  try{
   int interval=SendInterval.requireValid(intervalSeconds.getText().toString());
   JSONObject old=JobStore.load(this);
   if(imported==null){if(old==null){toast("ابتدا اکسل را انتخاب کنید");return;}if(!client.guid.equals(old.optString("owner"))){toast("این صف متعلق به حساب دیگری است");return;}if(JobStore.uncertain(old)){resolveUnknown();return;}if(JobStore.next(old)<0){toast("صف تمام شده؛ برای صف جدید اکسل انتخاب کنید");return;}
    new AlertDialog.Builder(this).setTitle("ادامه صف ذخیره‌شده").setMessage(JobStore.summary(old)+"\n\nفاصله قبلی: "+SendInterval.fromJob(old)+" ثانیه؛ فاصله جدید: "+interval+" ثانیه\n\nبا همان متن قبلی:\n"+old.getString("message")).setNegativeButton("انصراف",null).setPositiveButton("ثبت فاصله و ادامه",(d,w)->{try{old.put("interval_seconds",interval);JobStore.save(this,old);launch();}catch(Exception e){toast(error(e));}}).show();return;}
   String text=message.getText().toString().trim();if(text.isEmpty()){toast("متن پیام را بنویسید");return;}
   long planned=scheduleEnabled.isChecked()?selectedStartAt:0;
   if(planned>0&&(planned<System.currentTimeMillis()+60000)){toast("زمان ارسال باید حداقل یک دقیقه در آینده باشد");return;}
   if(scheduleEnabled.isChecked()&&planned==0){toast("ابتدا تاریخ و ساعت را انتخاب کنید");return;}
   if(planned>0&&!ensureSchedulePermission())return;
   ExcelReader.Result snapshot=imported;String owner=client.guid;StringBuilder list=new StringBuilder("تعداد: "+snapshot.entries.size()+"\nفاصله بین پیام‌ها: "+interval+" ثانیه\nشروع: "+(planned>0?timeText(planned):"فوری")+"\n\nمتن:\n"+text+"\n\nگیرندگان:\n");
   for(ExcelReader.Entry e:snapshot.entries)list.append(e.row).append(" • ").append(e.name).append(" • +").append(e.phone).append('\n');
   if(old!=null)list.append("\nشروع این صف، گزارش و صف قبلی را جایگزین می‌کند. اگر لازم است ابتدا گزارش را ذخیره کنید.");
   TextView body=new TextView(this);body.setText(list.toString());body.setTextSize(15);body.setPadding(dp(20),dp(15),dp(20),dp(15));body.setTextIsSelectable(true);ScrollView sv=new ScrollView(this);sv.addView(body);
   new AlertDialog.Builder(this).setTitle("بررسی فهرست و شروع ارسال").setView(sv).setNegativeButton("انصراف",null).setPositiveButton(planned>0?"ثبت زمان‌بندی":"شروع ارسال",(d,w)->{try{
    JSONObject job=JobStore.create(snapshot,text,owner);job.put("interval_seconds",interval);
    if(planned>0){job.put("scheduled_start_at",planned);job.put("note","شروع خودکار برنامه‌ریزی شد: "+timeText(planned));}
    ScheduleManager.cancel(this);JobStore.save(this,job);
    imported=null;fileStatus.setText("فایل در صف ذخیره شد");
    if(planned>0){if(ScheduleManager.schedule(this,job))toast("زمان‌بندی ذخیره شد؛ ارسال از "+timeText(planned));else toast("مجوز زمان‌بندی در تنظیمات فعال نیست؛ زمان ذخیره شد ولی باید مجوز بدهید");}
    else launch();refreshStatus();
   }catch(Exception e){toast(error(e));}}).show();
  }catch(Exception e){toast(error(e));}
 }
 void launch(){
  try{JSONObject job=JobStore.load(this);if(job!=null&&job.optLong("scheduled_start_at")>System.currentTimeMillis()){
    if(ScheduleManager.schedule(this,job)){toast("زمان ارسال هنوز نرسیده است: "+timeText(job.optLong("scheduled_start_at")));return;}
    if(!ensureSchedulePermission())return;
  }}catch(Exception e){toast(error(e));return;}
  if(DiscoveryService.running||OperationGate.occupied()){toast("ابتدا جست‌وجو را متوقف کنید");return;}if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},11);return;}startForegroundService(new Intent(this,QueueService.class));toast("صف شروع شد؛ توقف از داخل برنامه یا اعلان");}
 @Override public void onRequestPermissionsResult(int req,String[] p,int[] g){super.onRequestPermissionsResult(req,p,g);if(req==12){if(g.length>0&&g[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)launchResearch();else toast("برای وضعیت و توقف جست‌وجو اعلان را فعال کنید");}if(req==11){if(g.length>0&&g[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)launch();else toast("برای نمایش وضعیت و دکمه توقف، اعلان را در تنظیمات فعال کنید؛ صف شروع نشد");}}
 void resolveUnknown(){
  if(QueueService.running||busy)return;
  try{
   JSONObject job=JobStore.load(this);if(job==null){toast("گزارشی وجود ندارد");return;}
   int index=JobStore.reviewIndex(job);if(index<0){toast("مورد نیازمند بررسی وجود ندارد");return;}
   if(client==null||!client.guid.equals(job.optString("owner"))){toast("ابتدا با حساب مربوط به این صف وارد شوید");return;}
   JSONObject row=job.getJSONArray("recipients").getJSONObject(index);long created=job.optLong("created");
   String prompt=JobStore.destination(row)+"\n\nمتن این صف:\n"+job.getString("message")+"\n\nفقط اگر همین پیام را در چت این شماره در روبیکا دیده‌اید، «پیام را دیدم؛ ثبت ارسال» را بزنید. این دکمه فقط گزارش را اصلاح می‌کند و هیچ پیامی نمی‌فرستد.\n\nاگر مطمئن نیستید، بعداً بررسی کنید؛ یا بدون تأیید ارسال از این مورد عبور کنید.";
   TextView body=new TextView(this);body.setText(prompt);body.setTextSize(15);body.setPadding(dp(18),dp(14),dp(18),dp(14));body.setTextIsSelectable(true);ScrollView sv=new ScrollView(this);sv.addView(body);
   new AlertDialog.Builder(this).setTitle("بررسی نتیجهٔ مورد "+(index+1)).setView(sv)
    .setPositiveButton("پیام را دیدم؛ ثبت ارسال",(d,w)->recordReview(created,index,true))
    .setNeutralButton("عبور بدون تأیید",(d,w)->recordReview(created,index,false))
    .setNegativeButton("بعداً",null).show();
  }catch(Exception e){toast(error(e));}
 }
 void recordReview(long created,int index,boolean seen){
  if(QueueService.running||busy){toast("ابتدا صف را متوقف کنید");return;}
  try{
   JSONObject fresh=JobStore.load(this);
   if(fresh==null||fresh.optLong("created")!=created)throw new IOException("صف تغییر کرده است؛ گزارش را دوباره باز کنید");
   if(client==null||!client.guid.equals(fresh.optString("owner")))throw new IOException("حساب مربوط به این صف آماده نیست");
   JobStore.review(fresh,index,seen,System.currentTimeMillis());JobStore.save(this,fresh);
   if(seen){try(SentHistoryStore history=new SentHistoryStore(this)){history.archive(fresh,fresh.getJSONArray("recipients").getJSONObject(index));}}
   refreshStatus();
   toast(seen?"در گزارش ثبت شد؛ این پیام دوباره ارسال نمی‌شود":"از این مورد بدون ارسال دوباره عبور شد");
  }catch(Exception e){toast(error(e));}
 }
 void export(){if(QueueService.running||busy)return;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("text/csv");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"Rubika-queue-report.csv");startActivityForResult(i,102);}
 void refreshStatus(){if(accountStatus==null)return;boolean signed=client!=null&&client.privateKey!=null,locked=busy||OperationGate.occupied();accountStatus.setText(signed?"حساب: +"+client.phone:"وارد حساب نشده‌اید");sendCode.setEnabled(!locked&&!signed);login.setEnabled(!locked&&!signed);phone.setEnabled(!locked&&!signed);password.setEnabled(!locked&&!signed);code.setEnabled(!locked&&!signed);test.setEnabled(!locked&&signed);logout.setEnabled(!locked&&signed);importButton.setEnabled(!locked);start.setEnabled(!locked&&signed);resolve.setEnabled(!locked);export.setEnabled(!locked);message.setEnabled(!locked);intervalSeconds.setEnabled(!locked);
  researchStart.setEnabled(!locked&&signed);researchExport.setEnabled(!locked);researchView.setEnabled(!busy);groupSend.setEnabled(!locked&&signed);searchTerms.setEnabled(!locked);searchGroups.setEnabled(!locked);searchContacts.setEnabled(!locked);
  try{discoveryStatus.setText((DiscoveryService.running?"● در حال جست‌وجو\n":"")+DiscoveryData.summary(DiscoveryStore.load(this)));}catch(Exception e){discoveryStatus.setText(error(e));}
  try{JSONObject j=JobStore.load(this);
   String state=j==null?"هنوز صفی ساخته نشده":(QueueService.running?"● در حال اجرا\n":j.optLong("scheduled_start_at",0)>System.currentTimeMillis()?"◷ زمان‌بندی‌شده برای "+timeText(j.optLong("scheduled_start_at"))+"\n":"○ متوقف / آماده\n")+JobStore.summary(j)+"\nفاصله بین ارسال‌ها: "+SendInterval.fromJob(j)+" ثانیه";
   if(signed){try(SentHistoryStore history=new SentHistoryStore(this)){int n=history.total(client.guid);state+="\n\nسابقهٔ پیام‌های ثبت‌شده: "+n;if(n>0)state+="\nآخرین موارد:\n"+history.shortLatest(client.guid,3);}}
   queueStatus.setText(state);
  }catch(Exception e){queueStatus.setText(error(e));}
 }
 void buildDiscovery(){
  LinearLayout d=card("گروه‌ها و شماره تماس‌ها");
  label(d,"جست‌وجوی خودکار و ذخیره در Excel؛ بدون ارسال پیام. هر بار نتایج تازه به فهرست قبلی اضافه می‌شوند.",14,false);
  searchTerms=input(d,"عبارت‌های جست‌وجو؛ هر عبارت یک خط",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);searchTerms.setMinLines(3);searchTerms.setText(getSharedPreferences("discovery",0).getString("terms",LeadExtractor.DEFAULT_TERMS));
  searchGroups=new CheckBox(this);searchGroups.setText("گروه‌ها؛ عمومی و گروه‌های خودم");searchGroups.setChecked(getSharedPreferences("discovery",0).getBoolean("groups",true));d.addView(searchGroups);
  searchContacts=new CheckBox(this);searchContacts.setText("شماره‌های تماس کاریِ عمومی");searchContacts.setChecked(getSharedPreferences("discovery",0).getBoolean("contacts",true));d.addView(searchContacts);
  label(d,"منابع: جست‌وجوی روبیکا، توضیح کانال‌های عمومی و صفحات عمومی برق. شمارهٔ خصوصی اعضا خوانده نمی‌شود. شمارهٔ پیدا‌شده مشتری تأییدشده نیست؛ منبع آن در Excel می‌آید.",13,false);
  researchStart=button(d,"جست‌وجوی تازه و ذخیره خودکار Excel",()->beginResearch());
  button(d,"توقف جست‌وجو",()->{if(DiscoveryService.running)startService(new Intent(this,DiscoveryService.class).setAction("STOP"));else toast("جست‌وجویی در حال اجرا نیست");});
  discoveryStatus=label(d,"نتایج هنوز دریافت نشده‌اند",14,false);
  researchView=button(d,"نمایش گروه‌ها و شماره تماس‌ها",()->showResearch());
  researchExport=button(d,"انتخاب محل / ذخیره Excel نتایج",()->{if(!busy&&!OperationGate.occupied()){pendingResearch=false;pickResearchFile();}});
  groupSend=button(d,"ارسال جداگانه به گروه‌های انتخابی",()->chooseGroups());
 }
 void beginResearch(){
  if(busy||OperationGate.occupied()){toast("ابتدا ارسال یا جست‌وجوی در حال اجرا را متوقف کنید");return;}
  if(client==null||client.privateKey==null){toast("ابتدا وارد حساب شوید");return;}
  if(!searchGroups.isChecked()&&!searchContacts.isChecked()){toast("گروه‌ها یا شماره تماس‌ها را انتخاب کنید");return;}
  if(LeadExtractor.terms(searchTerms.getText().toString()).length==0){toast("یک عبارت حداقل دوحرفی وارد کنید");return;}
  getSharedPreferences("discovery",0).edit().putString("terms",searchTerms.getText().toString()).putBoolean("groups",searchGroups.isChecked()).putBoolean("contacts",searchContacts.isChecked()).commit();
  if(getSharedPreferences("discovery",0).getString("output","").isEmpty()){pendingResearch=true;pickResearchFile();return;}launchResearch();
 }
 void pickResearchFile(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"Rubika-groups-contacts.xlsx");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,103);}
 void launchResearch(){
  if(busy||OperationGate.occupied()){toast("ابتدا عملیات فعلی را متوقف کنید");return;}
  if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},12);return;}
  startForegroundService(new Intent(this,DiscoveryService.class));toast("جست‌وجو شروع شد؛ هیچ پیامی ارسال نمی‌شود");
 }
 void researchFileChosen(Uri uri,Intent result){
  try{getContentResolver().takePersistableUriPermission(uri,result.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(SecurityException ignored){}
  getSharedPreferences("discovery",0).edit().putString("output",uri.toString()).commit();
  if(pendingResearch){pendingResearch=false;launchResearch();return;}
  task(()->{JSONObject state=DiscoveryStore.load(this);try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("فایل باز نشد");ResearchXlsx.write(state,out);}return "فایل Excel گروه‌ها و شماره‌های تماس ذخیره شد";});
 }
 void showResearch(){
  try{JSONObject state=DiscoveryStore.load(this);StringBuilder b=new StringBuilder(DiscoveryData.summary(state)+"\n\nگروه‌ها\n");JSONArray groups=state.getJSONArray("groups"),contacts=state.getJSONArray("contacts");
   for(int i=0;i<Math.min(80,groups.length());i++){JSONObject r=groups.getJSONObject(i);b.append(i+1).append(". ").append(r.optString("name")).append('\n').append(r.optString("visibility")).append('\n').append(r.optString("link")).append("\n\n");}
   b.append("شماره‌های تماس کاریِ عمومی\n");for(int i=0;i<Math.min(80,contacts.length());i++){JSONObject r=contacts.getJSONObject(i);b.append('+').append(r.optString("phone")).append(" • ").append(r.optString("name")).append('\n').append(r.optString("source")).append("\n\n");}
   b.append("همهٔ نتایج و وضعیت منابع در فایل Excel ذخیره می‌شوند.");TextView t=new TextView(this);t.setText(b.toString());t.setTextSize(15);t.setPadding(dp(18),dp(14),dp(18),dp(14));t.setTextIsSelectable(true);android.text.util.Linkify.addLinks(t,android.text.util.Linkify.WEB_URLS);t.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());ScrollView sv=new ScrollView(this);sv.addView(t);new AlertDialog.Builder(this).setTitle("گروه‌ها و شماره تماس‌ها").setView(sv).setPositiveButton("بستن",null).show();
  }catch(Exception e){toast(error(e));}
 }
 void chooseGroups(){
  if(busy||OperationGate.occupied())return;
  try{JSONObject state=DiscoveryStore.load(this);if(client==null||client.privateKey==null||!state.optString("owner").equals(client.guid)){toast("ابتدا با این حساب جست‌وجو کنید");return;}
   JSONArray all=state.getJSONArray("groups");java.util.ArrayList<JSONObject> groups=new java.util.ArrayList<>();for(int i=0;i<all.length();i++)if(all.getJSONObject(i).optString("guid").startsWith("g0"))groups.add(all.getJSONObject(i));
   if(groups.isEmpty()){toast("هنوز گروهی با شناسهٔ قابل بررسی پیدا نشده است");return;}String[] names=new String[groups.size()];boolean[] chosen=new boolean[names.length];for(int i=0;i<names.length;i++)names[i]=groups.get(i).optString("name");
   new AlertDialog.Builder(this).setTitle("انتخاب گروه؛ بدون عضویت خودکار").setMultiChoiceItems(names,chosen,(d,w,on)->chosen[w]=on).setNegativeButton("انصراف",null).setPositiveButton("متن و بررسی ارسال",(d,w)->{try{JSONArray selected=new JSONArray();for(int i=0;i<chosen.length;i++)if(chosen[i])selected.put(groups.get(i));if(selected.length()==0){toast("یک گروه انتخاب کنید");return;}confirmGroups(selected);}catch(Exception e){toast(error(e));}}).show();
  }catch(Exception e){toast(error(e));}
 }
 void confirmGroups(JSONArray groups)throws Exception {
  LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(18),dp(10),dp(18),dp(10));StringBuilder targets=new StringBuilder();for(int i=0;i<groups.length();i++)targets.append("• ").append(groups.getJSONObject(i).optString("name")).append('\n');
  label(box,targets.toString(),15,true);label(box,"باید عضو گروه و دارای اجازهٔ ارسال باشید. برنامه عضو گروه نمی‌شود. فاصلهٔ بین پیام‌های گروه از تنظیم همین صفحه خوانده می‌شود و حداقل ۳۰ ثانیه است. شروع، صف قبلی را جایگزین می‌کند؛ در صورت نیاز ابتدا گزارش قبلی را ذخیره کنید.",14,false);
  EditText body=input(box,"متن پیام گروه‌ها",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);body.setMinLines(4);body.setText(message.getText());body.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4200)});ScrollView sv=new ScrollView(this);sv.addView(box);
  new AlertDialog.Builder(this).setTitle("بررسی و شروع ارسال گروهی").setView(sv).setNegativeButton("انصراف",null).setPositiveButton("ساخت صف و ارسال",(d,w)->{try{if(busy||OperationGate.occupied())return;String text=body.getText().toString().trim();if(text.isEmpty()){toast("متن پیام را وارد کنید");return;}int interval=SendInterval.requireValid(intervalSeconds.getText().toString());JSONObject job=JobStore.createGroups(groups,text,client.guid);job.put("interval_seconds",interval);JobStore.save(this,job);imported=null;fileStatus.setText("صف گروه‌های انتخابی ذخیره شد");launch();}catch(Exception e){toast(error(e));}}).show();
 }


 String timeText(long at){return new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault()).format(new Date(at));}
 boolean ensureSchedulePermission(){
  if(ScheduleManager.permissionGranted(this))return true;
  new AlertDialog.Builder(this).setTitle("مجوز زمان‌بندی دقیق")
   .setMessage("اندروید برای شروع خودکار ارسال در ساعت انتخابی به مجوز «هشدارها و یادآورها» نیاز دارد. پس از فعال‌کردن مجوز، دوباره دکمهٔ ثبت زمان‌بندی را بزنید.")
   .setNegativeButton("انصراف",null)
   .setPositiveButton("باز کردن تنظیمات",(d,w)->{
    try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}
    catch(Exception e){toast("تنظیمات زمان‌بندی در این گوشی در دسترس نیست");}
   }).show();
  return false;
 }
 void chooseSchedule(){
  if(busy||QueueService.running){toast("برای تغییر زمان، ابتدا صف فعلی را متوقف کنید");return;}
  Calendar c=Calendar.getInstance();c.setTimeInMillis(selectedStartAt>System.currentTimeMillis()?selectedStartAt:System.currentTimeMillis()+3600000L);
  new DatePickerDialog(this,(dateView,y,m,d)->{
   Calendar chosen=Calendar.getInstance();chosen.set(y,m,d,c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),0);
   new TimePickerDialog(this,(timeView,h,minute)->{
    chosen.set(Calendar.HOUR_OF_DAY,h);chosen.set(Calendar.MINUTE,minute);chosen.set(Calendar.SECOND,0);chosen.set(Calendar.MILLISECOND,0);
    long when=chosen.getTimeInMillis();
    if(when<System.currentTimeMillis()+60000){toast("زمان انتخابی باید حداقل یک دقیقه در آینده باشد");return;}
    selectedStartAt=when;scheduleEnabled.setChecked(true);
    scheduleStatus.setText("زمان انتخاب‌شده: "+timeText(when)+" (ساعت گوشی)");
   },c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),true).show();
  },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
 }
 void scheduleSavedJob(){
  if(busy||OperationGate.occupied()){toast("ابتدا عملیات جاری را متوقف کنید");return;}
  if(client==null||client.privateKey==null){toast("ابتدا وارد حساب شوید");return;}
  if(selectedStartAt<System.currentTimeMillis()+60000||!scheduleEnabled.isChecked()){toast("ابتدا در بخش فایل و متن تاریخ و ساعت آینده را انتخاب کنید");return;}
  if(!ensureSchedulePermission())return;
  try{JSONObject job=JobStore.load(this);if(job==null||!client.guid.equals(job.optString("owner"))){toast("صف متعلق به این حساب موجود نیست");return;}
   if(JobStore.next(job)<0||JobStore.uncertain(job)){toast("صف آماده نیست؛ موارد نیازمند بررسی را تعیین تکلیف کنید");return;}
   new AlertDialog.Builder(this).setTitle("زمان‌بندی ادامهٔ صف")
    .setMessage("صف ذخیره‌شده با همان متن و گیرندگان باقی‌مانده در "+timeText(selectedStartAt)+" اجرا شود؟")
    .setNegativeButton("انصراف",null).setPositiveButton("ثبت",(d,w)->{
     try{ScheduleManager.cancel(this);job.put("scheduled_start_at",selectedStartAt);job.put("note","زمان‌بندی ادامهٔ صف: "+timeText(selectedStartAt));JobStore.save(this,job);
      if(ScheduleManager.schedule(this,job))toast("زمان‌بندی ادامهٔ صف ثبت شد");else toast("زمان‌بندی ثبت نشد؛ مجوز تنظیمات را بررسی کنید");refreshStatus();}
     catch(Exception e){toast(error(e));}
    }).show();
  }catch(Exception e){toast(error(e));}
 }
 void showSentHistory(int page){
  if(client==null||client.privateKey==null){toast("ابتدا وارد همان حساب شوید");return;}
  try(SentHistoryStore h=new SentHistoryStore(this)){
   JSONObject current=JobStore.load(this);
   if(current!=null&&client.guid.equals(current.optString("owner")))h.reconcile(current);
   int count=h.total(client.guid),pages=Math.max(1,(count+49)/50);
   String body=count==0?"هنوز ارسال ثبت‌شده‌ای برای این حساب وجود ندارد. موارد در انتظار و نیازمند بررسی، پیام ارسال‌شده محسوب نمی‌شوند.":
      "جمع کل سوابق: "+count+"\nصفحه "+(page+1)+" از "+pages+"\nوضعیت «پذیرفته‌شده توسط سرور» الزاماً تحویل به مخاطب را تأیید نمی‌کند.\n\n"+h.page(client.guid,page,50);
   TextView text=new TextView(this);text.setText(body);text.setTextSize(15);text.setTextDirection(View.TEXT_DIRECTION_RTL);
   text.setPadding(dp(18),dp(12),dp(18),dp(14));text.setTextIsSelectable(true);
   ScrollView scroll=new ScrollView(this);scroll.addView(text);
   AlertDialog.Builder dlg=new AlertDialog.Builder(this).setTitle("سوابق پیام‌های ارسال‌شده").setView(scroll)
       .setNegativeButton("بستن",null);
   if(page>0)dlg.setNeutralButton("صفحه قبل",(d,w)->showSentHistory(page-1));
   if(page+1<pages)dlg.setPositiveButton("صفحه بعد",(d,w)->showSentHistory(page+1));
   dlg.show();
  }catch(Exception e){toast(error(e));}
 }
 @Override protected void onResume(){super.onResume();ScheduleManager.rearm(this);}

 @Override protected void onDestroy(){handler.removeCallbacks(refresh);io.shutdown();super.onDestroy();}
}
