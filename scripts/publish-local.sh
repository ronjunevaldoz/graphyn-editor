#!/usr/bin/env bash
# publish-local.sh — publish Graphyn artifacts to Maven Central using Doppler credentials.
#
# Usage:
#   ./scripts/publish-local.sh              # publish all modules at version from gradle.properties
#   ./scripts/publish-local.sh 0.6.0        # explicit version, all modules
#   ./scripts/publish-local.sh 0.6.0 server # explicit version, single module only
#
# Requirements:
#   - doppler CLI  (brew install dopplerhq/cli/doppler)
#   - DOPPLER_TOKEN in .env or .env.local
#   - Doppler project "maven-central / prd" contains:
#       MAVEN_CENTRAL_USERNAME, MAVEN_CENTRAL_PASSWORD,
#       GPG_SIGNING_KEY, GPG_SIGNING_PASSWORD

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

# ── Load local env (DOPPLER_TOKEN) ────────────────────────────────────────────
for env_file in .env.local .env; do
    if [[ -f "$env_file" ]]; then
        set -a; source "$env_file"; set +a
        echo "🔑  Loaded env from $env_file"
        break
    fi
done

if [[ -z "${DOPPLER_TOKEN:-}" ]]; then
    echo "❌  DOPPLER_TOKEN not set. Add it to .env or .env.local."
    exit 1
fi

# ── Resolve version ────────────────────────────────────────────────────────────
VERSION="${1:-$(grep '^VERSION=' gradle.properties | cut -d= -f2)}"
echo "📦  Publishing version: $VERSION"

# ── Module groups (published in dependency order) ─────────────────────────────
# Named PUBLISH_GROUPS (not GROUPS) to avoid collision with .env variables
declare -a PUBLISH_GROUPS=(
    ":core:model :core:execution :core:serialization :core:data"
    ":core:designsystem"
    ":plugin-api"
    ":ai"
    ":editor-api"
    ":ui:cards"
    ":plugins:control :plugins:list-ops :plugins:types :plugins:text :plugins:io :plugins:json :plugins:preview"
    ":plugins:sticky-notes :plugins:script :plugins:gmail :plugins:linkedin"
    ":plugins:media-core"
    ":plugins:media-ai"
    ":runtime"
    ":app:shared"
    ":server"
)

# If a specific module is requested (e.g. "server"), publish only that
if [[ -n "${2:-}" ]]; then
    PUBLISH_GROUPS=(":${2}")
    echo "🎯  Single module: :${2}"
fi

# ── Publish each group ────────────────────────────────────────────────────────
for group in "${PUBLISH_GROUPS[@]}"; do
    TASKS=""
    for module in $group; do
        TASKS+="${module}:publishAllPublicationsToMavenCentralRepository "
    done

    echo ""
    echo "▶️   $group"
    # Expand TASKS in outer bash before passing to doppler --command
    DOPPLER_TOKEN="$DOPPLER_TOKEN" doppler run \
        --project maven-central --config prd \
        --command "./gradlew $TASKS -PmavenCentralUsername=\$MAVEN_CENTRAL_USERNAME -PmavenCentralPassword=\$MAVEN_CENTRAL_PASSWORD -PsigningInMemoryKey=\$GPG_SIGNING_KEY -PsigningInMemoryKeyPassword=\$GPG_SIGNING_PASSWORD -PVERSION=$VERSION --no-daemon --no-configuration-cache"
done

echo ""
echo "✅  Done — $VERSION submitted to Maven Central."
echo "    Mirror propagation typically takes 15–60 minutes."
echo "    Check: https://repo1.maven.org/maven2/io/github/ronjunevaldoz/"
