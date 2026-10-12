# dish — Device Owner console for DhizukuF

`dish` runs Device Owner commands from any terminal (Termux, `adb shell`, any shell app),
the same way `rish` works for Shizuku.

1. In DhizukuF (it must be the Device Owner) open the **Terminal (dish)** card → **Export dish files**
   and pick a folder. Two files are written: `dish` and `dish_dhizukuf.dex`.
2. Put both files together where your terminal can run scripts:
   - Termux: `cp /sdcard/<folder>/dish* $PREFIX/bin/ && chmod +x $PREFIX/bin/dish`
   - adb: `adb push dish dish_dhizukuf.dex /data/local/tmp/` then `adb shell sh /data/local/tmp/dish`
   - anything else: `sh /path/to/dish`
3. Run `dish` (interactive, prompt `dish$ `), `dish help`, or `dish -c "status"`.
   Before the prompt appears dish checks access. If dish is off, DhizukuF is not the Device Owner
   or access is refused, it prints the reason and does not start. (`DISH_SKIP_CHECK=1` skips the check.)
4. The first run sends a registration request: the terminal appears in **DhizukuF > App management**
   (switch off) and the usual approval dialog opens, e.g. "Allow Termux (dish) to use Dhizuku?".
   dish waits up to 60 seconds; you can also flip the switch in the list. Later the entry can be
   blocked or revoked from the same list.

   Settings: **Enable dish** (default on, independent from **Enable Dhizuku**) turns the console on
   or off; **Show dish** (default on) adds the `(dish)` mark to the name in the dialog and in
   App management; **Advanced confirmation window** (default off) adds a Block button to the dialog.

All dish console output is English only.

## Newer commands

| Command | What it does |
|-|-|
| `log [N]`, `log clear` | the last commands run through dish (in memory only; only the first words of a command are kept) |
| `batch [--keep-going] "CMD" "CMD"` | several commands in one call (max 50); each one keeps its own rules, the dangerous switch included; stops at the first failure |
| `help -s WORD` | search the command descriptions |
| `uninstall PKG` | silent uninstall as Device Owner (dangerous) |
| `app-restrictions get\|set\|clear PKG [KEY VALUE]` | managed configuration of an app; `true`/`false` and numbers are typed |
| `lock-task status\|set PKG...\|clear` | kiosk: apps allowed to pin themselves (dangerous to change) |
| `vpn on PKG [lockdown]`, `vpn off`, `vpn status` | always-on VPN (turning it on is dangerous) |
| `location`, `backup`, `lock-timeout`, `time set`, `adb-wifi`, `ime`, `bugreport` | location on/off, backup service, longest screen-lock time, set the clock, wireless debugging, allowed keyboards, bug report |

Commands that run longer than 40 seconds are stopped with `NOT DONE`, so a stuck system call cannot block DhizukuF.

The interactive console also knows `history`, `!!` (repeat the last command) and `clear`.

## Compatibility

DhizukuF needs Android 8.0 (API 26) or newer. A command that the running Android version does not have answers
`NOT DONE: Not supported on this Android version` instead of failing.

| Android | What it adds for dish |
|-|-|
| 8.0+ | everything not listed below |
| 9+ | `timezone`, `install-existing`, `clear-data` |
| 10+ | `private-dns` |
| 11+ | `auto-time`, `auto-timezone` |
| 12+ | granting sensor permissions (camera, microphone, location) may be refused by the system |
| 14+ | the `.dex` file must be read-only; the `dish` launcher does this itself |

Notes for the `dish` launcher:

- On Android 14+ keep `dish` and `dish_dhizukuf.dex` in a normal folder (Termux: `$PREFIX/bin`, adb shell:
  `/data/local/tmp`), not on shared storage (`/sdcard`): files there cannot be made read-only and the system
  refuses to load them. The launcher prints this hint.
- It works with `/system/bin/app_process`, and falls back to the 64-bit or 32-bit variant on images that only
  ship one.
- The client reaches DhizukuF with a broadcast sent through the hidden `IActivityManager` API. The call is matched by
  name and parameter types, which covers Android 8 to current releases; if a future release changes it, dish prints
  `cannot send the request to DhizukuF` with the reason, and an app update will be needed.
- In **Profile Owner** mode the approval window can be refused by Android when it is opened from the background
  (Device Owners are exempt). dish then says so and keeps the request in *App management*: switch the terminal
  on there.

## Android TV

DhizukuF appears in the TV launcher and can be used with a remote control (D-pad): move the focus with the
arrows, press the centre button to open a card or toggle a switch. TV boxes usually have no folder picker; then
**Export dish files** saves `dish` and `dish_dhizukuf.dex` to `Android/data/com.EdS.DhizukuF/files/dish` and shows
the path. Copy the files with `adb pull` / `adb push`, or run them from `adb shell`:

```sh
adb pull /sdcard/Android/data/com.EdS.DhizukuF/files/dish
adb push dish/dish dish/dish_dhizukuf.dex /data/local/tmp/
adb shell sh /data/local/tmp/dish
```

## Dangerous commands

**Settings > Enable dangerous commands** is **off by default**. While it is off, dish refuses the risky
forms and answers `NOT DONE: '<command>' is a dangerous command and is switched off`:

| Command | Dangerous form |
|-|-|
| `reboot`, `clear-data` | always |
| `api` / `call` | every call except `--list` and `--sig` |
| `settings` | `put` |
| `permission` | `grant`, `deny`, `default` |
| `permission-policy` | `grant`, `deny` |
| `keyguard`, `status-bar` | `disable` |
| `adb` | `enable` |
| `private-dns` | setting a value (`status` is allowed) |

`dish help` marks these commands with `!`, `dish help COMMAND` has a `Dangerous:` line and
`dish status` shows whether the switch is on. The same switch applies to the regular Dhizuku API
(see [SECURITY.md](../SECURITY.md) for what can be recognised there). Wipe, factory reset, removing
users and similar calls stay blocked even when the switch is on.

## Device Owner and Profile Owner

dish works when DhizukuF is the **Device Owner** or the **Profile Owner**, but Profile Owner is a
limited mode. Commands that change device-wide state need the Device Owner: `reboot`, `adb`,
`stay-awake`, `timezone`, `private-dns`, `keyguard`, `status-bar`, `owner-info`, `auto-time`,
`auto-timezone` and `settings put global`. In Profile Owner mode they answer
`NOT DONE: <command> needs Device Owner, but DhizukuF is Profile Owner.` (reading with `status` or
`get` still works). App and profile commands (`hide`, `suspend`, `permission`, `restriction`,
`camera`, ...) work in both modes, but in Profile Owner mode they affect the managed profile only.

`dish status` shows the current mode. In `dish help` commands marked `*` need the Device Owner, and
`dish help COMMAND` has a `Needs:` line.

## Answers

Every command says what happened:

| Answer | Meaning | Exit code |
|-|-|-|
| `OK: ...` | The action was done (and verified where possible) | 0 |
| `NOT DONE: ...` | The system refused or the input was wrong; the reason follows | 1 |
| `Usage: ...` | Wrong command line | 2 |
| `Unknown command` | Suggestions are printed | 127 |

## Commands

`help` lists the names only. `help COMMAND` shows what a command does, its usage and an example.

| Group | Commands |
|-|-|
| General | `help` `status` `version` `id` `device` `ping` `log` `batch` `lock` `reboot` |
| Apps | `list` `app-info` `hide` `unhide` `is-hidden` `suspend` `unsuspend` `is-suspended` `block-uninstall` `allow-uninstall` `is-uninstall-blocked` `enable-system-app` `install-existing` `clear-data` `uninstall` `app-restrictions` `permission` `permission-policy` |
| Restrictions | `restriction` `camera` `screen-capture` `keyguard` `status-bar` `mute` |
| Device | `adb` `adb-wifi` `stay-awake` `auto-time` `auto-timezone` `time` `timezone` `private-dns` `location` `backup` `lock-timeout` `lock-task` `vpn` `ime` `bugreport` `owner-info` `org-name` |
| Settings | `settings get\|put\|list` |
| API | `api` |

Examples: `dish status`, `dish hide com.example.app`, `dish list hidden`,
`dish restriction add no_install_apps`, `dish camera disable`, `dish adb status`,
`dish settings put global stay_on_while_plugged_in 3`.

## Direct API

`dish api` calls any public method by name, so new calls need no rebuild.

```text
api [--on TARGET] [--no-admin] METHOD [ARG...]
api --list [TEXT]          find methods whose name contains TEXT
api --sig METHOD           show every signature of a method
```

- Default target is `dpm` (`DevicePolicyManager`); the admin argument is added automatically.
  Other targets: `pm um am audio power wifi conn notif tele`.
- Plain arguments are converted to the parameter type. Typed forms: `s:text`, `i:5`, `l:5`,
  `f:1.5`, `d:1.5`, `b:true`, `cn:pkg/class`, `strs:a,b`, `ints:1,2`, `null`, and `@admin` to put
  the admin component at a specific position.
- `call` is an alias of `api`.

```sh
dish api --list lockTask
dish api setLockTaskPackages strs:com.example.a,com.example.b
dish api getCameraDisabled
dish api --on audio getStreamVolume i:3
```

Destructive calls (wipe, factory reset, removing the owner, password reset) are blocked.

## Rebuilding the dex

The client is plain Java in `dish-client/src` (no dependencies). The prebuilt dex lives in
`app/src/main/assets/dish_dhizukuf.dex`. To rebuild it after editing the client:

```sh
# JDK + the Android SDK build-tools (d8):  export ANDROID_HOME=~/Android/Sdk   (or D8=/path/to/d8)
# or the old dx:                            apt install dalvik-exchange          (or DX=/path/to/dx)
./dish-client/build.sh
```

`build.sh` checks the result with `dish-client/verify_dex.py` (header, checksums, sorted tables, instruction
streams) before it replaces the committed file. The workflow **Rebuild dish dex** runs it whenever `dish-client/` changes on GitHub and commits the new
`dish_dhizukuf.dex` back to the branch (run `git pull` afterwards; the branch must allow pushes from
`github-actions`). The build and release workflows run it too, before `assembleRelease`; if it
fails the committed dex is kept, and the repository variable `DISH_REBUILD_DEX=false` switches the rebuild off.
The client improvements (every `IActivityManager` broadcast variant is tried, the request is sent twice, one
reconnect after a dead binder, `history` / `!!` / `clear`) take effect in a build made this way.

Transport: the client sends a broadcast through ActivityManager (as the terminal's own uid) that
carries its Binder; DhizukuF (`DishReceiver`) answers with its Binder (`DishBinder`) and from then on
every command is a direct Binder call. DhizukuF learns the caller's real uid from
`Binder.getCallingUid()`, so it cannot be spoofed. No sockets and no `am` are used, so it works from
any terminal and also wakes DhizukuF if it was killed. Keep the transaction codes and reply kinds in
`DishMain.java`, `DishBinder.kt` and `DishEngine.kt` in sync. Keep the codes in `DishMain.java` and `DishEngine.kt` in sync.
