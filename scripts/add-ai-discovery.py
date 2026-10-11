#!/usr/bin/env python3
"""Install additive discovery overlay after existing source preparation."""
from pathlib import Path
import shutil
base = Path('app/src/main/java/ir/sinicable/rubikaqueue')
main = base / 'MainActivity.java'
s = main.read_text()
anchor = '  buildDiscovery();'
button = '\n  button(root,"کشف هوشمند گروه‌ها و تاریخچه",()->startActivity(new Intent(this,AiDiscoveryActivity.class)));'
if button not in s:
    if s.count(anchor) != 1:
        raise SystemExit('Unexpected main source; refusing discovery integration')
    main.write_text(s.replace(anchor, anchor + button, 1))
for source in Path('overrides/ai-discovery').glob('*.java'):
    shutil.copyfile(source, base / source.name)
print('Installed independent AI discovery overlay')
