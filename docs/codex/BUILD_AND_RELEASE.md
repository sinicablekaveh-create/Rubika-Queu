# Build and Release — V3

## Main Termux build

Run:

    bash build-fixed-apk.sh

The script performs manifest preparation, AAPT2 compilation/linking, Java compilation, D8 dexing, APK assembly, C++17 ARM64 compilation, zip alignment, signing, signature verification, sanitized source packaging, and SHA-256 generation.

Expected outputs:

    /storage/emulated/0/Download/Rubika-Triple-Queue-v3.0.0-CPP-HTTPS.apk
    /storage/emulated/0/Download/Rubika-Triple-Queue-v3.0.0-CPP-HTTPS-SOURCE.zip
    /storage/emulated/0/Download/Rubika-Triple-Queue-v3.0.0-SHA256SUMS.txt

## Required tools

Known working environment:
- Java/Javac
- aapt2
- d8
- zipalign
- apksigner
- clang++
- Python 3
- Git
- GitHub CLI

The build currently expects:

    $HOME/electric-connect-build/toolchain/android.jar

## Signing

Local signing material lives outside the repository under:

    $HOME/.rubika-group-fix/

Never add the keystore or signing password to Git, source ZIPs, issues, or logs.

V2 and V3 locally built APKs use the same certificate.

## APK size

V3 is intentionally far below 10 MB. Do not pad the APK with junk data. Only increase size for real code, resources, dependencies, or additional ABIs.

## Meaning of a successful build

A successful build proves compilation, packaging, native library generation, signing, and signature verification. It does not prove live personal-account login or message delivery.
