## DhizukuF v1.2.0 (4)

DhizukuF is a fork of [Dhizuku](https://github.com/iamr0s/Dhizuku) that shares Android **Device Owner** permissions with
other apps and adds **dish**, a Device Owner console for any terminal. This release adds a compatibility hub for apps
written for the original Dhizuku, more dish commands and a steadier dish client.

### What's new

- **Dhizuku Compatibility Hub** — a hidden plugin (`com.rosan.dhizuku`, same key and same version as DhizukuF) that lets
  apps written for the original Dhizuku use DhizukuF. Install it from *Settings > Dhizuku Compatibility Hub*; it is
  carried inside the app, so nothing is downloaded. If the original Dhizuku is installed, **Replace** removes it after a
  confirmation that names the app. DhizukuF may close after the install; open it again. [docs/HUB.md](docs/HUB.md)
  The idea of the hub comes from [@Jozinho-netizen](https://github.com/Jozinho-netizen) — thank you!
- **More dish commands** — `log`, `batch`, `uninstall`, `app-restrictions`, `lock-task`, `vpn`, `location`, `backup`,
  `time`, `lock-timeout`, `adb-wifi`, `ime`, `bugreport`, `help -s WORD`. Risky forms need *Enable dangerous commands*.
- **Steadier dish** — a command that hangs is stopped after 40 s; the client tries every ActivityManager variant, sends
  the handshake twice, reconnects after DhizukuF restarts and has `history`, `!!` and `clear`. The client dex is rebuilt
  from source with d8 (and committed back by the *Rebuild dish dex* workflow).
- **App management** — switching a dish terminal off removes it from the list; refused or unanswered requests leave
  nothing behind.
- **Installs on more TV boxes** — the device-admin feature is no longer required; the `dish` launcher copes with images
  that ship only a 32 or 64 bit `app_process` and explains the read-only dex rule of Android 14+.

### Also in this line (1.1.0)

dish console, Enable dangerous commands (off by default), Device Owner / Profile Owner aware answers, Android TV support
(banner, D-pad focus, export fallback), security hardening ([SECURITY.md](SECURITY.md)), translations in all languages.

### Install

1. Download `DhizukuF-v1.2.0.apk` below and install it (application ID `com.EdS.DhizukuF`). `DhizukuF-Hub-v1.2.0.apk`
   is the same hub that the app installs for you; use it only for a manual install.
2. Make DhizukuF the Device Owner — see the
   [Dhizuku activation tutorial](https://github.com/iamr0s/Dhizuku/discussions/19).
   Set the device up **without accounts**, or remove them first.
3. Tap **Terminal (dish)** for instructions, then follow [docs/DISH.md](docs/DISH.md).

Requires Android 8.0 or newer. After updating, export the dish files again.

Full list of changes: [CHANGELOG.md](CHANGELOG.md).
