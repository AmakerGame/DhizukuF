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
| 4.4 / API 19–20 | ✅ since API 8 | ❌ | ❌ | Owner APIs do not exist; only Administrator could ever apply — but the app's UI needs Compose (API 21), so these versions are not supported anyway |
| 5.0–8.1 / API 21–27 | ✅ | ✅ | ✅ | All three modes available |
| 9–13 / API 28–33 | ⚠️ legacy | ✅ | ✅ | Google deprecated most admin policies; the mechanism still works |
| 14+ / API 34+ | ⚠️ legacy | ✅ | ✅* | Headless System User Mode: some devices require affiliated mode for Device Owner |

The app minimum is **API 21 (Android 5.0)** — set by Jetpack Compose, not by
DhizukuF. On every supported version all three modes exist; the UI still gates
the owner cards (`Dhizuku`, `Shizuku`, `ADB`) behind `AdminModes.ownerProvisioning`
so nothing that cannot work is ever shown. Whether owner provisioning actually
succeeds depends on the device state (existing accounts, an already provisioned
device, Android 14+ requirements) — the system enforces that at activation time
and the Activate page reports the real result.

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
