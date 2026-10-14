## DhizukuF v1.3.0 (5)

DhizukuF is a fork of [Dhizuku](https://github.com/iamr0s/Dhizuku) that shares Android **Device Owner** permissions with
other apps and adds **dish**, a Device Owner console for any terminal. This release is a stability and security update
with better remote-control (D-pad) support for Android TV boxes.

### What's new

- **Better D-pad / remote control** - every button shows a focus frame now, and the permission dialog starts on
  **Refuse** on a TV, so a held-down key cannot grant access by accident.
- **Safer `dish api`** - a call was treated as harmless if `--list` or `--sig` appeared anywhere in the line; now only the
  leading option counts, so *Enable dangerous commands* cannot be bypassed this way. The dish log keeps fewer words of
  `batch` / `api`.
- **Starts correctly before the first unlock** - settings and the dish registry moved to device protected storage (your
  values are moved over automatically). Before, a boot-time call could crash the app.
- **Permission dialog** - the answer is saved and sent once; a replaced request is answered `DENIED` instead of hanging;
  a closed client cannot crash DhizukuF; rotating the screen no longer denies.
- **Daemon service** - no more `ForegroundServiceDidNotStart` crash; a package check can no longer delete (and so
  un-block) an entry when the signature is temporarily unreadable; apps with several signers are checked by all of them.

### Install

1. Download `DhizukuF-v1.3.0.apk` below and install it (application ID `com.EdS.DhizukuF`). `DhizukuF-Hub-v1.3.0.apk`
   is the same hub that the app installs for you; use it only for a manual install. If you use the hub, open
   *Settings > Dhizuku Compatibility Hub* and press **Update** (its version follows DhizukuF).
2. Make DhizukuF the Device Owner - see the
   [Dhizuku activation tutorial](https://github.com/iamr0s/Dhizuku/discussions/19).
   Set the device up **without accounts**, or remove them first.
3. Tap **Terminal (dish)** for instructions, then follow [docs/DISH.md](docs/DISH.md).

Requires Android 8.0 or newer. dish files exported with 1.2.0 keep working. Updating over 1.2.0 keeps all settings and
approved apps.

Full list of changes: [CHANGELOG.md](CHANGELOG.md).
