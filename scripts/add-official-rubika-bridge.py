#!/usr/bin/env python3
"""Install the explicit Rubika 4.1.1 handoff into the extracted app source."""

from pathlib import Path
import re


ROOT = Path("app/src/main")
MAIN = ROOT / "java/ir/sinicable/rubikaqueue/MainActivity.java"
MANIFEST = ROOT / "AndroidManifest.xml"
BRIDGE_SOURCE = Path("overrides/official-rubika-4.1.1/OfficialRubikaBridge.java")
BRIDGE_TARGET = ROOT / "java/ir/sinicable/rubikaqueue/OfficialRubikaBridge.java"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one anchor, found {count}")
    return text.replace(old, new, 1)


def main() -> None:
    for path in (MAIN, MANIFEST, BRIDGE_SOURCE):
        if not path.is_file():
            raise SystemExit(f"missing bridge input: {path}")

    BRIDGE_TARGET.parent.mkdir(parents=True, exist_ok=True)
    bridge = BRIDGE_SOURCE.read_text(encoding="utf-8")
    if BRIDGE_TARGET.exists() and BRIDGE_TARGET.read_text(encoding="utf-8") != bridge:
        raise SystemExit(f"refusing to overwrite changed generated file: {BRIDGE_TARGET}")
    BRIDGE_TARGET.write_text(bridge, encoding="utf-8")

    manifest = MANIFEST.read_text(encoding="utf-8")
    if "app.rbmain.a" not in manifest:
        manifest, count = re.subn(
            r"(<manifest\b[^>]*>)",
            r'\1\n <queries>\n  <package android:name="app.rbmain.a"/>\n </queries>',
            manifest,
            count=1,
        )
        if count != 1:
            raise SystemExit("AndroidManifest.xml: manifest root not found")
        MANIFEST.write_text(manifest, encoding="utf-8")

    main_activity = MAIN.read_text(encoding="utf-8")
    launch_anchor = (
        '  button(accountPanel,"وب‌سایت رسمی و API بات روبیکا",'
        '()->startActivity(new Intent(this,OfficialBotActivity.class)));'
    )
    launch_button = (
        launch_anchor
        + '\n  button(accountPanel,"باز کردن روبیکای رسمی ۴.۱.۱",()->{'
          'if(!OfficialRubikaBridge.open(this))'
          'toast("روبیکای رسمی ۴.۱.۱ روی این گوشی نصب نیست");});'
    )
    if "باز کردن روبیکای رسمی ۴.۱.۱" not in main_activity:
        main_activity = replace_once(
            main_activity, launch_anchor, launch_button,
            "MainActivity official launcher anchor",
        )

    draft_anchor = (
        '  message.setFilters(new android.text.InputFilter[]{'
        'new android.text.InputFilter.LengthFilter(4200)});'
    )
    draft_button = (
        draft_anchor
        + '\n  button(p,"اشتراک متن فعلی با روبیکای رسمی ۴.۱.۱",()->{'
          'String draft=message.getText().toString().trim();'
          'if(draft.isEmpty()){toast("ابتدا متن پیام را بنویسید");return;}'
          'if(!OfficialRubikaBridge.shareText(this,draft))'
          'toast("روبیکای رسمی ۴.۱.۱ نصب نیست یا اشتراک متن را نمی‌پذیرد");});'
    )
    if "اشتراک متن فعلی با روبیکای رسمی ۴.۱.۱" not in main_activity:
        main_activity = replace_once(
            main_activity, draft_anchor, draft_button,
            "MainActivity official share anchor",
        )
    MAIN.write_text(main_activity, encoding="utf-8")


if __name__ == "__main__":
    main()
