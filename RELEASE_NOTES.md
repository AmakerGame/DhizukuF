## DhizukuF v1.4.0 (6)

DhizukuF is a fork of [Dhizuku](https://github.com/iamr0s/Dhizuku) that shares Android **Device Owner** permissions with
other apps and adds **dish**, a Device Owner console for any terminal. This release adds a **Security** screen and makes
*Enable dangerous commands* really work for the Dhizuku API.

### What's new

- **Security screen** - *Whitelist mode* and *Enable dangerous commands* now live here, together with
  **Require device authentication**: *Off* (default), *Only on confirmation*, *Only when opening the app*, or both.
  The screen lock, fingerprint or face is asked by the system. *On confirmation* asks before **Allow** (permission
  dialog and *App management*); Refuse and Block never ask. If the device has no screen lock (many TV boxes) a message
  says so and the option stays off.
- **Dangerous commands apply to the Dhizuku API** - until now the switch never matched anything for API clients. With
  the switch off, an API client can no longer wipe the device, remove users or the owner, change settings, permissions,
  keyguard, VPN, time and similar, uninstall apps or reboot; `remote_process`, `bind_user_service` and
  `set_delegated_scopes` are refused. With the switch on everything passes. **Apps that use these calls stop working
  until you switch it on.** The list is in [SECURITY.md](SECURITY.md).
- **Hub notes** - [docs/HUB.md](docs/HUB.md) now states what works for old and new Dhizuku clients and the one limit.
- **Fixes** - dish `camera` / `screen-capture` verify the result; icons are cached properly; a cancelled activation can
  no longer show its result; dish terminals with an unknown package can be switched in *App management*; a failed hub
  install leaves no session behind.
- **Project** - GitHub issue forms for this project; Kotlin 2.4.21, Gradle 9.8.1.

### Install

1. Download `DhizukuF-v1.4.0.apk` below and install it (application ID `com.EdS.DhizukuF`). `DhizukuF-Hub-v1.4.0.apk`
   is the same hub that the app installs for you; use it only for a manual install. If you use the hub, open
   *Settings > Dhizuku Compatibility Hub* and press **Update** (its version follows DhizukuF).
2. Make DhizukuF the Device Owner - see the
   [Dhizuku activation tutorial](https://github.com/iamr0s/Dhizuku/discussions/19).
   Set the device up **without accounts**, or remove them first.
3. Tap **Terminal (dish)** for instructions, then follow [docs/DISH.md](docs/DISH.md).

Requires Android 8.0 or newer. dish files exported with an earlier version keep working. Updating keeps all settings and
approved apps; *Whitelist mode* and *Enable dangerous commands* keep their values (they only moved to **Security**).

Full list of changes: [CHANGELOG.md](CHANGELOG.md).
