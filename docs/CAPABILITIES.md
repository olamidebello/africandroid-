# Capability scope and verification

AfricAndroid uses AOSP as its OS foundation. Source implementation below means code is present; it does not mean a compiled OS has passed runtime validation. No claim of exact parity with proprietary operating systems is made.

| Capability | Current implementation | Verification / remaining work |
|---|---|---|
| Mobile, tablet, desktop launcher layouts | AfricAndroid home activity with persistent profile choice | Compile and test on different display sizes; layouts affect launcher only |
| Installed app search and launch | Package-manager discovery, app icons, explicit launch intents | Runtime test install/uninstall, labels, and activity resolution |
| Dock and app context actions | Files picker, Settings, customization; long press opens app details | Runtime verify each action |
| Wallpapers and personalization | Generated wallpapers and Android settings links | Validate visual layout and persistence |
| African language discovery | Offline Glottolog language and dialect directory | Translation, native names, keyboard and speech support remain unverified |
| Touch navigation, notifications and quick settings | Inherited from AOSP SystemUI | Boot and functional tests pending |
| Permissions, sandboxing, encryption, SELinux | Inherited AOSP mechanisms | Security configuration and device testing pending |
| Networking, Bluetooth, audio, camera | Inherited AOSP interfaces and virtual-device support | Emulator and hardware support differ; test per target |
| Accessibility and multiple users | Inherited Android settings/platform support | Validate supplied build and app accessibility |
| True desktop window manager, freeform resizing, taskbar, virtual desktops | Not implemented by this launcher | Separate framework/SystemUI integration required |
| AirDrop-style sharing, cloud account sync, remote desktop | Not implemented | Protocol, service, authentication and client implementation required |
| Signed OTA updates and backup service | Not implemented | Release keys, transport, client, restore and interruption testing required |
| Linux desktop applications, shell environment, containers | Not implemented | Separate runtime, filesystem, security and architecture plan required |
| Native Windows/macOS/iOS application compatibility | Not implemented | Separate compatibility technology and legal/licensing feasibility required |
| Proprietary iCloud, App Store, FaceTime, Microsoft or Apple services | Not bundled | Provider agreements and supported APIs required |
| Physical modem, emergency calls, biometric sensors | No physical hardware target configured | Device BSP, firmware, HALs and hardware validation required |

Select **AfricAndroid Customize → Choose default home launcher → AfricAndroid** after boot to use the new launcher. The AOSP launcher remains available for comparison and recovery. The desktop profile is a launcher layout, not a Windows/macOS application runtime.

Feature additions must specify observable behavior, supported target, dependencies and acceptance tests. A general request for every operating-system feature cannot be verified as one requirement.
