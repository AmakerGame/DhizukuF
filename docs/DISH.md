# dish — Device Owner console for DhizukuF

`dish` runs Device Owner commands from any terminal (Termux, `adb shell`, any shell app),
the same way `rish` works for Shizuku.

1. In DhizukuF (it must be the Device Owner) open the **Terminal (dish)** card → **Export dish files**
   and pick a folder. Two files are written: `dish` and `dish_dhizukuf.dex`.
2. Put both files together where your terminal can run scripts:
   - Termux: `cp /sdcard/<folder>/dish* $PREFIX/bin/ && chmod +x $PREFIX/bin/dish`
   - adb: `adb push dish dish_dhizukuf.dex /data/local/tmp/` then `adb shell sh /data/local/tmp/dish`
   - anything else: `sh /path/to/dish`
3. Run `dish` (interactive), `dish help`, or `dish -c "status"`.
4. The first run sends a registration request: the terminal appears in **DhizukuF > App management**
   (switch off) and the usual approval dialog opens, e.g. "Allow Termux (dish) to use Dhizuku?".
   dish waits up to 60 seconds; you can also flip the switch in the list. Later the entry can be
   blocked or revoked from the same list.

   Settings: **Show dish** (default on) adds the `(dish)` mark to the name in the dialog and in
   App management; **Advanced confirmation window** (default off) adds a Block button to the dialog.

All dish console output is English only.

Examples: `dish status`, `dish hide com.example.app`, `dish restriction add no_install_apps`,
`dish camera disable`, `dish settings put global stay_on_while_plugged_in 3`,
`dish call --list`, `dish call setLockTaskPackages com.example.a,com.example.b`.

Destructive calls (wipe, removing the owner, password reset) are blocked in `dish call`.

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
