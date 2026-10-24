#!/usr/bin/env bash
# Provision a fresh Debian 12 server or repeat a website deployment.
set -euo pipefail
if [[ ${EUID} -ne 0 ]]; then
  echo 'Run as root (sudo bash scripts/bootstrap-public-server.sh).' >&2
  exit 1
fi
source /etc/os-release
if [[ ${ID} != debian || ${VERSION_ID} != 12 ]]; then
  echo 'This website deployment requires Debian 12.' >&2
  exit 1
fi
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y ansible git ca-certificates python3 util-linux
# Serialize repeated runs; preserve locally changed source instead of forcing a reset.
exec 9>/run/lock/africandroid-public-deploy.lock
flock -n 9 || { echo 'A website deployment is already running.' >&2; exit 1; }
exec ansible-pull \
  -U https://github.com/olamidebello/africandroid-.git \
  -C main -d /opt/africandroid-public-bootstrap \
  -i /opt/africandroid-public-bootstrap/ansible/public-local.ini -c local \
  ansible/pull-and-deploy.yml "$@"
