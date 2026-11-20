# Administrator mode

DhizukuF knows three administration states, shown on the home screen status card:

| Mode | How it is granted | Power |
| --- | --- | --- |
| **Device Owner** | `dpm set-device-owner`, provisioning, or the Activate page | Full: every dish command and Dhizuku API call that Device Owner allows |
| **Profile Owner** | `dpm set-profile-owner` or the Activate page | Managed profile only; device-wide commands stay off |
| **Administrator** | The standard Android **device administrator** toggle (no root, no ADB) | The smallest set: whatever a device admin may do, exposed through dish and the Dhizuku API |

## Android version support

| Android | Device Administrator | Profile Owner | Device Owner | Notes |
| --- | --- | --- | --- | --- |
| 4.4 / API 19–20 | ✅ since API 8 | ❌ | ❌ | Owner APIs do not exist; only Administrator could ever apply — and the app's UI libraries (Compose, androidx.navigation) need API 24, so these versions are not supported anyway |
| 5.0–7.0 / API 21–23 | ✅ | ✅ | ✅ | Modes exist, but Compose and androidx.navigation require API 24 — below that the app cannot install |
| 7.1–8.1 / API 24–27 | ✅ | ✅ | ✅ | All three modes available |
| 9–13 / API 28–33 | ⚠️ legacy | ✅ | ✅ | Google deprecated most admin policies; the mechanism still works |
| 14+ / API 34+ | ⚠️ legacy | ✅ | ✅* | Headless System User Mode: some devices require affiliated mode for Device Owner |

The app minimum is **API 24 (Android 7.0)** — required by the Compose and
androidx.navigation libraries in the current dependency versions, not chosen by
DhizukuF. On every supported version all three modes exist; the UI still gates
the owner cards (`Dhizuku`, `Shizuku`, `ADB`) behind `AdminModes.ownerProvisioning`
so nothing that cannot work is ever shown. Whether owner provisioning actually
succeeds depends on the device state (existing accounts, an already provisioned
device, Android 14+ requirements) — the system enforces that at activation time
and the Activate page reports the real result.

## Device Owner via QR code (provisioning)

A factory-reset device can become managed by DhizukuF straight from the setup
wizard — no computer and no ADB. The QR code below is the standard Android
**QR provisioning** payload for the **v2.0.0** release.

<p align="center">
  <img src="images/provisioning-qr.png" width="320" alt="DhizukuF v2.0.0 provisioning QR code" />
</p>

1. Factory-reset the device (provisioning only works on the very first setup screen).
2. On the **Welcome** screen tap the same spot **6 times** until the QR scanner opens
   (on some devices: tap the screen 6 times, then connect to Wi-Fi when asked).
3. Connect to Wi-Fi if asked, then scan the QR code.
4. Android downloads the APK, checks its signature, installs DhizukuF and makes it
   **Device Owner**. Finish the setup wizard as usual.

What the code contains:

| Field | Value |
| --- | --- |
| Admin component | `com.EdS.DhizukuF/.server.DhizukuDAReceiver` |
| APK | `https://github.com/AmakerGame/DhizukuF/releases/download/v2.0.0/DhizukuF-v2.0.0.apk` |
| Signature checksum | `pA2oClnRcMqpUM8VwYxFTUejmyaYnYtkDs10W6cb9dw` (SHA-256 of the signing certificate, URL-safe Base64) |
| Leave system apps enabled | yes |
| Skip encryption | yes |

<details>
<summary>Raw JSON (to regenerate the QR code)</summary>

```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.EdS.DhizukuF/.server.DhizukuDAReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://github.com/AmakerGame/DhizukuF/releases/download/v2.0.0/DhizukuF-v2.0.0.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": "pA2oClnRcMqpUM8VwYxFTUejmyaYnYtkDs10W6cb9dw",
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": true
}
```

</details>

> [!IMPORTANT]
> The QR code is tied to **v2.0.0**: the download link and the checksum belong to that
> release. For another version, or if the signing key changes, generate a new code
> with the new link and checksum. The code works only on a device that has not been
> set up yet; on a device that is already in use, use `dpm set-device-owner` or the
> Activate page instead.

## Before you activate Device Owner

> [!WARNING]
> **Activation through Shizuku can delete data.** ADB refuses to set a Device Owner while other
> users exist ("already several users on the device"). The Shizuku path skips that check so it also
> works on a device that is already set up. Android does not allow a **Private Space** on a device
> with a Device Owner and **deletes it together with all data inside** — this cannot be undone. Work
> profiles and other users can be affected too.
>
> Back up or move that data first. DhizukuF warns you and shows the number of profiles it found before
> it continues. If you are unsure, use ADB (it stops when other users exist) or the QR code on a
> freshly reset device. Background: [Dhizuku #359](https://github.com/iamr0s/Dhizuku/issues/359).

## What Administrator mode is

Administrator is the regular device-admin right every Android app can ask for:
DhizukuF opens the system **Add device admin** screen and the user flips the
switch in Android settings. Nothing else is needed — no computer, no Shizuku,
no root.

> [!WARNING]
> **CAUTION: this is the most limited mode.** It cannot manage other apps or
> change most system settings, and the admin right can always be removed in
> Android settings. Use it only for the few things it offers.

## What it can do

There is no separate settings page: Administrator mode simply lets the usual
interfaces reach the functions an active device admin is allowed to perform.

- **dish** — the console works in Administrator mode. Commands that need the
  Device Owner answer "needs Device Owner" (for example `settings put global`,
  app management), while everything a device admin may do runs with the admin
  rights — for example `dish lock`, or `dish api` calls into
  `DevicePolicyManager` methods the admin is allowed to use (screen lock,
  camera off, password policies, uninstall blocking of chosen apps, and so on,
  depending on the Android version and the declared policies).
- **Dhizuku API** — apps that use the Dhizuku API keep working. Their calls run
  with DhizukuF's admin rights, so only the calls a device admin is allowed to
  make succeed; everything that needs Device Owner fails with a system refusal,
  exactly like any other admin app.
- **Password monitoring** — the device admin receiver reports wrong screen-lock
  attempts, successful unlocks, and password expiry warnings to the journal
  (Settings > Logs, "Security alert").

## What it cannot do

- Manage, hide, suspend or uninstall other apps (device admin has no such
  right; `setUninstallBlocked` only *blocks* uninstalls).
- Change global/secure settings, permissions, restrictions or policies that
  require Device Owner.
- Guarantee protection from removal — the admin right itself can always be
  switched off in Android settings first.

## Activating

1. On the home screen open the **Administrator** card (visible while DhizukuF
   is not Device / Profile Owner).
2. Confirm on the system screen and enable DhizukuF in **Device admin apps**
   (Security settings on most devices).
3. The status card switches to **You are Administrator**; the same card stays
   available to review or revoke the right.

To go further, use the **Dhizuku** or **Shizuku** activation methods to become
Device Owner instead.

## Granting Administrator to another app

The Activate page (Dhizuku / Shizuku) can also grant the plain device-admin right to a
selected app: pick **Administrator** in the owner-type menu and confirm. DhizukuF calls
the private `setActiveAdmin` through the shell binder — the same effect as
`adb shell dpm set-active-admin` — and verifies `isAdminActive` before reporting success.
This is still the limited mode: the app gets only what a device admin may do.
