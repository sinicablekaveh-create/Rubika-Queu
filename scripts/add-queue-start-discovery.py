#!/usr/bin/env python3
"""Opt the existing group/contact finder into a serialized queue-start pass."""
from pathlib import Path
import shutil

base = Path("app/src/main/java/ir/sinicable/rubikaqueue")
main = base / "MainActivity.java"
queue = base / "QueueService.java"
main_text = main.read_text(encoding="utf-8")
queue_text = queue.read_text(encoding="utf-8")

field_anchor = "CheckBox scheduleEnabled;"
field_replacement = field_anchor + "CheckBox autoSearchOnQueue;"
if "autoSearchOnQueue" not in main_text:
    if main_text.count(field_anchor) != 1:
        raise SystemExit("Unexpected MainActivity fields; refusing queue search integration")
    main_text = main_text.replace(field_anchor, field_replacement, 1)

ui_anchor = "d.addView(searchContacts);"
ui_block = ui_anchor + '''
  autoSearchOnQueue=new CheckBox(this);autoSearchOnQueue.setText("شروع خودکار جست‌وجوی گروه و تماس عمومی با شروع صف");autoSearchOnQueue.setChecked(getSharedPreferences("discovery",0).getBoolean(QueueStartDiscoveryPolicy.PREFERENCE,true));autoSearchOnQueue.setOnCheckedChangeListener((button,checked)->getSharedPreferences("discovery",0).edit().putBoolean(QueueStartDiscoveryPolicy.PREFERENCE,checked).apply());d.addView(autoSearchOnQueue);
  label(d,"برای جلوگیری از درخواست هم‌زمان، ابتدا جست‌وجو تمام می‌شود و سپس ارسال صف ادامه پیدا می‌کند. در اعلان صف می‌توانید هر دو عملیات را متوقف کنید.",13,false);'''
if "شروع خودکار جست‌وجوی گروه و تماس عمومی" not in main_text:
    if main_text.count(ui_anchor) != 1:
        raise SystemExit("Unexpected discovery controls; refusing queue search integration")
    main_text = main_text.replace(ui_anchor, ui_block, 1)

queue_anchor = "if(JobStore.uncertain(job))throw new Exception(\"ابتدا مورد نامشخص را در روبیکا بررسی کنید\");"
queue_block = queue_anchor + '''
   if(QueueStartDiscovery.enabled(this)){
    checkpoint(job,"جست‌وجوی خودکار گروه و تماس عمومی پیش از ادامه صف");
    final JSONObject discoveryJob=job;
    try{QueueStartDiscovery.run(this,client,new QueueStartDiscovery.Progress(){public boolean stopped(){return cancel;}public void update(String note)throws Exception{checkpoint(discoveryJob,"جست‌وجوی خودکار: "+note);}});}
    catch(InterruptedException stopped){throw stopped;}
    catch(RubikaClient.ApiError api){if(DiscoveryEngine.rate(api))throw api;checkpoint(job,"جست‌وجو کامل نشد؛ صف ادامه می‌یابد: "+MainActivity.error(api));}
    catch(Exception searchError){if(cancel)throw new InterruptedException("جست‌وجو متوقف شد");checkpoint(job,"جست‌وجو کامل نشد؛ صف ادامه می‌یابد: "+MainActivity.error(searchError));}
    if(cancel)throw new InterruptedException("جست‌وجو متوقف شد");
   }'''
if "QueueStartDiscovery.run(this,client" not in queue_text:
    if queue_text.count(queue_anchor) != 1:
        raise SystemExit("Unexpected QueueService start sequence; refusing queue search integration")
    queue_text = queue_text.replace(queue_anchor, queue_block, 1)

main.write_text(main_text, encoding="utf-8")
queue.write_text(queue_text, encoding="utf-8")
for name in ("QueueStartDiscovery.java", "QueueStartDiscoveryPolicy.java"):
    shutil.copyfile(Path("overrides/ai-discovery") / name, base / name)
print("Installed opt-in, serialized group/contact discovery at queue start")
