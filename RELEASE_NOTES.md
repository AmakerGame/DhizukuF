## DhizukuF v1.0.0 (1) — first release

First public release of **DhizukuF**, a fork of [Dhizuku](https://github.com/iamr0s/Dhizuku) based on
upstream 2.13.0. It shares Android **Device Owner** permissions with other apps, and adds **dish**:
a Device Owner console you can use from any terminal (Termux, `adb shell`, any shell app), like
`rish` for Shizuku.

### Highlights

- **dish terminal** — export `dish` and `dish_dhizukuf.dex` from the *Terminal (dish)* card and run
  Device Owner commands from any shell. Guide: [docs/DISH.md](docs/DISH.md).
- **~50 commands** — hide / suspend / block uninstall, permissions, clear data, restrictions,
  camera, screen capture, keyguard, ADB, private DNS, settings, device info and more.
  `help` lists the names, `help COMMAND` explains one.
- **Direct API** — `dish api METHOD ARGS...` calls any public `DevicePolicyManager` method by name,
  with typed arguments. No rebuild needed.
- **Clear answers** — every command ends with `OK: ...` or `NOT DONE: ...` and the reason. Changes
  are checked by reading the value back.
- **Safe start** — access is checked before the prompt (`dish$ `) appears. If dish is off, DhizukuF
  is not the Device Owner, or you refuse the request, the console does not start.
- **Enable dish** switch next to **Enable Dhizuku**; the two work independently.
- **Approval window for terminals** — a terminal appears in *App management* as `Termux (dish)`;
  optional *Advanced confirmation window* with a Block button.

### Fixed

- User management: long user names were wrapped letter by letter; the account manager label now sits
  under the user ID.

### Install

1. Download `DhizukuF-v1.0.0.apk` below and install it (application ID `com.EdS.DhizukuF`).
2. Make DhizukuF the Device Owner — see the
   [Dhizuku activation tutorial](https://github.com/iamr0s/Dhizuku/discussions/19).
   Set the device up **without accounts**, or remove them first.
3. Open **Terminal (dish)** → **Export dish files**, then follow [docs/DISH.md](docs/DISH.md).

Requires Android 8.0 or newer. Existing dish users must export the dish files again.

Full list of changes: [CHANGELOG.md](CHANGELOG.md).
