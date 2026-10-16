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
- **Dangerous commands are off by default.** *Security > Enable dangerous commands* gates reboot,
  clearing app data, writing settings, changing permissions, disabling the keyguard or status bar,
  enabling ADB, setting private DNS and all direct `dish api` calls. It applies to the regular Dhizuku API too.
  The Dhizuku library tells DhizukuF only the name of the API function, so DhizukuF looks at the real action: for
  `remote_transact` it reads the target interface and the transaction code and turns them into the method name
  (`IDevicePolicyManager`, `IUserManager`, `IPackageInstaller`, `IPackageManager`, `IPowerManager`). While the switch
  is off, a call to a dangerous method (wipe, factory reset, removing a user or the owner, ownership transfer,
  password reset, reboot, clearing app data, writing settings, permissions, keyguard, status bar, lock task, VPN,
  time, input methods, private DNS, uninstalling) is refused with a `SecurityException`. `remote_process`,
  `bind_user_service` and `set_delegated_scopes` run client code as the owner or give owner powers away and cannot be
  filtered by method, so they are refused as a whole. With the switch **on** the API is open: everything passes
  (wipe and factory reset too; `dish api` keeps refusing them). A method that cannot be recognised (the system hides
  its transaction names) is let through and a warning is written to the log. Approved Dhizuku API clients are still
  trusted apps: approve only those you trust.
- **Screen-lock confirmation (Security > Require device authentication).** Off by default. *Only on confirmation*:
  Allow, Refuse and Block in the permission dialog ("Allow an app to use DhizukuF?") and the same decisions in App
  management ask for the screen lock (PIN, pattern, password), fingerprint or face first; doing nothing (the timer, Back)
  still refuses, without a prompt. *Only when opening the app*: DhizukuF stays locked behind the prompt when it is opened
  and after 30 seconds in the background. *Both*: both checks. The system answers only yes or no; DhizukuF stores nothing
  about the lock. Choosing a mode needs a screen lock on the device (many TV boxes have none: DhizukuF then shows a
  message and keeps the option off); changing the mode itself asks for the lock too. If the screen lock is removed later,
  the check cannot run and is not enforced (the Security page says so). This protects against someone who holds the
  device or the remote but not the lock; it does not protect against a user who can already run code as the owner.
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
