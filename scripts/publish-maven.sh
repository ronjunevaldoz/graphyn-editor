#!/usr/bin/env bash
# Publish Graphyn libraries to Maven Central.
# Credentials are pulled from Doppler (project: graphyn, config: prd).
#
# Usage:
#   ./scripts/publish-maven.sh              # publishes 0.1.0
#   ./scripts/publish-maven.sh 1.0.0        # publishes a specific version
#
# Required Doppler secrets:
#   ORG_GRADLE_PROJECT_mavenCentralUsername  — Sonatype Central Portal token username
#   ORG_GRADLE_PROJECT_mavenCentralPassword  — Sonatype Central Portal token password
#   ORG_GRADLE_PROJECT_signingKey            — ASCII-armored GPG private key (optional)
#   ORG_GRADLE_PROJECT_signingPassword       — GPG key passphrase (optional, needed if signingKey set)
#
# Gradle automatically maps ORG_GRADLE_PROJECT_* env vars to project properties,
# so no -P flags are needed when running via `doppler run`.

set -euo pipefail

VERSION="${1:-0.1.0}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

if ! command -v doppler &>/dev/null; then
  echo "Error: Doppler CLI not found. Install: brew install dopplerhq/cli/doppler" >&2
  exit 1
fi

echo "Publishing Graphyn v${VERSION} to Maven Central..."
echo "Pulling credentials from Doppler ($(doppler configure get project --plain 2>/dev/null || echo 'graphyn')/$(doppler configure get config --plain 2>/dev/null || echo 'prd'))..."

doppler run -- \
  "$ROOT_DIR/gradlew" \
    -p "$ROOT_DIR" \
    publishAllPublicationsToMavenCentralRepository \
    -PVERSION="$VERSION" \
    --no-daemon

echo "Done. Check https://central.sonatype.com/publishing to confirm the upload."
