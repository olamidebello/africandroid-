# Release gates

Current status: build recipe and customization sources only. All gates below are pending until evidence is produced on a suitable build host.

1. Sync and archive the pinned AOSP manifest, custom source commit and branch/release configuration.
2. Successfully compile all OS and host components; archive checksummed artifacts and build logs.
3. Boot Cuttlefish; pass the repository smoke test and inspect crash logs.
4. Test setup, launcher, Settings, Wi-Fi, app installation, permissions, wallpaper, language, accessibility, audio, camera simulation, networking and reboot persistence. Virtual telephony is not evidence of real modem operation.
5. Run applicable CTS/VTS and security tests; record results rather than claiming certification prematurely.
6. Provision offline private release keys, production user builds, AVB trust roots and rollback indexes. Never commit keys. Sign target files with upstream releasetools; key mappings are target-specific and must be audited.
7. Implement and test signed OTA hosting, update client integration, full and incremental updates, interrupted updates and recovery. None is provided by this initial repository.
8. Establish security patch intake, incident handling, reproducible release provenance and license compliance.
9. Before hardware release, obtain board support, kernel source, HALs, modem/camera/GPU firmware and redistribution permissions. Test calls, emergency calls, cellular data, sensors, thermal limits, charging and battery lifetime on each supported model.
10. Obtain any required Google Mobile Services agreements independently if distributing Google services.

Do not flash the Cuttlefish output to a physical phone. Adding a product name does not make a device supported.
