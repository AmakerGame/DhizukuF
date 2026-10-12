## DhizukuF v1.1.0 (3)

The first release with the **dish** console, Android TV support and a security hardening pass. DhizukuF is a fork of
[Dhizuku](https://github.com/iamr0s/Dhizuku) that shares Android **Device Owner** permissions with other apps.

### Highlights

- **dish** — a Device Owner console for any terminal (Termux, `adb shell`, ...), like `rish` for Shizuku:
  ~50 commands, a direct `dish api` for any `DevicePolicyManager` method, clear `OK:` / `NOT DONE:` answers and a
  safe start (no prompt without access). Tap the *Terminal (dish)* card for a short how-to. Guide:
  [docs/DISH.md](docs/DISH.md).
- **More commands, steadier client** — `log`, `batch`, `uninstall`, `app-restrictions`, `lock-task`, `vpn`,
  `location`, `backup`, `time`, `lock-timeout`, `adb-wifi`, `ime`, `bugreport`; commands that hang are stopped
  after 40 s; the client tries every ActivityManager variant and reconnects after DhizukuF restarts.
- **Dhizuku Compatibility Hub** — a hidden plugin (`com.rosan.dhizuku`, same key and same version as DhizukuF) that lets
  apps written for the original Dhizuku use DhizukuF. Install it from *Settings > Dhizuku Compatibility Hub*; DhizukuF
  may close after the install, open it again. [docs/HUB.md](docs/HUB.md)
- **Enable dangerous commands** (off by default) — reboot, clearing app data, writing settings, changing
  permissions and direct API calls need this switch, for dish and the Dhizuku API.
- **Device Owner / Profile Owner aware** — `dish status` shows the mode; commands that need the full Device Owner
  say so clearly.
- **Android TV** — launcher banner, remote / D-pad navigation with a focus frame, and an export fallback for boxes
  without a folder picker.
- **Translations** — every string is translated into all languages of the app (Manchu excepted); Hausa is new.
  Translations are not natively reviewed, corrections are welcome.
- **Texts** — what means this app now says "DhizukuF" (for example *Activation by DhizukuF*).

### Security and fixes

- Blocked apps stay blocked after a restart or an app update; an app that changes its signature loses access at once.
- Hardened dish entry points (malformed requests, floods, dialog spam), blocked destructive calls, protection of
  DhizukuF itself, no app backups, and least-privilege permissions. See [SECURITY.md](SECURITY.md).
- Android TV: every button, switch row and list item can be selected with the remote, links no longer crash
  devices without a browser.
- User management: long user names are no longer wrapped letter by letter.

### Install

1. Download `DhizukuF-v1.1.0.apk` below and install it (application ID `com.EdS.DhizukuF`).
2. Make DhizukuF the Device Owner — see the
   [Dhizuku activation tutorial](https://github.com/iamr0s/Dhizuku/discussions/19).
   Set the device up **without accounts**, or remove them first.
3. Tap **Terminal (dish)** for instructions, then follow [docs/DISH.md](docs/DISH.md).

Requires Android 8.0 or newer. Existing dish users: export the dish files again.

Full list of changes: [CHANGELOG.md](CHANGELOG.md).
