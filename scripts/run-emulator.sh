#!/usr/bin/env bash
# Start the Android emulator for local testing.
# Usage: ./scripts/run-emulator.sh [avd-name] [--ui]
#   Default AVD: mori (headless). Pass --ui to show the emulator window.
set -euo pipefail

AVD="${1:-mori}"
UI_MODE="${2:-}"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"

if ! emulator -list-avds 2>/dev/null | grep -qx "$AVD"; then
  echo "AVD '$AVD' not found. Available:" >&2
  emulator -list-avds >&2
  exit 1
fi

if adb devices 2>/dev/null | grep -q "emulator-"; then
  echo "Emulator already running:"
  adb devices
  exit 0
fi

LOG="$HOME/.android/emulator-${AVD}.log"
ARGS=(-avd "$AVD" -no-snapshot -memory 4096)
if [ "$UI_MODE" != "--ui" ]; then
  ARGS+=(-no-window -no-audio -gpu swiftshader_indirect)
fi

echo "Starting emulator '$AVD' (log: $LOG)..."
# shellcheck disable=SC2086
nohup emulator "${ARGS[@]}" > "$LOG" 2>&1 &
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
