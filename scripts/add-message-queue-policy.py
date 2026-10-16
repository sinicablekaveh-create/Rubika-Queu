#!/usr/bin/env python3
"""Install tested send-preflight and no-duplicate-dispatch policies."""
from pathlib import Path
import shutil

ROOT = Path("app/src/main/java/ir/sinicable/rubikaqueue")

def replace_once(source, before, after, label):
    if source.count(before) != 1:
        raise SystemExit(f"Unexpected {label} source; refusing message-policy overlay")
    return source.replace(before, after, 1)

store_path = ROOT / "JobStore.java"
main_path = ROOT / "MainActivity.java"
store_text = store_path.read_text(encoding="utf-8")
main_text = main_path.read_text(encoding="utf-8")
store_done = "MessageQueuePolicy.mayDispatch" in store_text and "MessageQueuePolicy.requiresReview" in store_text
main_done = "MessageQueuePolicy.requireDraft" in main_text and "MessageQueuePolicy.requireRecipients" in main_text
if store_done and main_done:
    shutil.copyfile("overrides/message-policy/MessageQueuePolicy.java", ROOT / "MessageQueuePolicy.java")
    print("Message preflight and safe-dispatch policy already installed")
    raise SystemExit(0)
if store_done or main_done:
    raise SystemExit("Partial message-policy overlay found; refusing to continue")

store = store_path
s = store_text
s = replace_once(
    s,
    'if(a.getJSONObject(i).optString("status").equals("pending"))return i;',
    'if(MessageQueuePolicy.mayDispatch(a.getJSONObject(i).optString("status")))return i;',
    "JobStore.next",
)
s = replace_once(
    s,
    'String s=r.optString("status");return s.equals("sending")||s.equals("uncertain")||(s.equals("skipped")&&!r.optBoolean("reviewed")&&r.optString("error").contains("نتیجه نامشخص"));',
    'return MessageQueuePolicy.requiresReview(r.optString("status"),r.optBoolean("reviewed"),r.optString("error"));',
    "JobStore.needsReview",
)
main = main_path
t = main_text
t = replace_once(
    t,
    'try{\n   int interval=SendInterval.requireValid(intervalSeconds.getText().toString());\n   JSONObject old=',
    'try{\n   int interval=SendInterval.requireValid(intervalSeconds.getText().toString());\n   JSONObject old=',
    "resume interval preflight",
)
t = replace_once(
    t,
    'String text=message.getText().toString().trim();if(text.isEmpty()){toast("متن پیام را بنویسید");return;}',
    'String text=MessageQueuePolicy.requireDraft(message.getText().toString());',
    "queue draft preflight",
)
t = replace_once(
    t,
    'ExcelReader.Result snapshot=imported;String owner=client.guid;StringBuilder list=new StringBuilder("تعداد: "+snapshot.entries.size()+',
    'ExcelReader.Result snapshot=imported;MessageQueuePolicy.requireRecipients(snapshot.entries.size());String owner=client.guid;StringBuilder list=new StringBuilder("تعداد: "+snapshot.entries.size()+',
    "recipient preview preflight",
)
t = replace_once(
    t,
    'String text=body.getText().toString().trim();if(text.isEmpty()){toast("متن پیام را وارد کنید");return;}int interval=SendInterval.requireValid(intervalSeconds.getText().toString());',
    'String text=MessageQueuePolicy.requireDraft(body.getText().toString());int interval=MessageQueuePolicy.requireInterval(SendInterval.requireValid(intervalSeconds.getText().toString()),SendInterval.MIN_SECONDS,SendInterval.MAX_SECONDS);MessageQueuePolicy.requireRecipients(groups.length());',
    "group message preflight",
)
store.write_text(s, encoding="utf-8")
main.write_text(t, encoding="utf-8")
shutil.copyfile("overrides/message-policy/MessageQueuePolicy.java", ROOT / "MessageQueuePolicy.java")
print("Installed message preflight and safe-dispatch policy")
