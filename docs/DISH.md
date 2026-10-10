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
| General | `help` `status` `version` `id` `device` `ping` `lock` `reboot` |
| Apps | `list` `app-info` `hide` `unhide` `is-hidden` `suspend` `unsuspend` `is-suspended` `block-uninstall` `allow-uninstall` `is-uninstall-blocked` `enable-system-app` `install-existing` `clear-data` `permission` `permission-policy` |
| Restrictions | `restriction` `camera` `screen-capture` `keyguard` `status-bar` `mute` |
| Device | `adb` `stay-awake` `auto-time` `auto-timezone` `timezone` `private-dns` `owner-info` `org-name` |
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
apt install openjdk-21-jdk-headless dalvik-exchange   # or set DX=/path/to/dx
./dish-client/build.sh
```

Transport: the client sends a broadcast through ActivityManager (as the terminal's own uid) that
carries its Binder; DhizukuF (`DishReceiver`) answers with its Binder (`DishBinder`) and from then on
every command is a direct Binder call. DhizukuF learns the caller's real uid from
`Binder.getCallingUid()`, so it cannot be spoofed. No sockets and no `am` are used, so it works from
any terminal and also wakes DhizukuF if it was killed. Keep the transaction codes and reply kinds in
`DishMain.java`, `DishBinder.kt` and `DishEngine.kt` in sync. Keep the codes in `DishMain.java` and `DishEngine.kt` in sync.
