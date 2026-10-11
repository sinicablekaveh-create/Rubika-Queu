#!/usr/bin/env python3
"""Fix three legacy numeric LinearLayout orientation constants after ZIP extraction.

This is a guarded mechanical lint fix, not an architecture or messaging change.
"""
from pathlib import Path

source = Path("app/src/main/java/ir/sinicable/rubikaqueue/MainActivity.java")
old = source.read_text(encoding="utf-8")
numeric = ".setOrientation(1)"
if old.count(numeric) != 3:
    raise SystemExit("Unexpected MainActivity source; refusing unreviewed rewrite")
fixed = old.replace(numeric, ".setOrientation(LinearLayout.VERTICAL)")
# SAF: persist only the URI grant flags actually returned by document picker.
unsafe = '  try{getContentResolver().takePersistableUriPermission(uri,result.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(SecurityException ignored){}'
safe = """  int grants=result.getFlags();
  if ((grants & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
   try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (SecurityException ignored) {}
  }
  if ((grants & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0) {
   try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION); } catch (SecurityException ignored) {}
  }"""
if fixed.count(unsafe) != 1:
    raise SystemExit("Unexpected research URI permissions source; refusing rewrite")
fixed = fixed.replace(unsafe, safe)
source.write_text(fixed, encoding="utf-8")
print("Fixed legacy LinearLayout constants and document URI grant handling")
