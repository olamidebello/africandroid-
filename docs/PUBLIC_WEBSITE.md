# AfricAndroid website and APK downloads

The `website/public/` pages introduce AfricAndroid, show the supplied African artwork, explain development status and display only verified published APKs. There are no fabricated download links. A public JSON catalog and immutable APK files provide downloads without accounts, payments or a database. Standard server access logs may be generated; see the site privacy page.

## Debian 12 deployment

Target: `africandroid.world`, server `181.214.149.146`. Set the domain A record to that IP. Remove a stale AAAA record unless the server has working IPv6. Ports 80 and 443 must be reachable. The playbook requests a certificate only for the bare domain; www is not configured.

From the authenticated server root shell:

```bash
apt-get update
apt-get install -y git ansible
cd /root
# Clone only if this directory does not already exist:
# git clone https://github.com/olamidebello/africandroid-.git
cd /root/africandroid-
git pull --ff-only
ansible-playbook -i ansible/public-local.ini ansible/public-website.yml
```

The default ACME email is `dobcalldemo@gmail.com`; override with `-e website_acme_email=YOUR_EMAIL`. To deploy a reviewed version set `-e website_revision=COMMIT_SHA`. The source clone is `/opt/africandroid-website-source`, public document root `/var/www/africandroid.world`, and persistent catalog/APKs `/var/lib/africandroid-public`. Redeployment preserves published downloads and the catalog.

A matching OlamidePanel `olamide-static-N` vhost can be migrated: its document root changes to the public site and existing certificate directives are retained. The old vhost is backed up and Nginx is validated before reload. Shared/proxied or unknown vhosts and duplicate server names cause an explicit stop. Other domains and the panel vhost are not changed. If the panel later removes this managed domain/account, its domain vhost may also be removed; retain the domain inventory while using its original vhost. Re-requesting Site Publisher is unnecessary for this website.

## Automatic native APK compilation and publication

To install the website, bootstrap build tools, compile and publish a **development** native Olamide SIP APK:

```bash
cd /root/africandroid-
git pull --ff-only
ansible-playbook -i ansible/public-local.ini ansible/public-site-and-apk.yml
```

The native APK build works on Debian/Ubuntu x86_64 with at least 4 GB RAM and 12 GiB free disk (more is advisable). Java 17, checksum-validated Gradle 8.13 and Android command-line tools 16.0, API 35/build-tools 35.0.0 are provisioned. Running this installation accepts Android SDK licenses on the designated build host. Network access to official Gradle/Google download services and the project's Maven repositories is required. Compilation runs as `africbuilder`, not root. No full AOSP checkout is downloaded for this task.

Build-only:

```bash
ansible-playbook -i ansible/public-local.ini ansible/build-public-apk.yml \
  -e publish_native_apk=false
```

Output: `/srv/africandroid/artifacts/OlamideSip.apk` and `SHA256SUMS`. Preserve `/home/africbuilder/.android/debug.keystore` with restricted backups; changing it changes the signing identity and can prevent an update over an installed build. These debug APKs are displayed as development builds, not secure production releases. The script verifies the actual APK signature and metadata before publishing. Stable distribution requires separate release signing, testing and license review; the playbook does not create a production signing key or publish a stable APK.

The APK belongs to the `native-dialer/` project (`com.dobhrap.olamide`), with Android 11/API 30 minimum and ARM64/x86_64 native code. A compiled APK is not evidence of working SIP service/cellular hardware or a compiled AfricAndroid OS. Customizer/launcher are platform-source apps; this workflow does not turn the entire AOSP product into one APK.

After completion open `https://africandroid.world/#downloads`. Build failures stop before publication, leaving the prior catalog intact. Inspect the Ansible failure and build logs; do not publish nonexistent artifacts. After publishing, download the file and compare its SHA-256 with the generated checksum, then install on a compatible test device and validate its behavior.

## Publish another signed APK

On the trusted publishing server, use the supplied script with the independently trusted signing certificate fingerprint:

```bash
python3 /opt/africandroid-website-source/website/tools/publish-apk.py \
  --apk /path/to/app.apk --name 'Application name' \
  --channel development \
  --expected-signer-sha256 YOUR_TRUSTED_CERTIFICATE_SHA256 \
  --source-url https://github.com/OWNER/REPOSITORY \
  --notes 'Compatibility, release status, source and license notes.'
```

The script verifies the signature with `apksigner`, extracts actual package/version/API/CPU metadata with `aapt`, calculates SHA-256, and atomically writes the catalog. It publishes from a private snapshot of the APK to prevent concurrent build changes. It rejects debuggable APKs in the stable channel and mismatched signing fingerprints. A checksum identifies a file; it does not establish an app's safety or license compliance.

Publishing replaces the visible entry for the same package/channel but retains old immutable APK files for existing links. Disk retention is operator-managed; no deletion/scheduling system is included. There is no unauthenticated upload API: only an authorized server operator/build playbook can publish.

## Validation status

Local checks cover static links/assets, JavaScript/Python syntax, Ansible syntax, signed-metadata parser rejection and Nginx vhost preservation/rollback with mocked commands. A real APK build, device installation, browser rendering, DNS/certificate issuance and production deployment must be validated on the target environment. No APK or live website is claimed until those operations succeed.
