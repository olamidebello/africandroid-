#!/usr/bin/env bash
source "$(dirname "$0")/common.sh"
need adb
[[ $# == 1 ]] || fail 'Usage: scripts/smoke-test.sh ADB_SERIAL'
adb_cmd=(adb -s "$1")
deadline=$((SECONDS + 180))
until [[ "$("${adb_cmd[@]}" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; do
  (( SECONDS < deadline )) || fail 'Device did not finish booting within 180 seconds.'
  sleep 2
done
[[ "$("${adb_cmd[@]}" shell getprop ro.product.brand | tr -d '\r')" == AfricAndroid ]] || fail 'Brand mismatch.'
"${adb_cmd[@]}" shell pm path org.africandroid.customizer | grep -q '^package:' || fail 'Customizer missing.'
"${adb_cmd[@]}" shell am start -W -n org.africandroid.customizer/.MainActivity | tee /tmp/africandroid-activity-result.txt
grep -q 'Status: ok' /tmp/africandroid-activity-result.txt || fail 'Customizer could not launch.'
[[ "$("${adb_cmd[@]}" shell getenforce | tr -d '\r')" == Enforcing ]] || fail 'SELinux is not enforcing.'
echo 'Boot, branding, app launch, and SELinux checks passed; full compatibility testing is still required.'
