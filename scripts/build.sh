#!/usr/bin/env bash
source "$(dirname "$0")/common.sh"
[[ "$BUILD_VARIANT" == userdebug ]] || fail 'This emulator development target currently supports userdebug only.'
"$AFRIC_ROOT/scripts/integrate.sh"
mkdir -p "$ARTIFACT_DIR"
dialer_args=()
if [[ "${OLAMIDE_ENABLED:-false}" == true ]]; then
    dialer_args=(--enabled --apk "${OLAMIDE_APK:?Olamide APK required}" --sha256 "${OLAMIDE_APK_SHA256:?APK SHA256 required}" --apkanalyzer "${APK_ANALYZER:-apkanalyzer}" --apksigner "${APK_SIGNER:-apksigner}")
fi
python3 "$AFRIC_ROOT/scripts/integrate-olamide.py" --aosp "$AOSP_DIR" --report "$ARTIFACT_DIR/dialer.json" "${dialer_args[@]}"
browser_args=()
if [[ "${AFRICANBROWSER_ENABLED:-true}" == true ]]; then
    browser_args=(--enabled --apk "${AFRICANBROWSER_APK:?AfricanBrowser APK required}" --sha256 "${AFRICANBROWSER_APK_SHA256:?APK SHA256 required}" --apkanalyzer "${APK_ANALYZER:-apkanalyzer}" --apksigner "${APK_SIGNER:-apksigner}")
fi
python3 "$AFRIC_ROOT/scripts/integrate-browser.py" --aosp "$AOSP_DIR" --report "$ARTIFACT_DIR/browser.json" "${browser_args[@]}"
mobile_profile_args=()
if [[ -n "${MOBILE_PROFILE:-}" ]]; then mobile_profile_args=(--profile "$MOBILE_PROFILE"); fi
python3 "$AFRIC_ROOT/scripts/mobile-profile.py" "${mobile_profile_args[@]}" --apply-to "$AOSP_DIR/vendor/africandroid/apps/Customizer" --report "$ARTIFACT_DIR/mobile.json"
python3 "$AFRIC_ROOT/scripts/language-packs.py" --packs "${LANGUAGE_PACK_DIR:-$AFRIC_ROOT/localization/packs}" --required-locales "${REQUIRED_APP_LOCALES:-}" --apply-to "$AOSP_DIR/vendor/africandroid/apps/Customizer" --report "$ARTIFACT_DIR/languages.json"
python3 "$AFRIC_ROOT/scripts/feature-audit.py" --report "$ARTIFACT_DIR/features.json"
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
