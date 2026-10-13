#!/usr/bin/env bash
source "$(dirname "$0")/common.sh"
"$AFRIC_ROOT/scripts/preflight.sh"
cd "$AOSP_DIR"
repo init --partial-clone --no-use-superproject -u https://android.googlesource.com/platform/manifest -b "$AOSP_BRANCH"
repo sync -c -j"$SYNC_JOBS" --fail-fast
mkdir -p "$ARTIFACT_DIR"
repo manifest -r -o "$ARTIFACT_DIR/aosp-pinned-manifest.xml"
"$AFRIC_ROOT/scripts/integrate.sh"
