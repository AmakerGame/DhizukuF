## DhizukuF v1.5.0 (8)

DhizukuF is a fork of [Dhizuku](https://github.com/iamr0s/Dhizuku) that shares Android **Device Owner** permissions with
other apps and adds **dish**, a Device Owner console for any terminal.

This release answers the request [#2](https://github.com/AmakerGame/DhizukuF/issues/2): *change the name and icon of the
app, keep them for updates, and an AMOLED theme*. It also adds a **Logs** journal and carries the fixes of 1.4.1.

### What's new

- **Customization** (new card on the home screen) - *Theme*: System, Light, Dark or **AMOLED** (pure black background);
  *App name*: DhizukuF, Dhizuku, Profile Owner or Device Owner; *Icon*: the **new icon** (default) or the icon of the
  **original Dhizuku** (kept, it cannot be deleted). The name and the icon are chosen **independently**: changing one
  keeps the other. Every combination has its own **TV banner**. *Remember the name and icon for updates* (on by default).
  The package name never changes.
- **Edit the home screen** - a **pencil** next to the X: move the cards with arrows, hide or show any card with the eye,
  or reset. Works with a remote (D-pad).
- **Logs** (off by default) - *Settings > Logs* turns on a journal of service events, permissions, commands and errors,
  with a *Logs* screen (search, details), level, categories, size limit and a text export for bug reports. Off means
  off: nothing is saved. See [docs/LOGS.md](docs/LOGS.md).
- **TV and remote** - Customization shows the TV banner of the chosen name and icon on a TV, a text field can be left with the
  Up / Down keys, and a failed switch of the launcher entry is reported instead of silently ignored.
- **Fixes of 1.4.1** - a crash in the Arabic translation (permission dialog), an up-to-date dish dex, Sui support with the
  right package name, *Freeze all* asks first, a backslash inside double quotes in dish is kept.

### Good to know

- Android cannot rename or redraw the launcher entry of an installed app, so the names and icons are ready-made: 8 launcher
  entries (4 names x 2 icons) switch between each other. The launcher may need a few seconds to refresh after a change. Icons
  that earlier versions left on the home screen keep working. See [docs/CUSTOMIZATION.md](docs/CUSTOMIZATION.md).

### Install

1. Download `DhizukuF-v1.5.0.apk` below and install it (application ID `com.EdS.DhizukuF`). `DhizukuF-Hub-v1.5.0.apk`
   is the same hub that the app installs for you; use it only for a manual install. If you use the hub, open
   *Settings > Dhizuku Compatibility Hub* and press **Update** (its version follows DhizukuF).
2. Make DhizukuF the Device Owner - see the
   [Dhizuku activation tutorial](https://github.com/iamr0s/Dhizuku/discussions/19).
   Set the device up **without accounts**, or remove them first.
3. Tap **Terminal (dish)** for instructions, then follow [docs/DISH.md](docs/DISH.md). Re-export the dish files to get
   the current dex.

Requires Android 8.0 or newer. Updating keeps all settings and approved apps.

Full list of changes: [CHANGELOG.md](CHANGELOG.md).
