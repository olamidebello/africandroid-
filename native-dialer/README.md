# Olamide native SIP dialer

Native Android source, package `com.dobhrap.olamide`, using pinned Liblinphone no-video 5.4.128. Includes native dialpad, encrypted/authentication-bound profile management, default profile, explicit registration, TLS/TCP/UDP signaling, required SRTP audio, certificate/hostname checks, incoming/outgoing SIP call state, answer/reject/hangup, DTMF, mute, hold/resume and speaker/earpiece. InCallService provides basic cellular answer/hangup/hold controls when selected as the phone app. Emergency numbers are sent to Android Telecom rather than SIP; physical validation is mandatory.

This is a development implementation, not a verified production dialer. WSS profiles are rejected by this native engine. SIP endpoints, valid credentials, SRTP support and network/media routing are required. There is no SIP PhoneAccount/ConnectionService binding for system-wide outbound account selection, push wakeup, autostart after termination, video, conferencing, recording or tested Bluetooth integration. Registration is explicit; foreground monitoring stops when terminated. Not every call-control feature is implemented for cellular calls.

## Build

Use JDK 17, Android SDK platform 35/build-tools 35.0.0 and Gradle 8.13. Run `gradle --no-daemon :app:assembleDebug` in this directory. GitHub Actions runs this build and uploads a debug APK. Source compilation is separate from live SIP/cellular testing. Debug signing is for testing; supply your own release signing and corresponding source/license obligations before distribution.

The native dialer sources in this directory are licensed under AGPL-3.0-or-later to match the open-source SDK distribution. Shared vault/auth code is also available under the repository Apache-2.0 license; incorporation here is under the native dialer license. The SDK and its third-party components retain their own notices. Do not describe the linked native APK as wholly Apache licensed. Review codecs and dependency license artifacts before redistributing.

Native UI has English source strings initially. The OS language catalog does not supply full translated native-dialer menus. Keystore invalidation after changing/removing screen lock may require re-entering credentials. No raw credentials are put in Intents or source control. Profiles imported from AfricCustomizer use an approved one-use URI plus a pinned installed-app signing certificate; no handoff works until the compiled APK certificate is configured in the OS build.
