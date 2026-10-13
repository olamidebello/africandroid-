# AfricAndroid

A custom AOSP distribution project targeting an x86_64 Cuttlefish virtual phone.

**Status: source and build automation prepared; no OS image has been compiled or boot-validated yet. This is not a production-certified release.**

The complete Android platform is downloaded from upstream using Repo rather than duplicated into this Git repository. The custom product inherits AOSP's Cuttlefish phone configuration, retains its operating-system components, and adds AfricAndroid branding and the AfricCustomizer application. The app applies three generated wallpapers and opens real Android settings for display, language, accessibility, Wi-Fi, apps and security. It uses public APIs and requests only wallpaper permission.

## Build on your own host

Use a dedicated Ubuntu 22.04/24.04 x86_64 machine with at least 400 GB free space and 64 GB RAM, plus KVM for running the virtual device. Internet access to GitHub, android.googlesource.com and AOSP download hosts is required.

```bash
git clone https://github.com/olamidebello/africandroid-.git
cd africandroid-
./scripts/setup-host.sh
./scripts/sync.sh
./scripts/build.sh
```

Defaults: `android-latest-release`, release configuration `aosp_current`, build variant `userdebug`. AOSP release configurations evolve; override `AOSP_BRANCH` and `RELEASE_CONFIG` together when selecting a different supported branch. Set `AOSP_DIR`, `ARTIFACT_DIR`, `SYNC_JOBS`, or `BUILD_JOBS` through environment variables as needed. The first sync is large; build duration depends on the machine.

## Run and validate

Install upstream Cuttlefish host packages and configure KVM first; see [the host guide](docs/HOST.md). After restarting the host:

```bash
./scripts/launch.sh
adb devices
./scripts/smoke-test.sh DEVICE_SERIAL_FROM_ADB
```

Interact at `https://localhost:8443`. For a remote host use an SSH tunnel. The smoke test checks completed boot, brand, custom app launch and SELinux. It does not certify all Android functionality.

## Outputs and customization

`artifacts/dist/` contains the upstream build's distribution outputs. `artifacts/aosp-pinned-manifest.xml` records resolved upstream revisions; `africandroid-source-commit.txt` records this repository revision; `SHA256SUMS` hashes generated outputs. Archive all three with a release. Artifact files are generated only when the full build succeeds.

Edit `vendor/africandroid/africandroid_cf_x86_64_phone.mk` for branding and packages; edit `vendor/africandroid/apps/Customizer/` for the customization app. Re-run build to integrate changes. The product is userdebug with upstream development signing; never distribute it as a secure consumer release.

## What remains before a fully operational consumer OS

See [release requirements](docs/RELEASE.md). Full compilation, emulator boot and functional testing are outstanding. Phone hardware needs a separate supported device configuration, kernel, HALs and licensed vendor binaries. Production additionally requires private signing keys, verified boot and rollback protection, working signed OTA delivery, security update operations, CTS/VTS and hardware testing. Google Play and Google services are not included or licensed by this project. No OTA server, payment service, SIP app or third-party store is bundled.

## Sources and licenses

- https://source.android.com/docs/setup/start/requirements
- https://source.android.com/docs/setup/download
- https://source.android.com/docs/setup/build/building
- https://source.android.com/docs/devices/cuttlefish/get-started
- https://android.googlesource.com/device/google/cuttlefish/

New code in this repository is Apache-2.0 licensed. Upstream Android components retain their individual licenses, including GPL obligations for kernel components. Preserve upstream notices and publish corresponding sources where required.

## African languages

The customization app includes a searchable offline Glottolog catalog of 2379 African language records and 4208 dialect records. Catalog membership does not imply translated system menus, keyboards or speech. See [language coverage and translation work](docs/LANGUAGES.md).

## Mobile and desktop workspace

AfricAndroid includes an optional home launcher with Mobile, Tablet and Desktop layout profiles, app search, app icons, launch actions, long-press app details, and a Files/Settings/Customize dock. Choose it in Android home-app settings or preview it from the customization app. See [capability status](docs/CAPABILITIES.md) for implemented sources and remaining cross-OS features.

## Ansible and reviewed translations

See [Ansible deployment](ansible/README.md) for host provisioning, full AOSP builds, validated app language-pack imports and explicit feature reports. All Customizer/launcher UI strings are resource-based and RTL support is declared. No reviewed non-English packs are bundled yet, and system-wide localization is not implemented. Strict gates reject missing required packs or unverified feature requirements.

## Owner authentication

Protected launcher/customizer actions require fresh Android strong-biometric or device-credential authentication. Enrollment and control use local Android settings, with no remote management dependency. See [authentication scope and hardware requirements](docs/AUTHENTICATION.md). This is app-level enforcement; system-wide event authentication remains unimplemented.
