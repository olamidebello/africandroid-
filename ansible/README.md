# AfricAndroid Ansible automation

This playbook deploys existing implementation and reviewed app translations. It does not author all language translations, implement missing operating-system features, or guarantee iOS/Windows/macOS/Linux parity.

On your controller, install a Python virtual environment and run:

```bash
python3 -m venv .venv
. .venv/bin/activate
pip install -r ansible/requirements.txt
cd ansible
ansible-playbook audit.yml
```

To build Android, configure `inventory.ini` with your Ubuntu 22.04/24.04 x86_64 build host. The account needs SSH and sudo access. Review `group_vars/all.yml`, pin `afric_revision` to a tested commit, and provide at least 400 GB free space and 64 GB RAM:

```bash
ansible-playbook site.yml --ask-become-pass
```

The playbook installs build dependencies, creates an unprivileged build account, checks out this project, validates supplied translation packs, checks storage/memory, syncs AOSP, compiles the OS, hashes artifacts and fetches language/feature reports. Sync/build tasks have a 24-hour timeout. Re-runs update source and invoke incremental builds; they do not skip verification based on a stale success marker. Full images stay on the build host under `/srv/africandroid/artifacts`.

## Language packs

Copy `localization/template.json` to a JSON file per BCP-47 locale (for example `yo.json`). Translate all strings with native-speaker review. Set the locale, `reviewed: true`, reviewer identity and license. An unchanged English template is rejected; no translations are bundled in this initial change. Set `translation_source` to your controller's pack directory and `required_app_locales` to the exact locale tags you require. A missing required locale fails before the build.

Only Customizer/launcher resources are imported. Framework, SystemUI, Settings, other apps, keyboards, fonts and speech require their own implementation/review and are not made complete by these packs. The importer checks structure, nonempty values and format placeholders; a review declaration is not proof of linguistic correctness.

Import records are written to a generated-file manifest; only previously generated pack files are removed on re-import. Uploaded packs remain on the build host across runs. To retire one, remove its JSON file from `/srv/africandroid/translations`, then rebuild; merely omitting it from a later copy does not remove it.

## Feature release gate

`config/capabilities.json` is a finite status list, not an exhaustive cross-OS spec. It distinguishes implemented source, upstream inheritance and missing features. Runtime evidence is not supplied by this project. Set `strict_feature_gate: true` to fail deployment while any listed feature lacks runtime verification. This currently fails by design; it is a readiness check rather than a feature generator.

For an audit-only check with required app locales:

```bash
ansible-playbook audit.yml -e '{"required_locales":["yo","ha","sw"]}'
ansible-playbook audit.yml -e '{"strict_parity":true}'
```

These checks currently fail because the required reviewed packs and runtime parity evidence are absent.

## Optional Cuttlefish host installation

Set `install_cuttlefish_host: true` and pin `cuttlefish_revision` to an upstream commit. The playbook builds and installs host packages and grants emulator groups to the build account. Restart the host yourself to activate modules/udev/groups, then run the project's launch script as `africbuilder`, setting `AOSP_DIR=/srv/africandroid/aosp`. Emulator launch, reboot, production signing, OTA delivery and device flashing are outside this playbook.

Documentation: https://docs.ansible.com/projects/ansible/latest/collections/ansible/builtin/ and https://developer.android.com/training/basics/supporting-devices/languages .

## Authentication sources

The built product includes per-action Android authentication in the launcher/customizer. Ansible does not enroll biometric templates or set a universal device PIN. The owner enrolls credentials locally after boot; hardware sensors and secure biometric implementations must be supplied and tested separately. See [authentication scope](../docs/AUTHENTICATION.md).

## Geofencing and carrier information

The included `mobile-integrations.yml` renders and validates sampling interval, maximum accepted location age and local history size. The build imports these Android resources and includes the geofencing service and carrier display screen. It does not enable tracking, grant location/phone permission, set zones or change operator identity automatically. Device owners manage those controls locally after boot. The controller receives a `mobile.json` configuration report. See [mobile integration details](../docs/MOBILE-INTEGRATIONS.md).

## Olamide native phone application

Configure the optional `olamide-dialer.yml` inputs only after a signed native dialer-compatible Olamide APK is available. Supply its trusted SHA-256 and Android SDK analyzer/signer paths; validation then preloads it and generates the factory default-dialer overlay. Without the APK the feature remains disabled, rather than reporting a working SIP dialer. See [APK and privileged integration requirements](../docs/OLAMIDE-DIALER.md).
