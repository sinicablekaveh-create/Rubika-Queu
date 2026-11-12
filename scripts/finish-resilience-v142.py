from pathlib import Path
root=Path("app/src/main/java/ir/sinicable/rubikaqueue")
def change(file, old, new):
    p=root/file
    s=p.read_text(encoding="utf-8")
    if s.count(old)!=1:raise AssertionError(f"{file}: {s.count(old)} occurrences of {old[:50]}")
    p.write_text(s.replace(old,new,1),encoding="utf-8")

change("QueueService.java",
    'String c=api.code;',
    'String c=api.code==null?"":api.code.trim().toUpperCase(java.util.Locale.ROOT).replace(\' \',\'_\').replace(\'-\',\'_\');')
change("QueueService.java",
    'String code=api.code==null?"":api.code.toUpperCase(java.util.Locale.ROOT);',
    'String code=api.code==null?"":api.code.trim().toUpperCase(java.util.Locale.ROOT).replace(\' \',\'_\').replace(\'-\',\'_\');')
change("QueueService.java",
    'int n=JobStore.next(job);if(n<0){checkpoint(job,"صف تمام شد");break;}',
    'int n=JobStore.next(job);if(n<0){checkpoint(job,JobStore.uncertain(job)?"موارد باقی‌مانده نیازمند بررسی دستی هستند؛ سایر گیرندگان پردازش شدند":"صف تمام شد");break;}')
change("QueueService.java",
    'try(SentHistoryStore history=new SentHistoryStore(this)){history.archive(job,current);}',
    'try(SentHistoryStore history=new SentHistoryStore(this)){history.archive(job,current);}catch(Exception archiveError){/* Sent state is already durably saved. Do not retry delivery on archive failure. */}')
change("ScheduledQueueReceiver.java",
    '||JobStore.next(job)<0||JobStore.uncertain(job))return;',
    '||JobStore.next(job)<0)return;')
change("ScheduleManager.java",
    '||JobStore.next(job)<0||JobStore.uncertain(job)||!permissionGranted(c))return;',
    '||JobStore.next(job)<0||!permissionGranted(c))return;')
change("MainActivity.java",
    'if(JobStore.uncertain(old)){resolveUnknown();return;}',
    'if(JobStore.uncertain(old))toast("موارد نامشخص محفوظ‌اند؛ ادامه فقط برای سایر گیرندگان انجام می‌شود");')
change("MainActivity.java",
    'if(JobStore.next(job)<0||JobStore.uncertain(job)){toast("صف آماده نیست؛ موارد نیازمند بررسی را تعیین تکلیف کنید");return;}',
    'if(JobStore.next(job)<0){toast("گیرندهٔ آماده‌ای برای ادامه وجود ندارد");return;}')
change("MainActivity.java",
    'if("INVALID_INPUT".equalsIgnoreCase(code))return',
    'if("INVALID_INPUT".equalsIgnoreCase(code==null?"":code.trim().replace(\' \',\'_\').replace(\'-\',\'_\')))return')
change("MainActivity.java",
    'برای جلوگیری از ارسال تکراری، صف متوقف شد";',
    'در صف ارسال، فقط همان مورد کنار گذاشته می‌شود و بقیه ادامه دارند";')
change("MainActivity.java",
    'return "خطا: "+e.getClass().getSimpleName()+"؛ صف متوقف شد";',
    'return "خطا: "+e.getClass().getSimpleName()+"؛ وضعیت صف را بررسی کنید";')
# Ensure row-specific explicit invalid cases don't inflate retry count.
change("QueueService.java",
    'current.put("error",MainActivity.error(api));\n      job.put("last_attempt",System.currentTimeMillis());',
    'current.put("error",MainActivity.error(api));\n      job.put("retry_count",0);\n      job.put("last_attempt",System.currentTimeMillis());')
print("RESILIENCE_IMPROVEMENTS_COMPLETE")
