# Native SIP profiles and approved dialer handoff

AfricCustomizer now supports adding, editing, deleting and selecting up to 25 local SIP profiles. TLS/TCP/UDP native profiles use an explicit `sip:host:port` server URI; WSS profiles use a separate secure-WebSocket URI and require a WebRTC client. These endpoints are not interchangeable. The editor stores identity domain, username, password, transport and profile name. Unsupported/malformed endpoints are rejected; the selected default profile is a preference, not proof of registration.

Credentials and connection fields are encrypted with Android Keystore AES-GCM under a device-authentication-bound key. Metadata exposes only profile IDs/names and the selected ID in app-private storage. Protected actions require fresh authentication, and key access has a 30-second system authorization window. No cloud credential backup or plaintext password export is provided. Removing/changing device security may invalidate keys; re-enter credentials if necessary. Screen capture is blocked on profile screens; biometric hardware and encryption behavior still require runtime tests.

When the user approves using a profile in Olamide, a non-exported provider issues a random, one-use content URI valid for 45 seconds. Raw credentials are not put in the Intent. The reader must be `com.dobhrap.olamide` with the signing certificate pinned from the supplied APK and declare profile-import version 1. URI permission is granted only to that app. Replay/expired requests fail and temporary byte buffers are cleared after streaming. No bridge is trusted when an APK signer is not configured in the OS build.

The customized native dialer can import this approved profile, store it in its own authentication-bound vault, select it as its default and explicitly register it. Native SIP supports TLS/TCP/UDP, with certificate/hostname verification and required SRTP audio. WSS imports are rejected rather than silently converting endpoints. SIP credentials are never automatically uploaded to Olamide's website by this native implementation.

## Scope and pending validation

The native dialer has its own SIP registration/call engine; it does not create a SIP PhoneAccount/ConnectionService for system-wide Telecom account selection. External ACTION_DIAL intents reach its UI when selected as the default phone app, and its SIP call button uses the registered default profile. ACTION_CALL from other apps and emergency/cellular routing remain Android Telecom paths. New registration is explicit, not a claim of background push/autostart reliability.

Compilation, successful registration, incoming/outgoing audio, authentication-bound key behavior, permission denial, handoff certificate mismatch/expiry/replay, NAT/firewall behavior, and actual emergency/cellular calls must be verified separately. See native-dialer/README.md and docs/OLAMIDE-DIALER.md. No live call credentials have been used in this session.
