# Administrator mode

DhizukuF knows three administration states, shown on the home screen status card:

| Mode | How it is granted | Power |
| --- | --- | --- |
| **Device Owner** | `dpm set-device-owner`, provisioning, or the Activate page | Full: every dish command and Dhizuku API call that Device Owner allows |
| **Profile Owner** | `dpm set-profile-owner` or the Activate page | Managed profile only; device-wide commands stay off |
| **Administrator** | The standard Android **device administrator** toggle (no root, no ADB) | The smallest set: screen lock helpers and password monitoring only |

## What Administrator mode is

Administrator is the regular device-admin right every Android app can ask for:
DhizukuF opens the system **Add device admin** screen and the user flips the
switch in Android settings. Nothing else is needed — no computer, no Shizuku,
no root.

> [!WARNING]
> **CAUTION: this is the most limited mode.** It cannot manage other apps,
> cannot change system settings, cannot run `dish`, and cannot protect itself
> from removal. Use it only for the few things it offers.

## What it can do

- **Screen lock** — device-admin APIs such as `lockNow()` where the system
  allows them for an active admin.
- **Password monitoring** — the device admin receiver reports wrong screen-lock
  attempts, successful unlocks, and password expiry/expiration warnings to the
  journal (Settings > Logs, "Security alert").
- **Dhizuku API** — apps that use the Dhizuku API keep working, but only the
  calls an active admin is allowed to make succeed; everything that needs
  Device Owner fails with a system refusal, exactly like any other admin app.

## What it cannot do

- Manage, hide, suspend or uninstall other apps.
- Change global/secure settings, permissions, restrictions or policies that
  require Device Owner.
- Run `dish` (the console requires Device Owner) or `remote_process` style
  shell commands.
- Block its own uninstall — Android does not offer that to a plain admin.

## Activating

1. On the home screen open the **Administrator** card (visible while DhizukuF
   is not Device / Profile Owner).
2. Confirm on the system screen and enable DhizukuF in **Device admin apps**
   (Security settings on most devices).
3. The status card switches to **You are Administrator**; the same card stays
   available to review or revoke the right.

To go further, use the **Dhizuku** or **Shizuku** activation methods to become
Device Owner instead.
