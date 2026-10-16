# Public website GUI backend

Run from the server checkout:
```bash
git pull --ff-only
ansible-playbook -i ansible/public-local.ini ansible/wire-public-gui.yml
```

This deploys the public website, Python standard-library API, SQLite schema, isolated systemd account, Nginx proxy and HTTPS endpoint checks. It preserves published APKs and signing keys.

The database lives at /var/lib/africandroid-api/releases.sqlite3. Schema initialization is automatic. Each catalog request transactionally synchronizes the database from the existing trusted publisher catalog; invalid catalogs return 503 without replacing the last committed database contents. Only existing published APK paths are accepted. Publishing continues through the signature-verifying build/publish playbooks. The API has no public mutation endpoints.

GUI mapping:
- Explore, Downloads, Install guide, home and skip links: existing local section/page navigation.
- Search and channel filter: filter releases loaded from GET /api/releases. API also supports q and channel query parameters.
- APK download: GET /api/download/{sha256}, validates database/catalog membership then redirects to the static APK.
- Checksum copy: browser clipboard with a manual-copy fallback.
- Source, license and capability links: existing GitHub documentation.
- Privacy: local privacy page.

The backend binds only to 127.0.0.1:8765. SQLite uses an isolated service-owned directory. Catalog and downloads are read-only to the service. No user tracking or payment/account tables are introduced. This scope is the public website; Android app screens and SIP service integrations are separate work.

Troubleshooting:
```bash
systemctl status africandroid-api --no-pager
journalctl -u africandroid-api -n 40 --no-pager
curl --fail http://127.0.0.1:8765/api/health
```
