# Owner control and per-action authentication

The AfricAndroid launcher and customization app request a fresh Android BiometricPrompt approval before every protected button action: app launch, dock actions, settings links, wallpaper changes, workspace preview, language directory opening, and layout chooser. Choosing a layout or opening language settings from a catalog dialog requires another fresh approval. Long-press app details is also protected. Cancellation, authentication errors, an insecure device and missing authentication support block the requested action. No grace period or cached approval is used; leaving the activity cancels outstanding requests and invalidates stale callbacks.

Android decides which enrolled fingerprint/face sensors satisfy BIOMETRIC_STRONG. A secure device PIN, pattern or password is an allowed fallback. Weak biometric sensors are excluded. This is not biometric-only enforcement and does not require two authentication factors. Biometrics need corresponding hardware/HAL/TEE support; the emulator is not evidence of secure physical biometric hardware. Raw fingerprints, face images and biometric templates are not collected by this app. Enrollment is delegated to Android settings.

When the device has no secure credential, the requested action is denied and a bootstrap dialog permits opening security settings to establish one. That enrollment path is intentionally accessible without prior authentication. Search typing, scrolling, dialog dismissal, Home/Back navigation, Android unlock, emergency calls and recovery are not gated by this app. These are not protected mutations.

## Independent owner control

There is no device-owner policy, remote administrative lock, cloud account dependency, vendor master password or server-side biometric service in this implementation. The user controls Android's local security settings and default launcher and can choose the AOSP launcher. Changing app actions requires authentication when initiated through this launcher/customizer. Other apps, external intents and direct Android settings paths are governed by Android's own policies; this gate cannot lock the entire OS.

This is not root access or unrestricted bypass of Android permissions. User-independent bootloader unlocking and physical device support depend on selected hardware. Source code can be customized by its owner; no automatic destructive unlock/flash is performed.

## System-wide requirements remain pending

Enforcement before every OS event would require framework/window-manager/service-level policies, carefully defined protected actions, secure enrollment/recovery and exceptions for emergency and accessibility flows. Neither Ansible nor a launcher intercepts all Android service calls. Do not advertise this app-level feature as a system-wide biometric firewall.

## Runtime acceptance tests (pending)

On a built emulator and supported physical hardware: (1) deny an action before credentials are enrolled, (2) enroll PIN and validate credential success, (3) test strong fingerprint/face success where hardware exists, (4) cancel/error/mismatch and verify no action runs, (5) tap rapidly and ensure one request is pending, (6) background/return during authentication and confirm stale approval does not execute, (7) verify each protected action and dialog callback, (8) verify independent Android recovery/emergency flows and alternate-launcher behavior. No compiled biometric runtime test has been performed here.

References: https://developer.android.com/reference/android/hardware/biometrics/BiometricPrompt.Builder and https://developer.android.com/reference/android/app/KeyguardManager .
