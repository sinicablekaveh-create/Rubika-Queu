#!/usr/bin/env python3
"""Install dependency-free plugin core after extracting the audited Android source."""
from pathlib import Path
import shutil

source = Path("overrides/plugin-system")
target = Path("app/src/main/java/ir/sinicable/rubikaqueue")
if not target.is_dir() or not (target / "MainActivity.java").is_file():
    raise SystemExit("Missing extracted Android app source; refusing overlay")

for name in ("CapabilityRegistry.java", "PluginManager.java", "WorkflowEngine.java"):
    original = source / name
    destination = target / name
    if not original.is_file():
        raise SystemExit(f"Missing source for plugin core: {original}")
    if destination.exists() and destination.read_bytes() != original.read_bytes():
        raise SystemExit(f"Refusing to overwrite divergent source: {destination}")
    shutil.copyfile(original, destination)

print("Dependency-free plugin core installed into Android source.")
