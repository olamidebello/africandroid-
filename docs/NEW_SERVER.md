# Deploy africandroid.space on a new server

Use a fresh Debian 12 server with root access. Point the domain's A record to
the new server and remove or update any stale AAAA record. Allow inbound TCP
80 and 443 and outbound package, GitHub and Let's Encrypt access before running.
DNS changes are not performed by this script.

```bash
sudo apt-get update
sudo apt-get install -y git ca-certificates
git clone https://github.com/olamidebello/africandroid-.git
cd africandroid-
sudo bash scripts/bootstrap-public-server.sh
```

The bootstrap installs Ansible and pulls `main`, then runs
`ansible/pull-and-deploy.yml` using the local website inventory. It deploys the
existing website, Python API and database, Nginx, HTTPS redirects, certificate
renewal and local HTTPS checks for pages, catalog and API endpoints. Certbot
fails visibly if DNS or certificate issuance is unavailable. Fix the reported
cause and rerun the same command. It does not build Android or publish an APK.

Override the domain or certificate contact with extra vars:

```bash
sudo bash scripts/bootstrap-public-server.sh \
  -e website_domain=africandroid.space \
  -e website_acme_email=dobcalldemo@gmail.com
```

If Ansible is already installed, the equivalent direct command is:

```bash
sudo ansible-pull \
  -U https://github.com/olamidebello/africandroid-.git \
  -C main -d /opt/africandroid-public-bootstrap \
  -i /opt/africandroid-public-bootstrap/ansible/public-local.ini -c local \
  ansible/pull-and-deploy.yml
```

Repeat the bootstrap command to pull and deploy later changes. No unattended
application update timer is installed. Existing APK catalog/downloads under
`/var/lib/africandroid-public` and API data under `/var/lib/africandroid-api`
are preserved on repeat runs. For migration, back up and restore those paths
(stop `africandroid-api` while copying its SQLite database); a fresh server
starts with an empty catalog. Existing shared or unrecognized domain vhosts
are rejected for review rather than overwritten. Other sites are retained.

Check service status with `systemctl status nginx africandroid-api certbot.timer`.
Application logs are available with `journalctl -u africandroid-api`.
