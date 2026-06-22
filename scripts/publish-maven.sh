#!/usr/bin/env bash
# Publish Graphyn libraries to Maven Central, tag the release, and post GitHub release notes.
# Credentials are pulled from Doppler (project: graphyn, config: prd).
#
# Usage:
#   ./scripts/publish-maven.sh 0.1.0          # tag + changelog + publish + GitHub release
#   ./scripts/publish-maven.sh 0.1.0 --dry-run  # preview changelog only, no publish/tag
#
# DOPPLER_TOKEN can be set in .env or the environment:
#   service token (dp.st.*) — recommended for scripting
#   personal token (dp.pt.*) — local use
#
# Required Doppler secrets:
#   ORG_GRADLE_PROJECT_mavenCentralUsername  — Sonatype Central Portal token username
#   ORG_GRADLE_PROJECT_mavenCentralPassword  — Sonatype Central Portal token password
#   ORG_GRADLE_PROJECT_signingKey            — ASCII-armored GPG private key (optional)
#   ORG_GRADLE_PROJECT_signingPassword       — GPG key passphrase (optional)

set -euo pipefail

VERSION="${1:-0.1.0}"
DRY_RUN=false
[[ "${2:-}" == "--dry-run" ]] && DRY_RUN=true

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
TAG="v${VERSION}"

# ── helpers ──────────────────────────────────────────────────────────────────
check_tool() { command -v "$1" &>/dev/null || { echo "Error: $1 not found. $2" >&2; exit 1; }; }

# ── pre-flight ────────────────────────────────────────────────────────────────
check_tool doppler  "Install: brew install dopplerhq/cli/doppler"
check_tool git-cliff "Install: brew install git-cliff"
check_tool gh       "Install: brew install gh"

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

# ── changelog ────────────────────────────────────────────────────────────────
echo "Generating release notes for ${TAG}..."
RELEASE_NOTES="$(git-cliff --tag "$TAG" --unreleased --strip all 2>/dev/null)"

if [[ -z "$RELEASE_NOTES" ]]; then
  echo "No unreleased commits found since last tag." >&2
  exit 1
fi

echo "$RELEASE_NOTES"
echo ""

if $DRY_RUN; then
  echo "── Dry run — skipping tag, publish, and GitHub release. ──"
  exit 0
fi

# ── update CHANGELOG.md ──────────────────────────────────────────────────────
echo "Updating CHANGELOG.md..."
git-cliff --tag "$TAG" --output "$ROOT_DIR/CHANGELOG.md"

# ── git tag ──────────────────────────────────────────────────────────────────
echo "Tagging ${TAG}..."
git -C "$ROOT_DIR" add CHANGELOG.md
git -C "$ROOT_DIR" commit -m "chore(release): ${TAG}" || true  # no-op if nothing changed
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

echo ""
echo "✓ ${TAG} published."
echo "  Maven Central: https://central.sonatype.com/publishing"
echo "  GitHub:        https://github.com/ronjunevaldoz/graphyn-editor/releases/tag/${TAG}"
