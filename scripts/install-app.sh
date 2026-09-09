#!/usr/bin/env bash
# Install the debug APK on connected device/emulator. No flags.
# Usage: ./scripts/install-app.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK" ]; then
  echo "APK not found: $APK" >&2
  echo "Build it first: ./gradlew :app:assembleDebug" >&2
  exit 1
fi

if ! "$ADB" devices 2>/dev/null | grep -q "	device$"; then
  echo "No device/emulator connected." >&2
  "$ADB" devices
  exit 1
fi

"$ADB" devices | awk '/	device$/ {print $1}' | while read -r dev; do
  echo "Installing on $dev..."
  "$ADB" -s "$dev" install -r "$APK"
done
