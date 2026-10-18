## DhizukuF v1.4.1 (7)

DhizukuF is a fork of [Dhizuku](https://github.com/iamr0s/Dhizuku) that shares Android **Device Owner** permissions with
other apps and adds **dish**, a Device Owner console for any terminal. This is a fix release after 1.4.0 (the Security
screen and *Enable dangerous commands* for the Dhizuku API).

### What's fixed

- **Crash in the Arabic translation** - showing the permission dialog threw an exception because of a broken
  countdown string. If you use Arabic, update.
- **dish** - the dex that is shipped with the project is up to date again (`history`, `!!`, `clear` and the newer error
  texts); a backslash inside double quotes is kept (`"C:\path"`), like in a shell. Re-export the dish files from
  *Terminal (dish)* to get the new dex.
- **Account manager** - *Freeze all* switches off every app that signs accounts in (Google Play Services among them);
  it now asks for confirmation first.
- **Sui** now works with the DhizukuF package name; the owner toast of the device admin receiver cannot throw any more;
  the Hungarian help links are back.
- Everything was checked again: dish parser and the dangerous-call rules of the API were compiled and run against a
  table of cases, all translations were checked for broken format strings.

### Install

1. Download `DhizukuF-v1.4.1.apk` below and install it (application ID `com.EdS.DhizukuF`). `DhizukuF-Hub-v1.4.1.apk`
   is the same hub that the app installs for you; use it only for a manual install. If you use the hub, open
   *Settings > Dhizuku Compatibility Hub* and press **Update** (its version follows DhizukuF).
2. Make DhizukuF the Device Owner - see the
   [Dhizuku activation tutorial](https://github.com/iamr0s/Dhizuku/discussions/19).
   Set the device up **without accounts**, or remove them first.
3. Tap **Terminal (dish)** for instructions, then follow [docs/DISH.md](docs/DISH.md).

Requires Android 8.0 or newer. Updating keeps all settings and approved apps.

Full list of changes: [CHANGELOG.md](CHANGELOG.md).
