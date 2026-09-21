#!/usr/bin/env bash
# Cut a release by tagging; .github/workflows/release.yml builds, signs, and
# publishes the GitHub Release from the tag. Tags without alpha/beta/rc
# publish as production releases (prerelease: false).
#
# Version line is 0.x: every auto release bumps the patch (0.1.0 -> 0.1.1).
# After the v1.x reset there are no stable tags, so bootstrap explicitly:
#   ./scripts/new-release.sh 0.1.0   # first release on the fresh line
# Usage once 0.x tags exist:
#   ./scripts/new-release.sh         # next patch (highest vX.Y.Z tag + .0.1)
#   ./scripts/new-release.sh 0.2.0   # explicit version
#   ./scripts/new-release.sh 0.1.23 hotfix   # moniker appended to tag message
set -euo pipefail

MONIKER="${2:-}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [ $# -ge 1 ]; then
  VERSION="$1"
else
  LATEST="$(git tag --list | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | sort -V | tail -n 1 || true)"
  if [ -z "$LATEST" ]; then
    echo "No stable vX.Y.Z tags found; pass an explicit version." >&2
    exit 1
  fi
  BASE="${LATEST#v}"
  MAJOR="${BASE%%.*}"
  REST="${BASE#*.}"
  MINOR="${REST%%.*}"
  PATCH="${REST##*.}"
  VERSION="$MAJOR.$MINOR.$((PATCH + 1))"
fi

if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Invalid version '$VERSION'; expected X.Y.Z." >&2
  exit 1
fi
TAG="v$VERSION"

if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "Working tree has uncommitted changes; commit or stash first." >&2
  exit 1
fi
if git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "Tag $TAG already exists." >&2
  exit 1
fi

git fetch --tags origin >/dev/null 2>&1 || true
if git ls-remote --tags origin | grep -q "refs/tags/$TAG$"; then
  echo "Tag $TAG already exists on origin." >&2
  exit 1
fi

echo "Verifying release build for $TAG..."
if [ -d "/opt/homebrew/opt/openjdk@17" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
fi
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
"$ROOT/gradlew" -p "$ROOT" :app:assembleRelease -PappVersionName="$TAG" >/dev/null
echo "Release build verified."

git tag -a "$TAG" -m "Mori $TAG${MONIKER:+ ($MONIKER)}"
git push origin "$TAG"

echo "Pushed $TAG."
echo "CI now builds, signs, and publishes the production GitHub Release:"
REPO="$(git remote get-url origin | sed -E 's#.*[:/]([^/]+/[^/.]+)(\.git)?$#\1#')"
echo "  https://github.com/$REPO/releases/tag/$TAG"
