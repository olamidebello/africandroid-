#!/usr/bin/env bash
set -euo pipefail
AFRIC_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$AFRIC_ROOT/config/build.env"
AOSP_DIR="${AOSP_DIR:-$AFRIC_ROOT/work/aosp}"
ARTIFACT_DIR="${ARTIFACT_DIR:-$AFRIC_ROOT/artifacts}"
fail() { echo "ERROR: $*" >&2; exit 1; }
need() { command -v "$1" >/dev/null || fail "Missing command: $1"; }
