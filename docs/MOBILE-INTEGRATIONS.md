# Location, geofencing and operator display

## Location controls

Open Customize → Location and geofencing. Add/edit/delete/enable zones using a name, latitude, longitude and radius (100–100000 meters, up to 50 zones). Start monitoring yourself, grant precise location and enable Android location services. All management actions use the existing authentication gate. Approximate-only permission is insufficient. A persistent foreground notification indicates monitoring; battery use depends on provider/device.

GPS/network updates evaluate enabled zones. Entry/exit events are recorded locally; the initial position is identified as initial_inside/initial_outside rather than a false crossing. Location accuracy is considered: uncertain boundary samples do not create crossings. Known mock locations and stale samples are rejected. This does not make location tamper-proof against modified firmware or sophisticated spoofing. No remote tracking server, cloud upload or forced permission grants are implemented.

Default sampling requests are 30 seconds, sample expiry is 180 seconds and history is limited to 200 zone/time events. Android/providers may deliver updates late; crossing detection is best-effort, not real-time. Ansible parameters configure sampling, sample expiry and history through the mobile integration resource profile. Controls, coordinates and history are stored in the app's private storage. Clearing history does not delete zones.

An optional owner-selected restriction permits third-party app launches from this launcher only with a fresh accurate position confidently inside an enabled zone. Outside/uncertain/stopped/restarted monitoring blocks those launches. Customize and Settings remain accessible so the owner can disable the rule. Already running apps, external intents, other launchers, emergency access and recovery are outside this restriction. It is not a system-wide kiosk, anti-theft lock or enterprise geofence.

Monitoring is not started at boot and is not silently restarted after app/service termination. Start it again from the visible app. This avoids requesting background location and follows Android's foreground-service rules. A compiled OS and physical devices must verify permission denial/revocation, background operation, boundary accuracy, providers unavailable, battery behavior and mocked/stale samples.

## Carrier display

Open Customize → Carrier information and display. Authenticate, then explicitly grant READ_PHONE_STATE to read active SIM details. The app displays SIM slot, SIM carrier, registered operator and MCC/MNC where supported. It does not request phone numbers, IMSI or IMEI. Without a SIM, privilege, permission or supported telephony service it reports that details are unavailable.

Edit the launcher badge or a per-subscription local SIM label, or reset all labels. The badge is visible in the AfricAndroid launcher. Per-SIM labels belong to this app and are keyed to Android subscription IDs; SIM replacement/ID changes may require resetting them. They do not modify subscription records, the SIM, the registered carrier or Android SystemUI/status-bar/lock-screen operator names. The real reported operator remains separately identified. No privileged operator-brand override or modem provisioning is implemented.

Mobile-network settings are delegated to Android, which applies its own carrier/device restrictions. APN/network-selection/SIM-renaming operations vary by hardware and provider. Changing SystemUI operator display requires a separate privileged integration, while real operator branding/routing requires carrier privileges. Ansible includes these classes in the existing AOSP build; it does not alter a carrier's identity or grant device permissions.

## Ansible

`ansible/site.yml` includes `mobile-integrations.yml` before compilation and passes its generated profile to `scripts/build.sh`. Reports are fetched as `mobile.json`. The device owner must configure zones, consent to monitoring and set local labels after boot. No coordinates or subscriber secrets are sent to Ansible. See `ansible/README.md` for build-host requirements. AI support-agent integration remains pending its independent API credential decision.

References: https://developer.android.com/develop/background-work/services/fgs/service-types , https://developer.android.com/reference/android/location/LocationManager , https://developer.android.com/reference/android/telephony/SubscriptionManager , https://developer.android.com/reference/android/telephony/TelephonyManager .
