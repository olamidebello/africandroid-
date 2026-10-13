#!/usr/bin/env bash
source "$(dirname "$0")/common.sh"
need sudo
sudo apt-get update
sudo apt-get install -y git-core gnupg flex bison build-essential zip curl zlib1g-dev libc6-dev-i386 x11proto-core-dev libx11-dev lib32z1-dev libgl1-mesa-dev libxml2-utils xsltproc unzip fontconfig python3 repo adb
"$AFRIC_ROOT/scripts/preflight.sh"
