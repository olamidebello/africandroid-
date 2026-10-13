# Olamide SIP and owner-authorized OS integration

The inspected Olamide mobile source identifies its package as `com.dobhrap.olamide`. The repository now includes customized native SIP dialer source in `native-dialer/`, separate from the earlier Capacitor wrapper. It includes a Liblinphone engine, profile management, call UI and InCallService. Successful APK compilation and live call behavior are distinct checks; it does not yet provide a SIP Telecom ConnectionService, push wakeup or server credential provisioning. A Capacitor website wrapper alone does not meet native dialer requirements.

## Preload and factory default

Ansible accepts a controller-local signed Olamide APK and trusted SHA-256. Set `olamide_enabled: true`, `olamide_apk_source` and `olamide_apk_sha256`; install Android SDK `apkanalyzer` and `apksigner` on the build host and configure their paths. Enabled integration fails if any input, signature/checksum check, expected package, exported tel ACTION_DIAL activity, protected UI InCallService or x86_64 native ABI requirement is missing.

After validation, the build generates an AOSP `android_app_import`, preloads the original signed APK on the product partition, and adds a framework `config_defaultDialer` overlay. This is a factory default preference; Android still validates role eligibility and the owner can change it. It does not guarantee a SIP call works or force-replace a user's existing phone-app choice after an update. Dialer integration is disabled by default until the compatible APK exists.

## Owner privileged controls

AfricCustomizer is built as a platform-signed privileged product app using the system SDK. Its privileged permission list contains only MANAGE_ROLE_HOLDERS; runtime location and phone-state permissions remain owner-granted. The Olamide APK retains its original signature and is not granted unrestricted privileged permissions. No root, arbitrary shell, universal device-privilege toggle or automatic permission grants are provided.

Open Customize → Olamide SIP and native dialer. After biometric/device-credential authentication and an explicit confirmation, the app requests the Android dialer role for the expected installed package for the current user. Android applies its eligibility rules; failures are reported. It also provides authenticated dialpad/default-app settings actions. Restore another dialer through Android Default apps. The existing AOSP phone application is retained for recovery and emergency-call fallback; do not remove it before complete hardware testing.

## Required runtime verification

Compile the system SDK integration and overlay, boot a fresh Cuttlefish image, inspect the actual role holder, test changing and restoring defaults, verify permission allowlisting, and test dial intents plus incoming/outgoing SIP and cellular calls. Hardware tests must cover emergency handling, audio routing, Bluetooth, network transitions, reboot/push wakeup and supported ABIs. No native SIP call or compiled privileged runtime has been validated here. Never use development platform signing keys for a consumer release.

Source inspected: https://github.com/olamidebello/Sip/tree/main/apps/mobile . Android role requirements: https://developer.android.com/develop/connectivity/telecom/dialer-app .

Set `build_native_dialer: true` to compile the included native source through Ansible before OS preload. The SDK/Gradle paths must exist and SDK licenses be accepted. Debug builds are development artifacts, not consumer release signing. The preload step pins the APK signing certificate for the approved SIP-profile handoff.
