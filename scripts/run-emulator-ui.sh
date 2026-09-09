#!/usr/bin/env bash
# Launch the Android emulator with GUI window. No flags.
# Usage: ./scripts/run-emulator-ui.sh
set -euo pipefail

AVD="mori"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"

if adb devices 2>/dev/null | grep -q "emulator-"; then
  echo "Emulator already running. Run ./scripts/kill-emulator.sh first."
  adb devices
  exit 0
fi

LOG="$HOME/.android/emulator-${AVD}.log"
echo "Starting emulator '$AVD' with UI (log: $LOG)..."
nohup emulator -avd "$AVD" -no-snapshot -memory 4096 > "$LOG" 2>&1 &

echo "Waiting for boot..."
adb wait-for-device
for _ in $(seq 1 60); do
  if [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
    echo "Booted:"
    adb devices
    exit 0
  fi
  sleep 5
done
echo "Timed out waiting for boot. See $LOG" >&2
exit 1
