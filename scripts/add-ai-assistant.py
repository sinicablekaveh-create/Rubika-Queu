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
    '  button(p,"دستیار AI برای بهبود متن (اختیاری)",()->{Intent ai=new Intent(this,AiAssistantActivity.class);ai.putExtra("draft",message.getText().toString());startActivity(ai);});\n'
    '  label(p,"دستیار AI هیچ پیام یا مخاطبی را خودکار ارسال نمی‌کند. متن پیشنهادی را پس از بازبینی خودتان استفاده کنید.",13,false);\n'
)
s = s.replace(needle, feature + needle, 1)
s = s.replace("نسخهٔ آزمایشی و غیررسمی ۱.۱", "نسخهٔ آزمایشی و غیررسمی ۱.۳")
main.write_text(s, encoding="utf-8")
for name in ("AiAssistantActivity.java", "AiPromptPolicy.java"):
    shutil.copyfile(Path("overrides/ai-assistant") / name, base / name)
print("Opt-in AI drafting assistant installed without modifying message queue.")
