package ir.sinicable.rubikaqueue;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/** Native Persian RTL dashboard. Read-only state; all actions open existing screens. */
public final class FullDashboardActivity extends Activity {
 private static final int BG=0xfff3f8f9, INK=0xff193544, TEAL=0xff067e80, MUTED=0xff637988;
 private static final String[] TABS={"خانه","مدیریت صف","جست‌وجو","گزارش‌ها","تنظیمات"};
 private int tab=0;
 private LinearLayout body;
 private boolean ready=false;
 private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
 private GradientDrawable shape(int color){
  GradientDrawable d=new GradientDrawable(); d.setColor(color);d.setCornerRadius(dp(16));return d;
 }
 private LinearLayout column(){
  LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;
 }
 private TextView text(String s,int size,int color,boolean bold){
  TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
  t.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
  if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
 }
 private LinearLayout box(String heading,String explanation){
  LinearLayout c=column();c.setPadding(dp(16),dp(15),dp(16),dp(15));c.setBackground(shape(Color.WHITE));
  TextView h=text(heading,17,INK,true);c.addView(h);
  if(explanation!=null){TextView e=text(explanation,13,MUTED,false);e.setPadding(0,dp(8),0,dp(8));c.addView(e);}
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);body.addView(c,p);return c;
 }
 private void action(LinearLayout c,String title,Class<? extends Activity> dest){
  TextView b=text(title+"  ‹",14,TEAL,true);b.setPadding(dp(13),dp(10),dp(13),dp(10));
  b.setMinHeight(dp(48));b.setBackground(shape(0xffe3f5f3));
  b.setOnClickListener(v->startActivity(new Intent(this,dest)));c.addView(b);
 }
 private void stat(LinearLayout c,String name,String value){
  TextView t=text(name+"  :  "+value,14,INK,false);t.setMinHeight(dp(38));c.addView(t);
 }
 private void queueStats(LinearLayout c){
  try{
   JSONObject job=JobStore.load(this);
   if(job==null){stat(c,"صف محلی","هنوز ایجاد نشده");return;}
   JSONArray rows=job.optJSONArray("recipients");
   if(rows==null){stat(c,"صف","بدون ردیف");return;}
   int pending=0,ack=0,uncertain=0,other=0;
   for(int i=0;i<rows.length();i++){
    JSONObject r=rows.optJSONObject(i);String state=r==null?"":r.optString("status","");
    if("pending".equals(state))pending++;
    else if("sent".equals(state)||"confirmed".equals(state)||"acknowledged".equals(state))ack++;
    else if("uncertain".equals(state)||"sending".equals(state)||"retry_wait".equals(state))uncertain++;
    else other++;
   }
   stat(c,"کل ردیف‌ها",String.valueOf(rows.length()));
   stat(c,"در انتظار",String.valueOf(pending));
   stat(c,"ثبت‌شده در سرور",String.valueOf(ack));
   stat(c,"نامشخص / نیازمند بازبینی",String.valueOf(uncertain));
   stat(c,"سایر وضعیت‌ها",String.valueOf(other));
   stat(c,"صف در حال اجرا",QueueService.running?"بله":"خیر");
   String note=job.optString("note","");
   if(!note.isEmpty())stat(c,"آخرین وضعیت",note);
  }catch(Exception e){stat(c,"وضعیت","خطا در خواندن؛ از تشخیص مدیریت اصلی استفاده کنید");}
 }
 @Override public void onCreate(Bundle state){
  super.onCreate(state); if(state!=null)tab=state.getInt("tab",0);
  getWindow().setStatusBarColor(BG);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
  ready=true;render();
 }
 @Override protected void onResume(){super.onResume();if(ready)render();}
 @Override protected void onSaveInstanceState(Bundle state){state.putInt("tab",tab);super.onSaveInstanceState(state);}
 private void render(){
  LinearLayout shell=column();shell.setFitsSystemWindows(true);shell.setBackgroundColor(BG);setContentView(shell);
  TextView title=text("صف روبیکا  |  "+TABS[tab],20,INK,true);
  title.setPadding(dp(18),dp(15),dp(18),dp(15));shell.addView(title);
  ScrollView scroll=new ScrollView(this);body=column();body.setPadding(dp(16),dp(8),dp(16),dp(8));scroll.addView(body);
  shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
  if(tab==0){
   LinearLayout c=box("وضعیت واقعی صف","اعداد از همین نصب خوانده می‌شوند؛ هیچ داده نمایشی یا پیام ساختگی وجود ندارد.");
   queueStats(c);
   LinearLayout a=box("دسترسی سریع","اجرای عملیات فقط پس از ورود شما به صفحه مربوطه است.");
   action(a,"واردسازی Excel / CSV و صف واقعی",MainActivity.class);
   action(a,"جست‌وجوی عمومی",AiDiscoveryActivity.class);
  }else if(tab==1){
   queueStats(box("مدیریت و بازبینی","ارسال نامشخص بدون بررسی دستی تکرار نمی‌شود."));
   LinearLayout c=box("ابزارهای واقعی صف","ساخت، واردسازی، توقف و ارسال فقط از مدیریت اصلی.");
   action(c,"مدیریت اصلی صف",MainActivity.class);action(c,"پشتیبان رمزگذاری‌شده",QueueBackupActivity.class);
  }else if(tab==2){
   LinearLayout c=box("جست‌وجوی واقعی","از اطلاعات عمومی استفاده می‌کند؛ این صفحه نتیجه ساختگی نشان نمی‌دهد.");
   action(c,"کشف هوشمند گروه‌ها",AiDiscoveryActivity.class);
   action(c,"تنظیم موضوعات و منابع",MainActivity.class);
  }else if(tab==3){
   queueStats(box("گزارش محلی","ثبت در سرور، اثبات خوانده‌شدن پیام نیست."));
   LinearLayout c=box("گزارش تشخیصی","گزارش را بدون شماره، پیام یا کلید ورود تهیه کنید.");
   action(c,"نمایش تشخیص در مدیریت اصلی",MainActivity.class);
   action(c,"گزارش توسعه",DevelopmentReportActivity.class);
  }else{
   LinearLayout c=box("تنظیم جست‌وجوی قبل از صف","این گزینه به تنظیم واقعی موتور جست‌وجو متصل است.");
   CheckBox cb=new CheckBox(this);cb.setText("جست‌وجوی خودکار قبل از شروع صف");cb.setTextSize(14);
   cb.setChecked(getSharedPreferences("discovery",MODE_PRIVATE).getBoolean("auto_on_queue_start",true));
   cb.setOnCheckedChangeListener((v,b)->getSharedPreferences("discovery",MODE_PRIVATE).edit().putBoolean("auto_on_queue_start",b).apply());
   c.addView(cb);
   LinearLayout p=box("حریم خصوصی و نصب مستقل","شناسه این نسخه: "+getPackageName()+"\nداده و نشست نسخه‌های قبلی به این نصب منتقل نمی‌شود.");
   action(p,"اتصال و مدیریت حساب",MainActivity.class);action(p,"دستیار اختیاری",AiAssistantActivity.class);
  }
  LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  nav.setBackgroundColor(Color.WHITE);
  for(int i=0;i<TABS.length;i++){
   final int chosen=i;TextView t=text(TABS[i],11,i==tab?TEAL:MUTED,i==tab);
   t.setGravity(Gravity.CENTER);t.setMinHeight(dp(52));
   t.setOnClickListener(v->{tab=chosen;render();});
   nav.addView(t,new LinearLayout.LayoutParams(0,dp(58),1f));
  }
  shell.addView(nav);
 }
}