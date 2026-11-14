#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/.local-build"
rm -rf "$OUT"
SDK="$HOME/electric-connect-build/toolchain/android.jar"
mkdir -p "$OUT"/{gen,classes,dex}
cd "$ROOT"
python3 -m py_compile scripts/remove-groups-fix-invalid-input.py
echo PREPARE_MANIFEST
python3 - "$ROOT/app/src/main/AndroidManifest.xml" "$OUT/AndroidManifest.xml" <<'PY'
from pathlib import Path
import sys
s=Path(sys.argv[1]).read_text(encoding='utf-8')
s=s.replace('<manifest xmlns:android="http://schemas.android.com/apk/res/android">',
            '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="ir.sinicable.rubikaqueue.cpp180" android:versionCode="40" android:versionName="2.0.0">',1)
s=s.replace('android:name=".', 'android:name="ir.sinicable.rubikaqueue.')
s=s.replace('صف پیام روبیکا • آزمایشی','سه صف روبیکا')
Path(sys.argv[2]).write_text(s,encoding='utf-8')
PY
echo COMPILE_RES
aapt2 compile --dir app/src/main/res -o "$OUT/res.zip"
echo PACKAGE_RES
aapt2 link -o "$OUT/unsigned.apk" --manifest "$OUT/AndroidManifest.xml" \
  -I "$SDK" --java "$OUT/gen" --custom-package ir.sinicable.rubikaqueue \
  --min-sdk-version 26 --target-sdk-version 34 "$OUT/res.zip"
echo COMPILE_JAVA
find app/src/main/java "$OUT/gen" -name '*.java' > "$OUT/java-files.txt"
javac -source 8 -target 8 -encoding UTF-8 -classpath "$SDK" \
  -d "$OUT/classes" @"$OUT/java-files.txt"
echo COMPILE_DEX
find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
d8 --min-api 26 --lib "$SDK" --output "$OUT/dex" @"$OUT/classes.txt"
echo ASSEMBLE_APK
(cd "$OUT/dex" && zip -q -j "$OUT/unsigned.apk" classes*.dex)
echo BUILD_NATIVE_CPP
mkdir -p "$OUT/native/lib/arm64-v8a"
clang++ -std=c++17 -O2 -fPIC -fno-exceptions -fno-rtti -nostdlib++ -shared -Wl,-z,max-page-size=16384 -I"$PREFIX/include" app/src/main/cpp/native_send_core.cpp -o "$OUT/native/lib/arm64-v8a/librubikasendcore.so"
(cd "$OUT/native" && zip -q -r "$OUT/unsigned.apk" lib)
zipalign -f -P 16 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
KEYDIR="$HOME/.rubika-group-fix"
mkdir -p "$KEYDIR"
chmod 700 "$KEYDIR"
KEY="$KEYDIR/debug-sidecar.p12"
PASS="$KEYDIR/pass.txt"
if [ ! -s "$PASS" ]; then python3 -c 'import secrets;print(secrets.token_hex(20))' > "$PASS"; chmod 600 "$PASS"; fi
if [ ! -s "$KEY" ]; then
  KEYPASS="$(cat "$PASS")"
  keytool -genkeypair -noprompt -keystore "$KEY" -storetype PKCS12 \
    -storepass "$KEYPASS" -keypass "$KEYPASS" -alias rubikafix \
    -dname "CN=Rubika Queue Fix, OU=Android, O=Sinicable Kaveh, C=IR" \
    -keyalg RSA -keysize 3072 -validity 3650 >/dev/null
fi
DEST="/storage/emulated/0/Download/Rubika-Triple-Queue-v2.0.0-FINAL-CPP-DEBUG.apk"
echo SIGN_APK
apksigner sign --ks "$KEY" --ks-key-alias rubikafix \
    --ks-pass "file:$PASS" --out "$DEST" "$OUT/aligned.apk"
apksigner verify --verbose "$DEST"
echo ARCHIVE_SOURCE
python3 scripts/package_v2_release.py "$ROOT" "$DEST"   /storage/emulated/0/Download/Rubika-Triple-Queue-v2.0.0-FINAL-CPP-SOURCE.zip
echo VERIFY_FILES
ls -lh "$DEST" /storage/emulated/0/Download/Rubika-Triple-Queue-v2.0.0-FINAL-CPP-SOURCE.zip
sha256sum "$DEST"
echo BUILD_COMPLETE
