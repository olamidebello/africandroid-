#!/usr/bin/env bash
source "$(dirname "$0")/common.sh"
[[ -r /dev/kvm && -w /dev/kvm ]] || fail 'KVM unavailable. Enable virtualization and configure Cuttlefish host packages/groups.'
cd "$AOSP_DIR"
set +u
source build/envsetup.sh
lunch "africandroid_cf_x86_64_phone-$RELEASE_CONFIG-$BUILD_VARIANT"
product_out="$(get_build_var PRODUCT_OUT)"
host_out="$(get_build_var HOST_OUT)"
set -u
[[ -x "$host_out/bin/launch_cvd" ]] || fail 'Build the Cuttlefish host tools first.'
"$host_out/bin/launch_cvd" --system_image_dir="$product_out" --daemon "$@"
echo 'Open https://localhost:8443; use an SSH tunnel when running remotely.'
