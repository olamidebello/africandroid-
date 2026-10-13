#!/usr/bin/env bash
source "$(dirname "$0")/common.sh"
base="$AOSP_DIR/device/google/cuttlefish/vsoc_x86_64_only/phone/aosp_cf.mk"
[[ -f "$base" ]] || fail 'Expected Cuttlefish product missing. Sync AOSP first.'
dest="$AOSP_DIR/vendor/africandroid"
if [[ -e "$dest" ]]; then
  [[ -f "$dest/.africandroid-managed" ]] || fail 'Refusing to overwrite an unmanaged vendor/africandroid directory.'
fi
mkdir -p "$dest"
cp -a "$AFRIC_ROOT/vendor/africandroid/." "$dest/"
touch "$dest/.africandroid-managed"
echo 'AfricAndroid product and app integrated.'
