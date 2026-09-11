#!/usr/bin/env bash
# Build a signed release APK for local testing. No flags.
# The APK is signed with the local debug key (NOT for Play distribution).
# Usage: ./scripts/build-release.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [ -d "/opt/homebrew/opt/openjdk@17" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
fi
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

BT="$ANDROID_HOME/build-tools/$(ls "$ANDROID_HOME/build-tools" | sort -V | tail -1)"
OUT="$ROOT/app/build/outputs/apk/release"
UNSIGNED="$OUT/app-release-unsigned.apk"
ALIGNED="$OUT/mori-release.apk"
SIGNED="$OUT/mori-release-signed.apk"

"$ROOT/gradlew" -p "$ROOT" :app:assembleRelease

"$BT/zipalign" -f 4 "$UNSIGNED" "$ALIGNED"
"$BT/apksigner" sign \
  --ks "$HOME/.android/debug.keystore" \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$SIGNED" "$ALIGNED"
"$BT/apksigner" verify "$SIGNED"

echo "Built: $SIGNED"
ls -la "$SIGNED"
