#!/usr/bin/env bash
source "$(dirname "$0")/common.sh"
[[ $(uname -s) == Linux && $(uname -m) == x86_64 ]] || fail 'Use x86_64 Linux for this target.'
for tool in git repo python3 make zip unzip curl; do need "$tool"; done
mkdir -p "$AOSP_DIR"
free_kb=$(df -Pk "$AOSP_DIR" | awk 'END {print $4}')
ram_kb=$(awk '/MemTotal/ {print $2}' /proc/meminfo)
(( free_kb >= 400 * 1024 * 1024 )) || fail 'At least 400 GiB free storage required.'
(( ram_kb >= 60 * 1024 * 1024 )) || fail 'Use a build host with at least 64 GB RAM.'
printf 'Build preflight passed. KVM is additionally required to run Cuttlefish.
'
