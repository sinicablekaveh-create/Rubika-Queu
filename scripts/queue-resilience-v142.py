#!/usr/bin/env python3
"""Update queue to continue after one recipient's definite rejection without unsafe retries."""
from pathlib import Path
p=Path("app/src/main/java/ir/sinicable/rubikaqueue/QueueService.java")
s=p.read_text(encoding="utf-8")
def once(old,new):
    global s
    if s.count(old)!=1:raise RuntimeError(f"Expected one occurrence, found {s.count(old)}: {old[:70]}")
    s=s.replace(old,new,1)
# An uncertain send must not be automatically sent again, but other recipients may proceed.
once('   if(JobStore.uncertain(job))throw new Exception("ابتدا مورد نامشخص را در روبیکا بررسی کنید");',
     '   if(JobStore.uncertain(job))checkpoint(job,"موارد با نتیجه نامشخص محفوظ هستند؛ صف سایر گیرندگان ادامه می‌یابد");')
# Start sticky is a best-effort recovery hint, not an OS keepalive guarantee.
once('  return START_NOT_STICKY;\n }\n void persistQueueStartError(String reason)',
     '  return START_STICKY;\n }\n void persistQueueStartError(String reason)')
once('     if("INVALID_INPUT".equalsIgnoreCase(c)){\n      dispatch=false;\n      current.put("status","pending");\n      current.put("error",MainActivity.error(api));\n      checkpoint(job,"درخواست روبیکا رد شد (INVALID_INPUT)؛ صف متوقف و مورد در انتظار اصلاح ماند");\n      return;\n     }',
     '''     if("INVALID_INPUT".equalsIgnoreCase(c)){
      dispatch=false;
      current.put("status","skipped");
      current.put("error",MainActivity.error(api));
      job.put("last_attempt",System.currentTimeMillis());
      nextAllowed=SystemClock.elapsedRealtime()+intervalMs;
      checkpoint(job,"مورد "+(n+1)+" ورودی نامعتبر داشت و رد شد؛ ادامه صف با فاصله تنظیم‌شده");
      current=null;
      continue;
     }''')
# Rate-limit logic already handles the explicit retryable code during send, no duplicates.
# Surround one recipient's work with a per-row exception handler.
once('         current=job.getJSONArray("recipients").getJSONObject(n);dispatch=false;',
     '    current=job.getJSONArray("recipients").getJSONObject(n);dispatch=false;\n    try {')
last='''    current=null;
   }
   if(cancel&&job!=null)'''
replace='''    current=null;
    } catch(InterruptedException interrupted) {
      throw interrupted;
    } catch(RubikaClient.ApiError api) {
      String code=api.code==null?"":api.code.toUpperCase(java.util.Locale.ROOT);
      if("INVALID_AUTH".equals(code))throw api;
      // RATE/FLOOD means backoff, not a reason to skip or hammer the API.
      if(code.contains("RATE")||code.contains("FLOOD")||code.contains("TOO_REQUESTS")) {
        long retry=Math.max(1,Math.min(8,job.optLong("retry_count",0)+1));
        long delay=RetryBackoff.delayMs(retry,(long)(Math.random()*10000));
        current.put("status","retry_wait");current.put("error",MainActivity.error(api));
        job.put("retry_count",retry);
        job.put("next_retry_time",System.currentTimeMillis()+delay);
        dispatch=false;
        checkpoint(job,"محدودیت روبیکا برای مورد "+(n+1)+"؛ انتظار "+((delay+999)/1000)+" ثانیه");
        current=null;continue;
      }
      // Any explicit non-retryable server rejection is not an accepted message.
      // Do not retry rejected rows or stop the other rows.
      current.put("status","skipped");
      current.put("error",MainActivity.error(api));
      dispatch=false;
      job.put("last_attempt",System.currentTimeMillis());
      nextAllowed=SystemClock.elapsedRealtime()+intervalMs;
      checkpoint(job,"مورد "+(n+1)+" از طرف سرور رد شد ("+code+")؛ ادامه صف");
      current=null;continue;
    } catch(Exception problem) {
      if(current!=null && dispatch) {
        // Transport failure during send: delivery is ambiguous. Never silently resend.
        current.put("status","uncertain");
        current.put("error","نتیجه ارسال نامشخص؛ "+MainActivity.error(problem));
        dispatch=false;
        checkpoint(job,"نتیجه مورد "+(n+1)+" نامشخص است؛ ارسال تکرار نشد و سایر موارد ادامه دارند");
        current=null;continue;
      }
      String description=MainActivity.error(problem);
      boolean invalid=problem instanceof IllegalArgumentException ||
          description.contains("صف گروهی قدیمی") ||
          description.contains("تطبیق شماره") ||
          description.contains("حساب روبیکا برای این شماره");
      if(invalid) {
        current.put("status","skipped");
        current.put("error",description);
        job.put("last_attempt",System.currentTimeMillis());
        nextAllowed=SystemClock.elapsedRealtime()+intervalMs;
        checkpoint(job,"مورد "+(n+1)+" قابل ارسال نبود و کنار گذاشته شد؛ ادامه صف");
      } else {
        // Only failures before dispatch can safely be retried automatically.
        long retry=Math.max(1,Math.min(8,job.optLong("retry_count",0)+1));
        long delay=RetryBackoff.delayMs(retry,(long)(Math.random()*10000));
        current.put("status","retry_wait");
        current.put("error",description);
        job.put("retry_count",retry);
        job.put("next_retry_time",System.currentTimeMillis()+delay);
        checkpoint(job,"خطای موقت پیش از ارسال مورد "+(n+1)+"؛ تلاش مجدد پس از "+((delay+999)/1000)+" ثانیه");
      }
      current=null;continue;
    }
   }
   if(cancel&&job!=null)'''
once(last,replace)
# For prolonged offline periods keep progress durable.
# Don't call the queue "always running" because Android can still kill the process.
p.write_text(s,encoding="utf-8")
print("QUEUE_RESILIENCE_V142_PATCHED")
