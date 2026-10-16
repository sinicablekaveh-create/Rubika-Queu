package dev.hooshika.rubikamanager;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

public class MainActivity extends Activity {
  private static final int FOLDER=1, IMPORT=2, BACKUP=3, REPORT=4, APK=5;
  private static final int MAX_FILES=500;
  private static final long MAX_BYTES=100L*1024*1024;
  private Uri folder;
  private TextView state,files,installed;
  private final ExecutorService pool=Executors.newSingleThreadExecutor();
  private final Handler ui=new Handler(Looper.getMainLooper());
  private final int ink=Color.rgb(17,38,58), teal=Color.rgb(0,114,110), muted=Color.rgb(94,109,127);

  private static class Item {
    final Uri uri; final String path; final long size; final String mime;
    Item(Uri u,String p,long s,String m){uri=u;path=p;size=s;mime=m;}
  }
  @Override public void onCreate(Bundle s) {
    super.onCreate(s);
    getWindow().setStatusBarColor(ink);
    String v=getPreferences(MODE_PRIVATE).getString("folder",null);
    if(v!=null){
      Uri u=Uri.parse(v);
      for(android.content.UriPermission p:getContentResolver().getPersistedUriPermissions())
        if(p.getUri().equals(u)&&p.isReadPermission()&&p.isWritePermission()) folder=u;
    }
    screen(); refresh();
  }
  private int d(int n){return (int)(getResources().getDisplayMetrics().density*n+0.5f);}
  private GradientDrawable shape(int color){GradientDrawable s=new GradientDrawable();s.setColor(color);s.setCornerRadius(d(17));return s;}
  private TextView text(String s,int size,int color,boolean bold){
    TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);
    v.setGravity(Gravity.RIGHT);v.setTextDirection(View.TEXT_DIRECTION_RTL);
    if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
    return v;
  }
  private void space(LinearLayout parent,int h){View v=new View(this);parent.addView(v,new LinearLayout.LayoutParams(1,d(h)));}
  private LinearLayout card(LinearLayout root,String title,String desc){
    LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);
    c.setPadding(d(16),d(17),d(16),d(17));c.setBackground(shape(Color.WHITE));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=d(14);root.addView(c,lp);
    c.addView(text(title,19,ink,true));space(c,6);c.addView(text(desc,13,muted,false));space(c,10);return c;
  }
  private void button(LinearLayout c,String title,boolean main,View.OnClickListener f){
    Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(14);
    b.setTextColor(main?Color.WHITE:teal);b.setBackground(shape(main?teal:Color.rgb(234,246,244)));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,d(50));lp.bottomMargin=d(7);c.addView(b,lp);b.setOnClickListener(f);
  }
  private void screen(){
    ScrollView sc=new ScrollView(this);sc.setBackgroundColor(Color.rgb(244,247,250));
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
    root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setPadding(d(17),d(22),d(17),d(20));
    sc.addView(root);setContentView(sc);
    root.addView(text("مدیر پروژه روبیکا",25,ink,true));
    space(root,7);root.addView(text("مدیریت فایل‌ها و برنامه‌ها روی همین گوشی، بدون کامپیوتر یا Termux",14,muted,false));space(root,16);
    LinearLayout a=card(root,"پوشه پروژه","تنها پوشه‌ای در دسترس است که خودتان با مجوز اندروید انتخاب کنید.");
    state=text("پوشه انتخاب نشده است",14,teal,true);a.addView(state);space(a,8);
    button(a,"انتخاب پوشه پروژه",true,v->chooseFolder());
    button(a,"نمایش و نوسازی فایل‌ها",false,v->refresh());
    files=text("—",13,ink,false);a.addView(files);
    LinearLayout b=card(root,"فایل‌ها و پشتیبان","کپی امن فایل‌های CSV، Excel و سایر فایل‌های پروژه؛ بدون حذف فایل اصلی.");
    button(b,"وارد کردن فایل",true,v->chooseFile());
    button(b,"پشتیبان ZIP از پوشه",false,v->askBackup());
    button(b,"خروجی گزارش بدون اطلاعات خصوصی",false,v->createDocument("application/json","rubika-report-"+stamp()+".json",REPORT));
    LinearLayout c=card(root,"برنامه‌های نصب‌شده","نسخه‌ها بررسی می‌شوند، اما به اطلاعات خصوصی صف و حساب دسترسی نداریم.");
    installed=text("در حال بررسی…",13,ink,false);c.addView(installed);space(c,9);
    button(c,"بازکردن صف روبیکا — نسخه دوم",true,v->launch("ir.sinicable.rubikaqueue.sidecar"));
    button(c,"بازکردن صف روبیکا — نسخه اصلی",false,v->launch("ir.sinicable.rubikaqueue"));
    button(c,"انتخاب فایل APK برای نصب دستی",false,v->askApk());
    LinearLayout e=card(root,"امنیت و توسعه","این ابزار پیام ارسال نمی‌کند و مجوز اینترنت یا دسترسی به همه فایل‌های گوشی ندارد.");
    button(e,"مشاهده پروژه در GitHub",false,v->{
      try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/sinicablekaveh-create/Rubika-Queu")));}
      catch(Exception ex){toast("مرورگر پیدا نشد.");}
    });
    e.addView(text("ZIP پشتیبان رمز ندارد و باید خصوصی بماند. فایل APK آزمایشی را بدون اطمینان از شناسه و امضا روی نسخه قبلی نصب نکنید.",13,muted,false));
  }
  private void toast(String x){Toast.makeText(this,x,Toast.LENGTH_LONG).show();}
  private boolean validFolder(){if(folder==null){toast("اول پوشه پروژه را انتخاب کنید.");return false;}return true;}
  private void chooseFolder(){
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    startActivityForResult(i,FOLDER);
  }
  private void chooseFile(){
    if(!validFolder())return; Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,IMPORT);
  }
  private void createDocument(String mime,String name,int code){
    Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType(mime);i.addCategory(Intent.CATEGORY_OPENABLE);
    i.putExtra(Intent.EXTRA_TITLE,name);startActivityForResult(i,code);
  }
  private void askBackup(){
    if(!validFolder())return;
    new AlertDialog.Builder(this).setTitle("پشتیبان بدون رمز").setMessage("این ZIP می‌تواند حاوی فایل‌های خصوصی باشد. حداکثر ۵۰۰ فایل و ۱۰۰ مگابایت. از آن محافظت کنید.")
      .setNegativeButton("انصراف",null).setPositiveButton("ادامه",(a,b)->createDocument("application/zip","Rubika-backup-"+stamp()+".zip",BACKUP)).show();
  }
  private void askApk(){
    new AlertDialog.Builder(this).setTitle("بررسی پیش از نصب").setMessage("برنامه‌های فعلی و داده‌های صف را حذف نکنید. اندروید باید نصب را تأیید کند. امضای متفاوت، جایگزینی نسخه قبلی را ممکن نمی‌کند.")
      .setNegativeButton("انصراف",null).setPositiveButton("انتخاب APK",(a,b)->{
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/vnd.android.package-archive");
        i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,APK);
      }).show();
  }
  @Override @SuppressWarnings("deprecation")
  protected void onActivityResult(int req,int result,Intent data){
    super.onActivityResult(req,result,data);
    if(result!=RESULT_OK||data==null||data.getData()==null)return;
    Uri u=data.getData();
    if(req==FOLDER){
      int f=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
      if(f!=(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION)){toast("دسترسی خواندن و نوشتن پوشه لازم است.");return;}
      try{
        getContentResolver().takePersistableUriPermission(u,f);folder=u;
        getPreferences(MODE_PRIVATE).edit().putString("folder",u.toString()).apply();refresh();
      }catch(SecurityException ex){toast("مجوز پوشه ذخیره نشد.");}
    }else if(req==IMPORT) async("در حال وارد کردن فایل…",()->copy(u));
    else if(req==BACKUP) async("در حال تهیه ZIP…",()->backup(u));
    else if(req==REPORT) async("در حال ساخت گزارش…",()->report(u));
    else if(req==APK){
      String name=name(u);
      if(!name.toLowerCase(Locale.ROOT).endsWith(".apk")){toast("فایل APK معتبر انتخاب کنید.");return;}
      Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(u,"application/vnd.android.package-archive");
      i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      try{startActivity(i);}catch(Exception ex){toast("نصب‌کننده اندروید باز نشد.");}
    }
  }
  private interface Task{String run()throws Exception;}
  private void async(String message,Task t){
    state.setText(message);
    pool.execute(()->{
      String messageOut;
      try{messageOut=t.run();}catch(Exception ex){messageOut="عملیات انجام نشد: "+ex.getClass().getSimpleName()+"؛ مجوز و فضای حافظه را بررسی کنید.";}
      final String output=messageOut;
      ui.post(()->{toast(output);refresh();});
    });
  }
  private static String stamp(){return new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US).format(new Date());}
  private String pkg(String p,String label){
    try{PackageInfo x=getPackageManager().getPackageInfo(p,0);return label+": نصب ("+x.versionName+")";}
    catch(Exception e){return label+": قابل مشاهده نیست";}
  }
  private void launch(String p){
    try{
      Intent i=getPackageManager().getLaunchIntentForPackage(p);
      if(i==null)toast("برنامه نصب‌شده قابل اجرا پیدا نشد.");else startActivity(i);
    }catch(Exception ex){toast("باز کردن برنامه ممکن نشد.");}
  }
  private void refresh(){
    state.setText(folder==null?"پوشه انتخاب نشده است.":"پوشه انتخاب‌شده مجاز است.");
    installed.setText(pkg("ir.sinicable.rubikaqueue","اصلی")+"\n"+pkg("ir.sinicable.rubikaqueue.sidecar","نسخه دوم"));
    if(folder==null){files.setText("لطفاً پوشه پروژه را انتخاب کنید.");return;}
    pool.execute(()->{
      try{
        List<Item> list=scan();
        long size=0;int csv=0,xls=0,apk=0;
        StringBuilder names=new StringBuilder();
        for(Item x:list){
          size+=x.size;String p=x.path.toLowerCase(Locale.ROOT);
          if(p.endsWith(".csv"))csv++;if(p.endsWith(".xlsx")||p.endsWith(".xls"))xls++;if(p.endsWith(".apk"))apk++;
          if(names.length()<700)names.append("• ").append(x.path).append("\n");
        }
        final String s=list.size()+" فایل | "+String.format(Locale.US,"%.1f",size/1048576.0)+" مگابایت\nCSV: "+csv+" | Excel: "+xls+" | APK: "+apk+"\n\n"+names;
        ui.post(()->files.setText(s));
      }catch(Exception ex){ui.post(()->files.setText("خطا در خواندن پوشه یا تعداد فایل بیش از حد مجاز."));}
    });
  }
  private Uri root(){
    return DocumentsContract.buildDocumentUriUsingTree(folder,DocumentsContract.getTreeDocumentId(folder));
  }
  private String name(Uri uri){
    try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
      if(c!=null&&c.moveToFirst())return c.getString(0);
    }catch(Exception ignored){}
    return "file-"+stamp();
  }
  private List<Item> scan()throws IOException{
    if(folder==null)throw new IOException("No folder");
    List<Item> list=new ArrayList<>();
    inspect(DocumentsContract.getTreeDocumentId(folder),"",0,list,new HashSet<>());
    return list;
  }
  private void inspect(String id,String prefix,int depth,List<Item> output,Set<String> visited)throws IOException{
    if(depth>8||!visited.add(id))return;
    Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(folder,id);
    String[] col={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE};
    try(Cursor c=getContentResolver().query(children,col,null,null,null)){
      if(c==null)throw new IOException("Cannot read directory");
      while(c.moveToNext()){
        String did=c.getString(0), n=c.getString(1), mime=c.getString(2);
        long sz=c.isNull(3)?0:Math.max(0,c.getLong(3));
        if(n==null||n.contains("/")||n.contains("\\")||n.equals(".."))continue;
        String path=prefix+n;
        if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mime))inspect(did,path+"/",depth+1,output,visited);
        else {
          if(output.size()>=MAX_FILES)throw new IOException("Too many files");
          output.add(new Item(DocumentsContract.buildDocumentUriUsingTree(folder,did),path,sz,mime));
        }
      }
    }
  }
  private String copy(Uri source)throws IOException{
    String original=name(source).replace("/","_").replace("\\","_");
    if(original.length()>120)original=original.substring(original.length()-120);
    Set<String> occupied=new HashSet<>();
    for(Item x:scan())if(!x.path.contains("/"))occupied.add(x.path);
    if(occupied.contains(original))original="import-"+stamp()+"-"+original;
    String mime=getContentResolver().getType(source);
    if(mime==null)mime="application/octet-stream";
    Uri dest=DocumentsContract.createDocument(getContentResolver(),root(),mime,original);
    if(dest==null)throw new IOException("Cannot create file");
    boolean ok=false;
    try(InputStream in=getContentResolver().openInputStream(source);OutputStream out=getContentResolver().openOutputStream(dest,"w")){
      if(in==null||out==null)throw new IOException("Input/output unavailable");
      byte[] buf=new byte[32768];int n;long total=0;
      while((n=in.read(buf))!=-1){
        total+=n;if(total>MAX_BYTES)throw new IOException("File too big");
        out.write(buf,0,n);
      }
      ok=true;
    }finally{if(!ok)try{DocumentsContract.deleteDocument(getContentResolver(),dest);}catch(Exception ignored){}}
    return "فایل بدون حذف اصل کپی شد.";
  }
  private String backup(Uri dest)throws IOException{
    List<Item> entries=scan();
    if(entries.isEmpty())throw new IOException("Empty project");
    String outputId="";
    try{outputId=DocumentsContract.getDocumentId(dest);}catch(Exception ignored){}
    long bytes=0;
    for(Item e:entries)if(!DocumentsContract.getDocumentId(e.uri).equals(outputId)){
      bytes+=e.size;if(bytes>MAX_BYTES)throw new IOException("Backup exceeds limit");
    }
    byte[] buf=new byte[32768];long written=0;int count=0;
    try(OutputStream raw=getContentResolver().openOutputStream(dest,"wt")){
      if(raw==null)throw new IOException("Cannot write backup");
      ZipOutputStream out=new ZipOutputStream(raw);
      for(Item e:entries){
        if(DocumentsContract.getDocumentId(e.uri).equals(outputId))continue;
        try(InputStream in=getContentResolver().openInputStream(e.uri)){
          if(in==null)throw new IOException("Cannot read file");
          out.putNextEntry(new ZipEntry(e.path));int n;
          while((n=in.read(buf))!=-1){
            written+=n;if(written>MAX_BYTES)throw new IOException("Backup too big");
            out.write(buf,0,n);
          }
          out.closeEntry();count++;
        }
      }
      out.finish();
    }
    return "ZIP پشتیبان ("+count+" فایل) ساخته شد؛ بدون رمز است.";
  }
  private String report(Uri dest)throws IOException{
    List<Item> entries=scan();int csv=0,excel=0,apk=0;long bytes=0;
    for(Item e:entries){
      bytes+=e.size;String s=e.path.toLowerCase(Locale.ROOT);
      if(s.endsWith(".csv"))csv++;
      if(s.endsWith(".xlsx")||s.endsWith(".xls"))excel++;
      if(s.endsWith(".apk"))apk++;
    }
    String json="{\n  \"tool\":\"Rubika Phone Manager 1.0\",\n  \"files\":"+entries.size()+",\n  \"bytes\":"+bytes+
      ",\n  \"csv\":"+csv+",\n  \"excel\":"+excel+",\n  \"apk\":"+apk+
      ",\n  \"contains_private_data\":false,\n  \"note\":\"No filenames, message text, phone numbers or tokens\"\n}\n";
    try(OutputStream stream=getContentResolver().openOutputStream(dest,"wt")){
      if(stream==null)throw new IOException("Cannot save report");
      stream.write(json.getBytes(StandardCharsets.UTF_8));
    }
    return "گزارش بدون محتوای خصوصی ذخیره شد؛ آن را در چت پیوست کنید.";
  }
  @Override protected void onDestroy(){super.onDestroy();pool.shutdown();}
}
