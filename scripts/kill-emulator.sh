#!/usr/bin/env bash
# Stop any running Android emulators.
# Usage: ./scripts/kill-emulator.sh
set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

if ! adb devices 2>/dev/null | grep -q "emulator-"; then
  echo "No emulator running."
  pkill -f "qemu-system.*-avd" 2>/dev/null || true
  exit 0
fi

adb devices | awk '/emulator-/ {print $1}' | while read -r dev; do
  echo "Killing $dev..."
  adb -s "$dev" emu kill || true
done
sleep 2
pkill -f "qemu-system.*-avd" 2>/dev/null || true
echo "Done:"
adb devices
