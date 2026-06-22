#!/usr/bin/env bash
# Publish Graphyn libraries to Maven Central, tag the release, post GitHub release notes,
# then auto-bump the patch version in gradle.properties ready for the next publish.
#
# Usage:
#   ./scripts/publish-maven.sh              # reads VERSION from gradle.properties
#   ./scripts/publish-maven.sh --dry-run    # preview changelog only, no publish/tag/bump
#
# For a milestone bump (minor/major), edit gradle.properties before running:
#   VERSION=0.3.0
#
# DOPPLER_TOKEN: set in .env or the environment (dp.st.* service / dp.pt.* personal token).
#
# Required Doppler secrets:
#   ORG_GRADLE_PROJECT_mavenCentralUsername  — Sonatype Central Portal token username
#   ORG_GRADLE_PROJECT_mavenCentralPassword  — Sonatype Central Portal token password
#   ORG_GRADLE_PROJECT_signingKey            — ASCII-armored GPG private key (optional)
#   ORG_GRADLE_PROJECT_signingPassword       — GPG key passphrase (optional)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
PROPS="$ROOT_DIR/gradle.properties"

DRY_RUN=false
[[ "${1:-}" == "--dry-run" ]] && DRY_RUN=true

# ── read VERSION from gradle.properties ───────────────────────────────────────
VERSION="$(grep -E '^VERSION=' "$PROPS" | cut -d= -f2 | tr -d '[:space:]')"
if [[ -z "$VERSION" ]]; then
  echo "Error: VERSION not found in gradle.properties" >&2; exit 1
fi
TAG="v${VERSION}"

# ── helpers ───────────────────────────────────────────────────────────────────
check_tool() { command -v "$1" &>/dev/null || { echo "Error: $1 not found. $2" >&2; exit 1; }; }

bump_patch() {
  local ver="$1"
  local major minor patch
  IFS='.' read -r major minor patch <<< "$ver"
  echo "${major}.${minor}.$((patch + 1))"
}

# ── pre-flight ────────────────────────────────────────────────────────────────
check_tool doppler   "Install: brew install dopplerhq/cli/doppler"
check_tool git-cliff "Install: brew install git-cliff"
check_tool gh        "Install: brew install gh"

if [[ -z "${DOPPLER_TOKEN:-}" && -f "$ROOT_DIR/.env" ]]; then
  # shellcheck source=/dev/null
  set -o allexport; source "$ROOT_DIR/.env"; set +o allexport
fi

DOPPLER_ARGS=("run")
if [[ -n "${DOPPLER_TOKEN:-}" ]]; then
  echo "Using DOPPLER_TOKEN ($(echo "$DOPPLER_TOKEN" | cut -c1-12)...)."
  DOPPLER_ARGS+=(--token "$DOPPLER_TOKEN")
else
  echo "No DOPPLER_TOKEN — using doppler login session."
fi
DOPPLER_ARGS+=(--)

# ── changelog ─────────────────────────────────────────────────────────────────
echo "Generating release notes for ${TAG}..."
RELEASE_NOTES="$(git-cliff --tag "$TAG" --unreleased --strip all 2>/dev/null)"

if [[ -z "$RELEASE_NOTES" ]]; then
  echo "No unreleased commits found since last tag." >&2; exit 1
fi

echo "$RELEASE_NOTES"
echo ""

if $DRY_RUN; then
  echo "── Dry run — skipping tag, publish, and bump. ──"
  exit 0
fi

# ── update CHANGELOG.md ───────────────────────────────────────────────────────
echo "Updating CHANGELOG.md..."
git-cliff --tag "$TAG" --output "$ROOT_DIR/CHANGELOG.md"

# ── git tag ───────────────────────────────────────────────────────────────────
echo "Tagging ${TAG}..."
git -C "$ROOT_DIR" add CHANGELOG.md
git -C "$ROOT_DIR" commit -m "chore(release): ${TAG}" || true
git -C "$ROOT_DIR" tag -a "$TAG" -m "Release ${TAG}"
git -C "$ROOT_DIR" push origin main --tags

# ── maven publish ─────────────────────────────────────────────────────────────
echo "Publishing Graphyn ${VERSION} to Maven Central..."
doppler "${DOPPLER_ARGS[@]}" \
  "$ROOT_DIR/gradlew" \
    -p "$ROOT_DIR" \
    publishAllPublicationsToMavenCentralRepository \
    -PVERSION="$VERSION" \
    --no-daemon

# ── github release ────────────────────────────────────────────────────────────
echo "Creating GitHub release ${TAG}..."
gh release create "$TAG" \
  --repo ronjunevaldoz/graphyn-editor \
  --title "Graphyn ${VERSION}" \
  --notes "$RELEASE_NOTES"

# ── auto-bump patch ───────────────────────────────────────────────────────────
NEXT_VERSION="$(bump_patch "$VERSION")"
echo "Bumping VERSION: ${VERSION} → ${NEXT_VERSION}..."
sed -i '' "s/^VERSION=.*/VERSION=${NEXT_VERSION}/" "$PROPS"
# sync fallbacks in all publishable modules
for f in \
  "$ROOT_DIR/core/build.gradle.kts" \
  "$ROOT_DIR/plugin-api/build.gradle.kts" \
  "$ROOT_DIR/editor-api/build.gradle.kts" \
  "$ROOT_DIR/app/shared/build.gradle.kts" \
  "$ROOT_DIR/ui/cards/build.gradle.kts"; do
  sed -i '' "s|?: \"${VERSION}\"|?: \"${NEXT_VERSION}\"|g" "$f"
done
git -C "$ROOT_DIR" add gradle.properties \
  core/build.gradle.kts plugin-api/build.gradle.kts editor-api/build.gradle.kts \
  app/shared/build.gradle.kts ui/cards/build.gradle.kts
git -C "$ROOT_DIR" commit -m "chore: bump version to ${NEXT_VERSION}"
git -C "$ROOT_DIR" push origin main

echo ""
echo "✓ ${TAG} published. Next version is ${NEXT_VERSION}."
echo "  Maven Central: https://central.sonatype.com/publishing"
echo "  GitHub:        https://github.com/ronjunevaldoz/graphyn-editor/releases/tag/${TAG}"
