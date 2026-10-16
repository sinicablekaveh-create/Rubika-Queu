#!/usr/bin/env python3
"""Small fail-fast source overlay: report queue start failures and keep optional notifications optional."""
from pathlib import Path

root = Path("app/src/main/java/ir/sinicable/rubikaqueue")
main_path, service_path = root / "MainActivity.java", root / "QueueService.java"
main, service = main_path.read_text(encoding="utf-8"), service_path.read_text(encoding="utf-8")
marker_main = 'void showQueueDiagnostics()'
marker_service = 'void persistQueueStartError(String reason)'
if marker_main in main and marker_service in service:
    if main.count(marker_main) != 1 or service.count(marker_service) != 1:
        raise SystemExit("Ambiguous existing queue diagnostics")
    print("Queue startup diagnostics already installed")
    raise SystemExit(0)
if marker_main in main or marker_service in service:
    raise SystemExit("Partial diagnostics overlay; refusing to modify source")

def once(text, old, new, label):
    if text.count(old) != 1:
        raise SystemExit("Unexpected source: " + label + "; refusing to modify")
    return text.replace(old, new, 1)

main = once(
    main,
    '  LinearLayout q=card("۳. وضعیت ارسال");queueStatus=label(q,"صفی ساخته نشده",15,false);',
    '  LinearLayout q=card("۳. وضعیت ارسال");queueStatus=label(q,"صفی ساخته نشده",15,false);\n'
    '  button(q,"تشخیص علت کارنکردن صف (بدون ارسال)",()->showQueueDiagnostics());',
    "diagnostic button",
)
old_launch = ('  if(DiscoveryService.running||OperationGate.occupied()){toast("ابتدا جست‌وجو را متوقف کنید");return;}'
              'if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)'
              '!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions('
              'new String[]{android.Manifest.permission.POST_NOTIFICATIONS},11);return;}'
              'startForegroundService(new Intent(this,QueueService.class));'
              'toast("صف شروع شد؛ توقف از داخل برنامه یا اعلان");}')
new_launch = '''  if(DiscoveryService.running||OperationGate.occupied()){
   toast("عملیات دیگری فعال است؛ ابتدا آن را متوقف کنید");return;
  }
  // POST_NOTIFICATIONS is optional for starting a foreground service on Android 13+.
  // The OS still manages foreground-service visibility when drawer notifications are denied.
  try {
   startForegroundService(new Intent(this,QueueService.class));
   toast("درخواست شروع ثبت شد؛ نتیجه را در «وضعیت ارسال» ببینید");
  } catch(Exception e) {
   String reason="شروع صف ناموفق: "+error(e);
   toast(reason);
   try {JSONObject job=JobStore.load(this);if(job!=null){job.put("note",reason);JobStore.save(this,job);}}catch(Exception ignored){}
  }
 }'''
main = once(main,old_launch,new_launch,"startForegroundService and optional notification permission")
main = once(
    main,
    ' void resolveUnknown(){',
    ''' void showQueueDiagnostics(){
  try {
   JSONObject job=JobStore.load(this);
   StringBuilder out=new StringBuilder("گزارش محلی؛ بدون شماره، متن پیام یا کلید ورود\\n");
   out.append("حساب واردشده: ").append(client!=null&&client.privateKey!=null?"بله":"خیر");
   out.append("\\nصف فعال: ").append(QueueService.running?"بله":"خیر");
   out.append("\\nعملیات فعال دیگر: ").append(OperationGate.occupied()&&!QueueService.running?"بله":"خیر");
   out.append("\\nفایل اکسل تازه انتخاب‌شده: ").append(imported!=null?"بله":"خیر");
   out.append("\\nاجازه نمایش اعلان: ").append(android.os.Build.VERSION.SDK_INT<33||
     checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED?"بله":"خیر (برای شروع صف ضروری نیست)");
   if(job==null){out.append("\\nصف ذخیره‌شده: خیر");}
   else {
    JSONArray rows=job.getJSONArray("recipients");
    int pending=0;for(int i=0;i<rows.length();i++)if("pending".equals(rows.getJSONObject(i).optString("status")))pending++;
    out.append("\\nصف ذخیره‌شده: بله");
    out.append("\\nتعداد کل: ").append(rows.length()).append("؛ در انتظار: ").append(pending);
    out.append("\\nحساب مربوط به صف: ").append(client!=null&&client.guid.equals(job.optString("owner"))?"مطابق":"نامطابق/وارد نشده");
    out.append("\\nنیازمند بررسی ارسال نامشخص: ").append(JobStore.uncertain(job)?"بله":"خیر");
    out.append("\\nزمان‌بندی آینده: ").append(job.optLong("scheduled_start_at",0)>System.currentTimeMillis()?"بله":"خیر");
    out.append("\\nوضعیت آخر: ").append(job.optString("note","ثبت نشده"));
   }
   TextView view=new TextView(this);view.setPadding(dp(18),dp(12),dp(18),dp(12));
   view.setText(out.toString());view.setTextIsSelectable(true);
   ScrollView scroll=new ScrollView(this);scroll.addView(view);
   new AlertDialog.Builder(this).setTitle("تشخیص بدون ارسال پیام").setView(scroll)
    .setNeutralButton("کپی گزارش", (dialog,which)->{
     android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
     if(clipboard!=null){clipboard.setPrimaryClip(ClipData.newPlainText("وضعیت صف",out.toString()));toast("گزارش کپی شد");}
    }).setPositiveButton("بستن",null).show();
  }catch(Exception e){toast(error(e));}
 }
 void resolveUnknown(){''',
    "diagnostic UI",
)
service = once(service,
    '  if(!OperationGate.acquire("queue")){stopSelf();return START_NOT_STICKY;}',
    '  if(!OperationGate.acquire("queue")){persistQueueStartError("صف شروع نشد؛ عملیات دیگری فعال است");stopSelf();return START_NOT_STICKY;}',
    "queue busy gate"
)
service = once(service,
    '  }catch(Exception e){running=false;OperationGate.release("queue");stopSelf();}',
    '  }catch(Exception e){running=false;OperationGate.release("queue");'
    'persistQueueStartError("خطا در راه‌اندازی صف: "+MainActivity.error(e));'
    'if(wake!=null&&wake.isHeld())wake.release();stopSelf();}',
    "queue startup failure"
)
service = once(service,
    ' Notification notification(String text){',
    ''' void persistQueueStartError(String reason){
  try {JSONObject job=JobStore.load(this);if(job!=null){job.put("note",reason);JobStore.save(this,job);}}
  catch(Exception ignored){}
 }
 Notification notification(String text){''',
    "persistent startup diagnostic"
)
# No writes if any expected source pattern was not found.
main_path.write_text(main,encoding="utf-8")
service_path.write_text(service,encoding="utf-8")
print("Installed queue start-error visibility and optional notification permissions")
