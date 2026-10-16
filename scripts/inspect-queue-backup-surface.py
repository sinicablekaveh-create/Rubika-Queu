#!/usr/bin/env python3
"""CI-only static code audit for backup UI and source integration; never reads user data."""
from pathlib import Path
root=Path("app/src/main/java/ir/sinicable/rubikaqueue")
sections={
 "MainActivity.java":["class MainActivity","void onCreate","void setup","void build","void import","void launch","void showQueueDiagnostics","void export","void save","void resolveUnknown","void refresh","onActivityResult","getContent","createDocument","ACTION_OPEN_DOCUMENT","class "],
 "JobStore.java":["public static synchronized JSONObject load","public static synchronized void save","public static JSONObject create","public static boolean uncertain","public static int next","public static void recordSuccess"],
 "Vault.java":["public static","static RubikaClient load"],
}
for name,finds in sections.items():
 lines=(root/name).read_text(encoding="utf-8").splitlines()
 indices=[i for i,l in enumerate(lines) if any(s in l for s in finds)]
 windows=[]
 for i in indices:windows.append((max(0,i-3),min(len(lines),i+6)))
 merged=[]
 for a,b in windows:
  if merged and a<=merged[-1][1]:merged[-1]=(merged[-1][0],max(b,merged[-1][1]))
  else:merged.append((a,b))
 print("=== "+name+" ("+str(len(lines))+" lines) ===")
 for a,b in merged:
  for i in range(a,b):print(str(i+1).rjust(4)+" "+lines[i])
  print("...")
