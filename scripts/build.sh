#!/usr/bin/env bash
source "$(dirname "$0")/common.sh"
[[ "$BUILD_VARIANT" == userdebug ]] || fail 'This emulator development target currently supports userdebug only.'
"$AFRIC_ROOT/scripts/integrate.sh"
cd "$AOSP_DIR"
# AOSP environment scripts are not compatible with nounset.
set +u
source build/envsetup.sh
lunch "africandroid_cf_x86_64_phone-$RELEASE_CONFIG-$BUILD_VARIANT"
m -j"$BUILD_JOBS" dist
set -u
mkdir -p "$ARTIFACT_DIR/dist"
cp -a "${OUT_DIR:-out}/dist/." "$ARTIFACT_DIR/dist/"
repo manifest -r -o "$ARTIFACT_DIR/aosp-pinned-manifest.xml"
git -C "$AFRIC_ROOT" rev-parse HEAD > "$ARTIFACT_DIR/africandroid-source-commit.txt"
python3 "$AFRIC_ROOT/scripts/checksums.py" "$ARTIFACT_DIR"
echo "Build artifacts: $ARTIFACT_DIR"
