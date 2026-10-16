#!/usr/bin/env python3
"""Read static (non-user) queue source to locate crash/retry transitions in CI."""
from pathlib import Path
import re
from zipfile import ZipFile

source=Path("source/Rubika-Queu-v1.1-personal-queue-source.zip")
prefix="app/src/main/java/ir/sinicable/rubikaqueue/"
patterns={
    "QueueService.java": ["client.send(", '"sending"', '"uncertain"', "checkpoint(", "JobStore.load", "JobStore.save"],
    "JobStore.java": ['"sending"', '"uncertain"', "JSONObject load(", "void save(", ".apply()", ".commit()", "SharedPreferences"],
}
with ZipFile(source) as z:
    for name, needles in patterns.items():
        text=z.read(prefix+name).decode("utf-8")
        lines=text.splitlines()
        ranges=[]
        for i,line in enumerate(lines):
            if any(word in line for word in needles):
                ranges.append((max(0,i-7),min(len(lines),i+8)))
        ranges.sort()
        merged=[]
        for start,end in ranges:
            if merged and start<=merged[-1][1]:
                merged[-1]=(merged[-1][0],max(end,merged[-1][1]))
            else:
                merged.append((start,end))
        print(f"=== {name}: {len(lines)} lines, selected non-user code snippets ===")
        for a,b in merged:
            print("...")
            for i in range(a,b):
                print(f"{i+1:4} {lines[i]}")
