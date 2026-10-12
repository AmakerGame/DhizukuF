# Security

DhizukuF runs with Device Owner (or Profile Owner) rights, so a mistake here can affect the whole
device. This page explains how dish is protected and how to report a problem.

## How dish is protected

- **Caller identity comes from the kernel.** Every dish request arrives through a Binder; the caller
  is identified by `Binder.getCallingUid()`, which cannot be forged by an app.
- **Same approval as every other client.** A terminal must be approved by the user (dialog or
  *App management*) and its signature must match; blocked apps are always refused. Whitelist mode
  is respected.
- **Off means off.** With *Enable dish* switched off, or when DhizukuF is not an owner, dish
  requests are refused before anything is registered or any dialog is shown.
- **Hardened entry points.** The handshake receiver ignores malformed data and limits parallel
  handshakes; unapproved callers cannot tie up Binder threads (at most 4 waiting requests, one per
  app) or flood the approval dialog (one dialog per 10 seconds per app); command lines are size-limited;
  internal error details are not sent to callers.
- **Dangerous calls are blocked.** `wipe*`, factory reset, removing users, removing the owner,
  ownership transfer and password reset are not available through `dish api`, and dish refuses to
  hide, suspend or clear the data of DhizukuF itself.
- **Dangerous commands are off by default.** *Enable dangerous commands* in Settings gates reboot,
  clearing app data, writing settings, changing permissions, disabling the keyguard or status bar,
  enabling ADB, setting private DNS and all direct `dish api` calls. It also applies to the regular
  Dhizuku API, but there it is best effort: the Dhizuku library passes only the name of the API
  function to DhizukuF, not the `DevicePolicyManager` method, so only dangerous function names can be
  recognised. Treat an approved Dhizuku API client as fully trusted.
- **Approvals are checked on every call.** The caller's signature is read each time (never cached), so an
  app updated with another certificate loses access. Blocked and pending entries are kept: they were
  silently deleted on every service start or package change before.
- **Least privilege.** Unused storage permissions were removed, the unused FileProvider (it exposed
  whole directories such as all external storage) was removed, and the boot/owner-change receiver is no
  longer exported (the system can still deliver its broadcasts).
- **Compatibility hub.** The hub (`com.rosan.dhizuku`) is signed with the same key as DhizukuF and forwards the Dhizuku
  API only to a DhizukuF with that signature, so a look-alike `com.EdS.DhizukuF` cannot receive the calls of other
  apps; DhizukuF installs and removes only a hub with its own signature. The hub carries the original's exported
  provider and request activity but adds no access: DhizukuF still identifies the calling app of every request and
  applies approvals, blocking, whitelist mode and the dangerous-commands switch. The hub APK is built with the app and
  bundled in it, so it is not downloaded and cannot be swapped in transit.
- **No backups.** `allowBackup` is off, so the approval database cannot be copied or restored
  from a backup onto another device.
- **Verified changes.** Settings and restrictions are read back after a change, so a refused change
  is reported as `NOT DONE`.

## Things to know

- On devices without a folder picker (many Android TV boxes) *Export dish files* writes the two files to the app's
  own folder `Android/data/<package>/files/dish`. On Android 10 and older, other apps that hold the storage
  permission can write there. Check the files (or export them again) before you run them, and prefer the normal
  export to a folder you trust whenever the device has a picker.

- Approving a terminal gives it the full power of the Device Owner. Approve only terminal apps you trust,
  and keep the exported `dish` and `dish_dhizukuf.dex` files in a folder only you can write to
  (anyone who can replace the `.dex` can run code as your terminal).
- The permission dialog of the Dhizuku API identifies the requesting app by the uid it receives in the
  request (inherited from upstream Dhizuku). Check the app name and icon in the dialog before you allow.

## Reporting a vulnerability

Please do not open a public issue for a security problem. Use
[GitHub private vulnerability reporting](https://github.com/AmakerGame/DhizukuF/security/advisories/new)
or contact [@AmakerGame](https://github.com/AmakerGame).
