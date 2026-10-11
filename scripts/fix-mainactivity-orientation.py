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
source.write_text(fixed, encoding="utf-8")
print("Fixed three existing LinearLayout orientation constants")
