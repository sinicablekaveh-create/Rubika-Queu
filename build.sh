#!/usr/bin/env bash
set -e

if [ -f "./gradlew" ]; then
  chmod +x ./gradlew
  ./gradlew --no-daemon --max-workers=4 assembleDebug
  exit 0
fi

echo "Gradle wrapper not found. Add Android project sources before APK build."
exit 1
