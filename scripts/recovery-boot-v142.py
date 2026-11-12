from pathlib import Path
root=Path("app/src/main/java/ir/sinicable/rubikaqueue")
def patch(name,a,b):
 p=root/name;s=p.read_text(encoding="utf-8")
 if s.count(a)!=1:raise RuntimeError(f"{name}: expected 1 occurrence, found {s.count(a)}")
 p.write_text(s.replace(a,b,1),encoding="utf-8")

patch("QueueService.java",
 'if(i!=null&&"STOP".equals(i.getAction())){cancel=true;',
 'if(i!=null&&"STOP".equals(i.getAction())){getSharedPreferences("queue_recovery",0).edit().putBoolean("resume",false).apply();cancel=true;')
patch("QueueService.java",
 '  running=true;cancel=false;',
 '  getSharedPreferences("queue_recovery",0).edit().putBoolean("resume",true).apply();\n  running=true;cancel=false;')
patch("QueueService.java",
 'if(n<0){checkpoint(job,JobStore.uncertain(job)?"موارد باقی‌مانده نیازمند بررسی دستی هستند؛ سایر گیرندگان پردازش شدند":"صف تمام شد");break;}',
 'if(n<0){getSharedPreferences("queue_recovery",0).edit().putBoolean("resume",false).apply();checkpoint(job,JobStore.uncertain(job)?"موارد باقی‌مانده نیازمند بررسی دستی هستند؛ سایر گیرندگان پردازش شدند":"صف تمام شد");break;}')
patch("QueueService.java",
 'if(current!=null){if(authRejected)',
 'if(authRejected)getSharedPreferences("queue_recovery",0).edit().putBoolean("resume",false).apply();\n    if(current!=null){if(authRejected)')
# Receiver tries restoring only user-initiated jobs and only if Android allows foreground launch.
patch("ScheduledQueueReceiver.java",
 '   ScheduleManager.rearm(context);return;',
 '''   ScheduleManager.rearm(context);
   try {
    if(!context.getSharedPreferences("queue_recovery",Context.MODE_PRIVATE).getBoolean("resume",false))return;
    JSONObject saved=JobStore.load(context);
    if(saved==null||JobStore.next(saved)<0)return;
    long planned=saved.optLong("scheduled_start_at",0);
    if(planned>System.currentTimeMillis())return;
    if(Vault.load(context)==null)return;
    context.startForegroundService(new Intent(context,QueueService.class));
   }catch(Exception unavailable) {
    // Android can disallow foreground service starts at boot. User may resume manually.
   }
   return;''')
print("BOOT_RECOVERY_PATCH_OK")
