# Changelog

All changes of DhizukuF compared to upstream [Dhizuku](https://github.com/iamr0s/Dhizuku).
The newest changes are at the top.

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
