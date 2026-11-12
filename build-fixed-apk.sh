#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/.local-build"
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
            '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="ir.sinicable.rubikaqueue" android:versionCode="34" android:versionName="1.6.3-queue-deep-reliability">',1)
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
zipalign -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
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
DEST="/storage/emulated/0/Download/Rubika-Triple-Queue-v1.6.3-ORIGINAL-ID-DEBUG.apk"
echo SIGN_APK
apksigner sign --ks "$KEY" --ks-key-alias rubikafix \
    --ks-pass "file:$PASS" --out "$DEST" "$OUT/aligned.apk"
apksigner verify --verbose "$DEST"
echo ARCHIVE_SOURCE
cd "$ROOT"
zip -q -r /storage/emulated/0/Download/Rubika-Triple-Queue-v1.6.3-ORIGINAL-ID-SOURCE.zip \
  app scripts overrides patches tests build-fixed-apk.sh README-FIXED-FA.md
echo VERIFY_FILES
ls -lh "$DEST" /storage/emulated/0/Download/Rubika-Triple-Queue-v1.6.3-ORIGINAL-ID-SOURCE.zip
sha256sum "$DEST"
echo BUILD_COMPLETE
