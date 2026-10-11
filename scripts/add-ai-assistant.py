#!/usr/bin/env python3
"""Add opt-in assistant after the audited ZIP and the official bot overlays."""
from pathlib import Path
import shutil

base = Path("app/src/main/java/ir/sinicable/rubikaqueue")
main = base / "MainActivity.java"
s = main.read_text(encoding="utf-8")
needle = '  label(p,"فاصله بین ارسال هر پیام (ثانیه)",17,true);\n'
if s.count(needle) != 1:
    raise SystemExit("Unexpected MainActivity source. Refusing unreviewed modification.")
feature = (
    '  button(p,"دستیار AI برای بهبود متن (اختیاری)",()->{Intent ai=new Intent(this,AiAssistantActivity.class);ai.putExtra("draft",message.getText().toString());startActivityForResult(ai,104);});\n'
    '  label(p,"دستیار AI هیچ پیام یا مخاطبی را خودکار ارسال نمی‌کند. متن پیشنهادی را پس از بازبینی خودتان استفاده کنید.",13,false);\n'
)
s = s.replace(needle, feature + needle, 1)
s = s.replace("نسخهٔ آزمایشی و غیررسمی ۱.۱", "نسخهٔ آزمایشی و غیررسمی ۱.۳")
# Handle AI draft return before the existing SAF URI-result handling.
result_anchor = 'super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();'
if s.count(result_anchor) != 1:
    raise SystemExit("Unexpected MainActivity result handling; refusing AI return change")
replacement = (
    'super.onActivityResult(req,result,data);'
    'if(req==104){'
    ' if(result==RESULT_OK&&data!=null){'
    '  String suggestion=data.getStringExtra("ai_proposal");'
    '  if(suggestion!=null && suggestion.trim().length()>0 && suggestion.length()<=4200){'
    '   new AlertDialog.Builder(this).setTitle("جایگزینی با متن پیشنهادی AI")'
    '    .setMessage("آیا متن فعلی پیام با پیشنهاد AI جایگزین شود؟ ارسال صف خودکار انجام نمی‌شود.")'
    '    .setNegativeButton("لغو",null)'
    '    .setPositiveButton("جایگزین کن",(d,w)->message.setText(suggestion)).show();'
    '  }'
    ' }'
    ' return;'
    '}'
    'if(result!=RESULT_OK||data==null||data.getData()==null)return;'
    'Uri uri=data.getData();'
)
s = s.replace(result_anchor, replacement, 1)
main.write_text(s, encoding="utf-8")
for name in ("AiAssistantActivity.java", "AiPromptPolicy.java", "AiOnlineClient.java"):
    shutil.copyfile(Path("overrides/ai-assistant") / name, base / name)
print("Opt-in AI drafting assistant installed without modifying message queue.")
