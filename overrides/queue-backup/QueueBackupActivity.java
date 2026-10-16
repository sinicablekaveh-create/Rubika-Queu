package ir.sinicable.rubikaqueue;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;
import org.json.JSONArray;

/** Explicit owner-driven encrypted backup; never dispatches messages. */
public final class QueueBackupActivity extends Activity {
 private static final int WRITE=801,READ=802;
 private final ExecutorService io=Executors.newSingleThreadExecutor();
 private EditText secret;
 private TextView status;
 private JSONObject pending;
 private char[] password;
 private volatile boolean working=false;

 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  ScrollView scroll=new ScrollView(this);
  LinearLayout root=new LinearLayout(this);
  root.setOrientation(LinearLayout.VERTICAL);
  root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  int p=(int)(20*getResources().getDisplayMetrics().density);
  root.setPadding(p,p,p,p);
  scroll.addView(root);setContentView(scroll);
  status=label(root,"پشتیبان رمزگذاری‌شده صف (بدون ارسال)",20);
  label(root,"فایل شامل وضعیت ردیف‌ها، متن پیام و شناسه‌های ارسال است. با رمز حداقل ۱۲ کاراکتر رمزگذاری می‌شود. کلید ورود روبیکا در فایل نیست. رمز را از دست ندهید.",15);
  secret=new EditText(this);secret.setHint("رمز پشتیبان (حداقل ۱۲ کاراکتر)");
  secret.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
  root.addView(secret,new LinearLayout.LayoutParams(-1,-2));
  button(root,"ذخیره پشتیبان رمزگذاری‌شده",()->choose(WRITE));
  button(root,"انتخاب فایل و بازیابی به صف خالی",()->choose(READ));
  label(root,"بازیابی روی صف موجود انجام نمی‌شود؛ ارسال خودکار، زمان‌بندی و انتقال نشست ورود نیز انجام نمی‌شود.",14);
 }

 private TextView label(LinearLayout p,String text,int size){
  TextView view=new TextView(this);view.setText(text);view.setTextColor(Color.DKGRAY);
  view.setTextSize(size);view.setPadding(0,12,0,16);p.addView(view);return view;
 }
 private void button(LinearLayout p,String title,Runnable click){
  Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setOnClickListener(v->click.run());p.addView(b);
 }
 private boolean idle(){
  if(QueueService.running||DiscoveryService.running||OperationGate.occupied()||working){
   status.setText("ابتدا ارسال یا جست‌وجو را متوقف کنید");return false;
  }
  return true;
 }
 private void choose(int request){
  if(!idle())return;
  char[] candidate=secret.getText().toString().toCharArray();
  if(candidate.length<12||candidate.length>256){
   status.setText("رمز باید بین ۱۲ تا ۲۵۶ کاراکتر باشد");Arrays.fill(candidate,'\0');return;
  }
  if(password!=null)Arrays.fill(password,'\0');
  password=candidate;pending=null;
  Intent intent=new Intent(request==WRITE?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT);
  intent.addCategory(Intent.CATEGORY_OPENABLE);
  intent.setType("application/octet-stream");
  if(request==WRITE)intent.putExtra(Intent.EXTRA_TITLE,"RubikaQueue-secure-backup.rqbak");
  try{startActivityForResult(intent,request);}
  catch(Exception e){status.setText("انتخاب فایل ممکن نیست");clearPassword();}
 }
 @Override protected void onActivityResult(int req,int result,Intent data){
  super.onActivityResult(req,result,data);
  if(req!=WRITE&&req!=READ)return;
  if(result!=RESULT_OK||data==null||data.getData()==null){clearPassword();return;}
  Uri uri=data.getData();
  if(password==null||!idle()) {clearPassword();return;}
  char[] pass=password;password=null;
  working=true;
  if(req==WRITE)io.execute(()->export(uri,pass));
  else io.execute(()->importPreview(uri,pass));
 }

 private static byte[] readLimited(InputStream in)throws Exception{
  if(in==null)throw new IllegalArgumentException("Cannot open backup");
  ByteArrayOutputStream out=new ByteArrayOutputStream();
  byte[] buffer=new byte[8192];int n,total=0;
  while((n=in.read(buffer))!=-1){
   total+=n;
   if(total>QueueBackupCrypto.MAX_FILE)throw new IllegalArgumentException("Backup exceeds size limit");
   out.write(buffer,0,n);
  }
  return out.toByteArray();
 }
 private void export(Uri uri,char[] pass){
  byte[] plaintext=null,encrypted=null;
  try{
   if(QueueService.running||DiscoveryService.running||OperationGate.occupied())
    throw new IllegalStateException("Operation in progress");
   JSONObject job=JobStore.load(this);
   if(job==null)throw new IllegalStateException("No saved queue");
   RubikaClient account=Vault.load(this);
   if(account==null||!account.guid.equals(job.optString("owner")))
    throw new IllegalStateException("Queue account mismatch");
   plaintext=QueueBackupSchema.envelope(job,getPackageName()).toString().getBytes(StandardCharsets.UTF_8);
   encrypted=QueueBackupCrypto.encrypt(plaintext,pass);
   if(QueueService.running||DiscoveryService.running||OperationGate.occupied())
    throw new IllegalStateException("Operation in progress");
   try(OutputStream output=getContentResolver().openOutputStream(uri,"wt")){
    if(output==null)throw new IllegalStateException("Cannot write file");
    output.write(encrypted);output.flush();
   }
   show("پشتیبان رمزگذاری‌شده ذخیره شد؛ رمز و فایل را امن نگه دارید");
  }catch(Exception e){show("ذخیره پشتیبان انجام نشد. فایل خروجی را معتبر فرض نکنید.");}
  finally{if(plaintext!=null)Arrays.fill(plaintext,(byte)0);if(encrypted!=null)Arrays.fill(encrypted,(byte)0);Arrays.fill(pass,'\0');working=false;}
 }

 private void importPreview(Uri uri,char[] pass){
  byte[] file=null,plaintext=null;
  try{
   if(QueueService.running||DiscoveryService.running||OperationGate.occupied())
    throw new IllegalStateException("Operation in progress");
   if(JobStore.load(this)!=null)throw new IllegalStateException("A queue already exists");
   RubikaClient account=Vault.load(this);
   if(account==null||account.privateKey==null)throw new IllegalStateException("Login is required");
   try(InputStream in=getContentResolver().openInputStream(uri)){file=readLimited(in);}
   plaintext=QueueBackupCrypto.decrypt(file,pass);
   JSONObject restored=QueueBackupSchema.fromEnvelope(new JSONObject(new String(plaintext,StandardCharsets.UTF_8)));
   if(!account.guid.equals(restored.optString("owner")))
    throw new IllegalStateException("Backup belongs to a different account");
   int count=restored.getJSONArray("recipients").length();
   int review=0;JSONArray rows=restored.getJSONArray("recipients");
   for(int i=0;i<rows.length();i++)if(JobStore.needsReview(rows.getJSONObject(i)))review++;
   final JSONObject approved=restored;
   final int reviewCount=review;
   runOnUiThread(()->{
    working=false;pending=approved;
    new AlertDialog.Builder(this).setTitle("بررسی بازیابی بدون ارسال")
      .setMessage("تعداد ردیف‌ها: "+count+"\nنیازمند بررسی: "+reviewCount+
                  "\nهیچ پیام یا زمان‌بندی خودکار شروع نمی‌شود. آیا بازیابی به صف خالی انجام شود؟")
      .setNegativeButton("انصراف",(d,w)->pending=null)
      .setPositiveButton("بازیابی بدون ارسال",(d,w)->confirmRestore())
      .show();
   });
  }catch(Exception e){
   show("بازیابی ممکن نشد: رمز/فایل/حساب را بررسی کنید؛ صف فعلی تغییر نکرد.");
  }finally{if(file!=null)Arrays.fill(file,(byte)0);if(plaintext!=null)Arrays.fill(plaintext,(byte)0);Arrays.fill(pass,'\0');working=false;}
 }
 private void confirmRestore(){
  if(pending==null||!idle())return;
  JSONObject candidate=pending;pending=null;
  working=true;
  io.execute(()->{
   try{
    if(QueueService.running||DiscoveryService.running||OperationGate.occupied())
     throw new IllegalStateException("Operation active");
    if(JobStore.load(this)!=null)throw new IllegalStateException("Existing queue");
    RubikaClient account=Vault.load(this);
    if(account==null||!account.guid.equals(candidate.optString("owner")))
     throw new IllegalStateException("Account changed");
    JobStore.save(this,candidate);
    show("صف بازیابی شد؛ ارسال متوقف است. موارد نامشخص را ابتدا بررسی کنید.");
   }catch(Exception e){show("بازیابی متوقف شد؛ صف قبلی تغییر نکرد.");}
   finally{working=false;}
  });
 }
 private void show(String text){runOnUiThread(()->{secret.setText("");status.setText(text);});}
 private void clearPassword(){if(password!=null){Arrays.fill(password,'\0');password=null;}}
 @Override protected void onDestroy(){
  clearPassword();secret.setText("");pending=null;io.shutdownNow();super.onDestroy();
 }
}
