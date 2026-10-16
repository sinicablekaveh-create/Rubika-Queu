#!/usr/bin/env python3
"""Install a fail-closed same-queue duplicate destination check before dispatch."""
from pathlib import Path
import shutil

base=Path("app/src/main/java/ir/sinicable/rubikaqueue")
service_path=base/"QueueService.java"
source=service_path.read_text(encoding="utf-8")
marker="QueueDuplicateGuard.blocksRepeat(previous.optString"
if marker in source:
    if source.count(marker)!=1:
        raise SystemExit("Ambiguous existing duplicate guard")
    print("Duplicate destination guard already installed")
    raise SystemExit(0)
anchor='     current=job.getJSONArray("recipients").getJSONObject(n);dispatch=false;'
if source.count(anchor)!=1:
    raise SystemExit("Unexpected dispatch boundary; refusing to modify sender")
# The predecessor was already processed or has an unknown outcome; never resend.
guard='''     current=job.getJSONArray("recipients").getJSONObject(n);dispatch=false;
     String destinationKey=QueueDuplicateGuard.destinationKey(
       current.optString("kind"),current.optString("phone"),current.optString("guid"));
     if(!destinationKey.isEmpty()){
      boolean duplicateOfEarlierAttempt=false;
      JSONArray queueRows=job.getJSONArray("recipients");
      for(int previousIndex=0;previousIndex<n;previousIndex++){
       JSONObject previous=queueRows.getJSONObject(previousIndex);
       if(QueueDuplicateGuard.blocksRepeat(previous.optString("status")) &&
          destinationKey.equals(QueueDuplicateGuard.destinationKey(
            previous.optString("kind"),previous.optString("phone"),previous.optString("guid")))){
        duplicateOfEarlierAttempt=true;break;
       }
      }
      if(duplicateOfEarlierAttempt){
       current.put("status","skipped");
       current.put("error","مقصد تکراری در همین صف؛ ارسال دوم انجام نشد");
       checkpoint(job,"مورد "+(n+1)+" مقصد تکراری بود؛ ارسال نشد");
       current=null;continue;
      }
     }'''
new=source.replace(anchor,guard,1)
# No APIs, account keys or network calls are added or changed.
if new.count("client.send(")!=source.count("client.send("):
    raise SystemExit("Attempted to change number of sending calls")
if not (new.index("QueueDuplicateGuard.blocksRepeat(")<new.index("client.send(")):
    raise SystemExit("Duplicate guard must run before network dispatch")
sourceFile=Path("overrides/queue-safety/QueueDuplicateGuard.java")
if not sourceFile.exists():
    raise SystemExit("Missing pure Java duplicate guard")
shutil.copyfile(sourceFile,base/sourceFile.name)
service_path.write_text(new,encoding="utf-8")
print("Installed fail-closed same-queue duplicate check before recipient lookup and send")
