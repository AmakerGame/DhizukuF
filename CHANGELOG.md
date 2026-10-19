# Changelog

All changes of DhizukuF compared to upstream [Dhizuku](https://github.com/iamr0s/Dhizuku).
The newest changes are at the top.

## Unreleased

### Added
- **Logs.** *Settings > Logs* (off by default) turns on a journal of DhizukuF: service started / stopped, Device Owner
  status, app requested access / connected / disconnected, permission granted / denied / blocked, commands from apps
  (Dhizuku API and dish) with their result, blocked dangerous calls and errors (a crash is written too).
  - A **Logs** card on the home screen, **above Terminal**, shown only while logging is on. The **Logs** screen has
    *Search* (text, app name, package, event type, command, result), newest entries first, and a details view with
    everything that is known (time, type, package, UID, app, command, result, error).
  - Options: *Enable logs*, *Log level* (Errors only / Warnings / Normal / Debug), *Log categories* (service,
    permission, app requests, commands, errors, debug), *Maximum log size* (1 / 5 / 10 / 25 MB, oldest entries are
    deleted), *Export logs* (`dhizukuf-logs-YYYY-MM-DD.txt`), *Clear logs*.
  - Off means off: no file, no thread, no saved event. Commands are recorded by their first words only; see
    [docs/LOGS.md](docs/LOGS.md) for what is and is not recorded.
- The bug report forms ask for the exported journal.

## 1.4.1 (7)

A fix release: one crash, a stale dish file and several small things found in a full review of the project.

### Fixed
- **Crash in Arabic**: the string of the countdown in the permission dialog had `d%` instead of `%d`, so showing the
  dialog threw `UnknownFormatConversionException`. (Inherited from the original project.) All other translations were
  checked with a Java `Formatter` simulation: no more broken format strings.
- Hungarian: the help links of the Shizuku and ADB cards were missing.
- The committed `dish_dhizukuf.dex` was older than `dish-client/src` (no `history`, `!!`, `clear`, the newer error text);
  it is rebuilt from the sources (CI did this already for the APKs; local builds used the old file).
- Sui: `Sui.init()` was called with the package name of the original Dhizuku instead of DhizukuF's own.
- *Account manager*: **Freeze all** switches off every app that signs accounts in (Google Play Services among them) and
  asked nothing; it now asks for confirmation first.
- The "owner" toast of the device admin receiver could throw if the system service was missing.
- dish: inside double quotes a backslash only escapes `"`, `\`, `$` and the backtick, like in a shell. Before, every
  backslash was dropped, so `"C:\path"` became `C:path`.

### Checked
- The command line splitter of dish and the dangerous-call rules of the Dhizuku API were compiled and run with the
  Kotlin compiler against a table of cases (all rules behave as written in SECURITY.md); every Kotlin file of the app
  parses cleanly; all strings of all 40+ translations were checked with a Java `Formatter` simulation, plus the
  Markdown links, the manifest components and the keys of all lazy lists (no duplicates).

## 1.4.0 (6)

### Added
- **Security** screen (new card on the home screen): *Whitelist mode* and *Enable dangerous commands* moved here from
  *Settings*, plus **Require device authentication** with four choices: *Off* (default), *Only on confirmation*,
  *Only when opening the app*, *On confirmation and when opening the app*. The screen lock (PIN, pattern, password),
  fingerprint or face is asked through the system prompt (AndroidX Biometric 1.1.0).
  - *On confirmation*: **Allow** (permission dialog, and switching an app on in *App management*) asks first.
    Refuse, Block, Unblock, switching an app off and doing nothing (timer, Back) never ask. The countdown of the dialog
    waits while the prompt is open.
  - *When opening the app*: DhizukuF is locked until the prompt is passed and again after 30 seconds in the background.
  - The device is checked for a screen lock. Without one (many TV boxes) a message explains it, with a button to the
    Android settings, and the option stays off. Changing the mode asks for the lock too.
- GitHub issue forms made for this project: app / UI, dish terminal, Android TV / D-pad, activation, Hub / API, idea,
  translation (plus a private security link and a pull request template). `.github/create-labels.sh` creates their labels.

### Changed
- **Dangerous commands now really apply to the Dhizuku API.** The Dhizuku library passes only function names such as
  `remote_transact` to DhizukuF, so the old name filter never matched anything. DhizukuF now reads the target interface
  and the transaction code of every `remote_transact` and refuses dangerous methods (see SECURITY.md for the list)
  while the switch is off; `remote_process`, `bind_user_service` and `set_delegated_scopes` are refused as a whole
  (they run code as the owner or hand out its powers). With the switch on everything passes. A client sees a
  `SecurityException` that names the switch. Hub clients are covered the same way.
- Build: Kotlin 2.4.21 and Gradle 9.8.1, like the original Dhizuku.
- *docs/HUB.md* now says what was checked against the source of the Dhizuku client library, and the limit that follows
  from it (see Hub below).

### Fixed
- Dhizuku API: the names of the Binder transactions are read through hidden API; the exemption is now set where it is
  used and an empty answer is not remembered, so the dangerous-call check cannot stay silently off after one early failure.
- Hub: an install of the hub that fails before the commit no longer leaves its install session behind.
- dish `camera` and `screen-capture` (`disable` / `enable`) said `OK` without checking; the value is read back now and a
  refusal of the system is reported as `NOT DONE`.
- The icon cache counted every icon 256 times too big, so it held about one icon and the lists reloaded icons while
  scrolling. It counts in KB now.
- Pressing *Cancel* while an activation was already inside a system call could show the result of the cancelled
  activation afterwards.
- A dish terminal whose package cannot be resolved can be switched on / blocked in *App management* (it did nothing).
- The Shizuku account log (`log.txt`) was never trimmed; it stops at 256 KB now.

## 1.3.0 (5)

### Changed
- **Remote / D-pad**: every button now shows the focus frame (app bar buttons, owner-type menu, block / unblock,
  Freeze, Terminal and the other dialogs buttons) - before, only some of them did. The permission dialog starts with the
  focus on **Refuse** on a TV or a box without a touch screen, so a key that is still held down from the previous screen
  cannot grant access by accident. New helpers in `TvFocus.kt`: `TvButton`, `TvOutlinedButton`, `TvIconButton`.
- **Activation screen with a remote**: the D-pad jumped from the back arrow straight to **OK** and never reached the
  list of apps, and once the focus was on "back" or "OK" nothing could be selected. On a TV or a box without a touch
  screen the screen is now ONE list: the apps, then the **OK** button (and the owner-type menu in Shizuku mode) as its
  last item; the back arrow is not a D-pad target there (the remote's Back key does the same). The rows are plain
  clickable rows (like the other lists), point at each other explicitly, the first app gets the focus at start, and the
  status dialog starts with the focus on its first button. The same row style is used in *App management* and for the
  switch rows in *Settings* (`tvToggle` in `TvFocus.kt`).
- A refusal of the permission dialog by any way (Back, timeout, losing focus) now reaches a waiting dish terminal at
  once instead of after its timeout.

### Fixed
- Settings and the dish registry now live in device protected storage (existing values are moved over). Before, a
  call during Direct Boot (provider / boot receiver / daemon service) threw `IllegalStateException`.
- Permission dialog: the answer is saved and sent to the client exactly once (it used to run on every `finish()`);
  a request replaced by a new one is answered `DENIED` instead of hanging; a dead client no longer crashes the app;
  no `finish()` from composition or from a background thread; rotation no longer denies the request.
- Daemon service: calls `startForeground()` before stopping (no `ForegroundServiceDidNotStart` crash); a background
  error no longer crashes the app; an unreadable signature no longer deletes (and so un-blocks) an entry.
- dish: `api` was treated as harmless if `--list` / `--sig` appeared anywhere in the line; only leading options count now.
  The in-memory log keeps fewer words of `batch` / `api` so values are not stored.
- Signature check uses all signers of an app (identical value for single-signer apps), `getPackagesForUid` no longer
  throws on an empty list.

## 1.2.0 (4)

### Added
- **Dhizuku Compatibility Hub**: a hidden plugin (`com.rosan.dhizuku`, no launcher icon) that forwards the Dhizuku API to
  DhizukuF, so apps written for the original Dhizuku work with DhizukuF. New module `:hub`, built with the app, signed with
  the same key and with the same version (it reads `version.properties`); the APK is carried in the app and installed
  silently from *Settings > Dhizuku Compatibility Hub*, the first card, with install / update / remove and a status line.
  When the original Dhizuku blocks it the card offers **Replace**, with a confirmation naming the app that will be
  removed. See [docs/HUB.md](docs/HUB.md). The idea of the hub is by @Jozinho-netizen. The release workflow also attaches `DhizukuF-Hub-<tag>.apk`.
- New dish commands: `log` (audit log), `batch`, `uninstall`, `app-restrictions`, `lock-task`, `vpn`, `location`,
  `backup`, `time`, `lock-timeout`, `adb-wifi`, `ime`, `bugreport`, and `help -s WORD`. Dangerous forms (`uninstall`,
  `lock-task set|clear`, `vpn on`, `time set`, `ime allow|reset`, `adb-wifi enable`) need *Enable dangerous commands*.
- dish stability: a command is stopped after 40 s instead of blocking a Binder thread; internal failures are reported
  as `NOT DONE`.
- dish client (source): every `IActivityManager` broadcast variant is tried until one works, the handshake is sent
  twice (DhizukuF may need to start first), one automatic reconnect after a dead binder, error messages include the
  Android API level, and the console has `history`, `!!` and `clear`.
- New workflow **Rebuild dish dex**: when `dish-client/` changes on a branch (never on a tag) it rebuilds the dex with d8,
  verifies it and commits it back to that branch, so the repository always holds a dex that matches the source.
- `dish-client/build.sh` supports d8 (Android SDK build-tools) as well as dx and verifies the dex with the new
  `dish-client/verify_dex.py`; both workflows rebuild the dex before the APK and keep the committed one if that fails.

### Fixed
- App management: switching a dish terminal **off** removes it from the list (the next dish request registers it
  again). A terminal that was refused, not answered in time, or whose app was uninstalled no longer stays behind as a
  disabled row either; blocked terminals keep their entry so the block is not lost.
- Compatibility: the `android.software.device_admin` feature is no longer *required*, so the app installs on TV
  boxes that do not declare it (without a device policy service it shows that it is not activated).
- The `dish` launcher now finds `app_process` on images that only ship a 32/64-bit variant, resolves its own folder,
  uses `getprop` by full path, and on Android 14+ explains how to place the files when the `.dex` cannot be made
  read-only (shared storage).

## 1.1.0 (3)

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
- Android TV / D-pad: on **Activation by DhizukuF** (and by Shizuku) the apps in the list could not be chosen, only
  Back and OK were reachable. Each row is now one focus target with a focus frame; OK also has a frame. On a TV the first app is focused automatically when the list opens.
- Android TV / D-pad: buttons placed inside a card (**Export dish files**, **View command**, the delete button of a
  user, the checkbox when choosing an admin) could not be reached with the remote, because the card itself took
  the focus. Cards that only hold buttons are no longer focus targets, cards with an action and buttons skip the
  D-pad but keep touch, and all text buttons and the approval dialog buttons show a focus frame. The dish card has a
  *How to use dish* button for the remote.
- Opening a link no longer crashes on devices without a browser (the link is copied instead).
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
