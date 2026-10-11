# Changelog

All changes of DhizukuF compared to upstream [Dhizuku](https://github.com/iamr0s/Dhizuku).
The newest changes are at the top.

## 1.1.0-pre1 (2) — pre-release

### Added
- **Android TV support**: appears in the TV launcher with a banner, no touch screen required, full D-pad /
  remote navigation (every card and switch row is focusable and shows a focus frame; a settings row is toggled
  with the centre button). If the device has no folder picker, *Export dish files* saves to the app folder
  (`Android/data/com.EdS.DhizukuF/files/dish`) and shows the path.
- Tapping the **Terminal (dish)** card opens a short "How to use dish" dialog: dish is not a terminal
  inside the app, export the files, run dish in a terminal app (best in Termux), and the three steps.
  It has an *Export dish files* button.

### Changed
- Texts that mean this app now say **DhizukuF** (enable switch, permission dialog, settings, block texts,
  notification) in every language; texts that mean other Dhizuku implementations or the Dhizuku API keep "Dhizuku".
- The activation card is titled **Activation by DhizukuF** (was "Dhizuku") in every language. Its description
  still says "Dhizuku", because it refers to other Dhizuku implementations.
- Device Owner vs Profile Owner: `status` shows the mode; commands that need the Device Owner answer
  `NOT DONE: ... needs Device Owner, but DhizukuF is Profile Owner`; `help` marks them with `*`
  and `help COMMAND` has a `Needs:` line. `SecurityException` answers mention the mode.

### Translations
- All missing strings are now translated into every language of the app, including the dish
  texts, user and account management and error messages (except Manchu `mnc`, which stays in
  English where no translation exists). Hausa (`ha`) was empty and is now fully translated.
  These translations were prepared without native review; corrections are welcome.

### Security
- Least privilege: removed the unused `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` permissions and the
  unused FileProvider (its paths covered the whole external storage); `DaemonReceiver` is no longer exported.
- New switch **Enable dangerous commands** (off by default) for dish and the regular Dhizuku API; see
  [docs/DISH.md](docs/DISH.md#dangerous-commands). For the regular API it is best effort.
- Fixed: a **blocked** app (or a pending request) was deleted from the database every time the daemon
  service started or a package changed, which silently un-blocked it. Only a missing package or a changed
  signature removes an entry now, and an app update no longer drops its approval.
- Fixed: the caller's signature was cached in the Dhizuku service, so an app updated with a different
  certificate could keep access until the service was recreated. It is checked on every call now.
- `allowBackup` is now off: the approval database cannot be backed up or restored onto another device.
- The dish handshake receiver ignores malformed extras (they could crash the app) and limits parallel
  handshakes.
- Unapproved callers can no longer block Binder threads for 60 s (max 4 waiting requests, one per app),
  and the approval dialog is shown at most once per 10 seconds per app.
- dish command lines are size-limited and internal error details are no longer returned to callers.
- `dish api` also blocks `removeUser*` calls and `clearApplicationUserData` outside dpm; dish refuses to
  hide, suspend or clear the data of DhizukuF itself.
- New [SECURITY.md](SECURITY.md) with the protection model and how to report vulnerabilities.

### Fixed
- The **Terminal (dish)** card was shown only when DhizukuF was already an owner, so on a device that
  was not (yet) an owner, or had not refreshed its state, it looked missing. It is now always shown.
  Without an owner it explains that dish will refuse commands; in Profile Owner mode it notes that
  commands needing the Device Owner are unavailable.

## 1.0.0 (1)

### Added
- **Enable dish** switch in Settings, next to **Enable Dhizuku**. dish no longer depends on the
  Dhizuku switch, and the Dhizuku switch no longer affects dish.
- dish checks access **before** the console opens: the `dish` launcher runs a silent `ping` first.
  If dish is off, DhizukuF is not the Device Owner, or access is refused, no prompt appears.
- When dish is off, a terminal is neither registered in *App management* nor shown an approval window.
- New prompt: `dish$ ` instead of `dish> `.
- New commands: `version`, `id`, `device`, `ping`, `list`, `app-info`, `is-suspended`,
  `enable-system-app`, `install-existing`, `clear-data`, `permission-policy`, `keyguard`,
  `status-bar`, `mute`, `adb`, `stay-awake`, `auto-time`, `auto-timezone`, `private-dns`,
  `owner-info`, `org-name`, `restriction has|available`, `settings list`.
- **`dish api`** (alias `call`): direct calls to any public method by name, typed arguments
  (`s: i: l: f: d: b: cn: strs: ints: null @admin`), `--list`, `--sig`, `--no-admin` and
  `--on dpm|pm|um|am|audio|power|wifi|conn|notif|tele`.
- `help` prints only the command names; `help COMMAND` shows description, usage and an example.
- Unknown commands get suggestions.

### Changed
- Every dish answer says what happened: `OK: ...`, `NOT DONE: ...` (exit code 1) or `Usage: ...`
  (exit code 2). Changes to restrictions, settings and ADB are verified by reading the value back.
- `suspend` / `unsuspend` report which packages succeeded and which did not.
- Clearer access-check messages (`Access check failed: ...`) with the next step to take.
- `status` shows owner state, admin component and Android version in labelled lines.

### Fixed
- User management: the account manager label took most of the card width, so a user name such as
  "Адріан" was wrapped letter by letter. The label now sits under the user ID.

### Notes
- `dish_dhizukuf.dex` was changed in place (the prompt string and the sorted string table) because
  no dex compiler was available. `dish-client/src` contains the matching source change; a rebuild
  with `dish-client/build.sh` gives the same behaviour.
- Existing users must export the dish files again to get the new launcher and client.

## Earlier fork changes

### Added
- **dish terminal**: `Terminal (dish)` card with *Export dish files*, a Binder-based client
  (`DishMain`), and the server side (`DishReceiver`, `DishBinder`, `DishEngine`).
- Terminals are registered in *App management* as `<app> (dish)` and use the normal approval dialog.
- Settings: **Show dish** label and **Advanced confirmation window** (Block / Allow / Don't allow).
- Basic commands: `status`, `lock`, `reboot`, `hide`, `suspend`, `block-uninstall`, `permission`,
  `restriction`, `camera`, `screen-capture`, `settings`, `timezone`, `call`.
- Application ID `com.EdS.DhizukuF`.
