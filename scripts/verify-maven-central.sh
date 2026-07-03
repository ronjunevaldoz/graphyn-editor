#!/usr/bin/env bash
# verify-maven-central.sh — confirm every Graphyn artifact actually reached
# repo1.maven.org. This is what makes a green publish job mean "really published":
# from v0.3.0–v0.6.0 the job went green while nothing ever reached the mirror.
#
# Usage:
#   ./scripts/verify-maven-central.sh <version> [timeout-seconds]
#
# Polls each expected POM until all are present (HTTP 200) or the timeout elapses.
# Exits non-zero (failing CI) if any artifact never appears.

# No `set -u`: GitHub's macOS runners ship bash 3.2, where expanding an empty
# array under `set -u` is an "unbound variable" error.
set -eo pipefail

VERSION="${1:?usage: verify-maven-central.sh <version> [timeout-seconds]}"
TIMEOUT="${2:-1800}"   # default 30 minutes
BASE="https://repo1.maven.org/maven2/io/github/ronjunevaldoz"

# Keep in sync with publishedModulePaths in build.gradle.kts and publish.yml.
ARTIFACTS=(
    graphyn-core-model graphyn-core-execution graphyn-core-serialization graphyn-core-data
    graphyn-ui-design graphyn-plugin-api graphyn-ai graphyn-editor-api graphyn-ui-cards
    graphyn-plugin-control graphyn-plugin-list-ops graphyn-plugin-types graphyn-plugin-text
    graphyn-plugin-io graphyn-plugin-json graphyn-plugin-preview
    graphyn-plugin-sticky-notes graphyn-plugin-script
    graphyn-plugin-media-core graphyn-plugin-media-ai graphyn-plugin-stable-diffusion
    graphyn-plugin-gmail graphyn-plugin-linkedin
    graphyn-runtime graphyn-editor graphyn-ktor-plugin
)

echo "Verifying ${#ARTIFACTS[@]} artifacts at version $VERSION on Maven Central (timeout ${TIMEOUT}s)…"

deadline=$(( $(date +%s) + TIMEOUT ))
pending=( "${ARTIFACTS[@]}" )

while (( ${#pending[@]} > 0 )); do
    still=()
    for a in "${pending[@]}"; do
        url="$BASE/$a/$VERSION/$a-$VERSION.pom"
        code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 15 "$url" || echo 000)
        if [[ "$code" == "200" ]]; then
            echo "  ✓ $a"
        else
            still+=( "$a" )
        fi
    done
    pending=( ${still[@]+"${still[@]}"} )

    (( ${#pending[@]} == 0 )) && break

    if (( $(date +%s) >= deadline )); then
        echo "  ✗ Timed out after ${TIMEOUT}s. Still missing from repo1.maven.org: ${pending[*]}"
        echo "    Check deployment state at https://central.sonatype.com (may be stuck in PUBLISHING/FAILED)."
        exit 1
    fi

    echo "  … waiting on ${#pending[@]}: ${pending[*]} (retry in 30s)"
    sleep 30
done

echo "✓ All ${#ARTIFACTS[@]} artifacts are live on Maven Central for $VERSION."
