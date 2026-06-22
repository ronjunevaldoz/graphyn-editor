#!/usr/bin/env bash
# Publish Graphyn libraries to Maven Central.
# Credentials are pulled from Doppler (project: graphyn, config: prd).
#
# Usage:
#   DOPPLER_TOKEN=dp.st.xxx ./scripts/publish-maven.sh          # with service token
#   ./scripts/publish-maven.sh 1.0.0                            # specific version (uses doppler login session)
#
# DOPPLER_TOKEN can be a service token (dp.st.*) or personal token (dp.pt.*).
# When set, it bypasses doppler login entirely — safe for CI and local one-off runs.
#
# Required Doppler secrets:
#   ORG_GRADLE_PROJECT_mavenCentralUsername  — Sonatype Central Portal token username
#   ORG_GRADLE_PROJECT_mavenCentralPassword  — Sonatype Central Portal token password
#   ORG_GRADLE_PROJECT_signingKey            — ASCII-armored GPG private key (optional)
#   ORG_GRADLE_PROJECT_signingPassword       — GPG key passphrase (optional, needed if signingKey set)

set -euo pipefail

VERSION="${1:-0.1.0}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

if ! command -v doppler &>/dev/null; then
  echo "Error: Doppler CLI not found. Install: brew install dopplerhq/cli/doppler" >&2
  exit 1
fi

# Build the doppler run command. --token is passed only when DOPPLER_TOKEN is set,
# otherwise the CLI falls back to the logged-in session.
DOPPLER_ARGS=("run")
if [[ -n "${DOPPLER_TOKEN:-}" ]]; then
  echo "Using DOPPLER_TOKEN ($(echo "$DOPPLER_TOKEN" | cut -c1-12)...)."
  DOPPLER_ARGS+=(--token "$DOPPLER_TOKEN")
else
  echo "No DOPPLER_TOKEN set — using doppler login session."
fi
DOPPLER_ARGS+=(--)

echo "Publishing Graphyn v${VERSION} to Maven Central..."

doppler "${DOPPLER_ARGS[@]}" \
  "$ROOT_DIR/gradlew" \
    -p "$ROOT_DIR" \
    publishAllPublicationsToMavenCentralRepository \
    -PVERSION="$VERSION" \
    --no-daemon

echo "Done. Check https://central.sonatype.com/publishing to confirm the upload."
